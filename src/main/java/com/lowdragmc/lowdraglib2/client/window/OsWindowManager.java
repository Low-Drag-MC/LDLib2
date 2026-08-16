package com.lowdragmc.lowdraglib2.client.window;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Drives every open {@link OsWindow} once per frame, and makes sure none of them outlive the game.
 *
 * <p>The pump hangs off {@code RenderFrameEvent.Post}, which fires after the game renderer has
 * finished and flushed but before Minecraft unbinds its main render target and swaps. At that point
 * the render thread is idle, the shared buffer source is empty and the model-view stack is back
 * where it started — the cleanest seam in the frame. Priority is {@code LOW} so other post-frame
 * consumers see the state they expect, and every host is driven inside its own try/catch so one
 * misbehaving window cannot take the game's frame down with it.
 */
@EventBusSubscriber(modid = LDLib2.MOD_ID, value = Dist.CLIENT)
public final class OsWindowManager {

    /**
     * How many frames in a row a host may throw before it is closed. One is a transient; two in a row
     * is a host that will keep throwing, and leaving it open floods the log every frame.
     */
    private static final int FAILURE_LIMIT = 2;

    /**
     * Everything the manager owns for a host, keyed by host and in the order they were opened.
     *
     * <p>The window is held here rather than read back off the host: a host hands its window
     * reference away when it is destroyed, so asking it during teardown is how the manager ends up
     * dereferencing a window that no longer exists.
     */
    private static final LinkedHashMap<OsWindowHost, Entry> ENTRIES = new LinkedHashMap<>();
    private static int totalFailures;

    private static final class Entry {
        private final OsWindow window;
        private final OsWindowPresenter presenter;
        private int failures;

        private Entry(OsWindow window) {
            this.window = window;
            this.presenter = OsWindowPresenter.create(window);
        }
    }

    private OsWindowManager() {
    }

    /**
     * Whether a second OS window can be opened at all.
     *
     * <p>Fullscreen is excluded on macOS deliberately rather than left to fail: a native fullscreen
     * window owns a Space of its own, so the second window would be created successfully but land in
     * a different one — the user sees nothing happen and has to go looking for it through Mission
     * Control. There is no way to place a window into another application's fullscreen Space, so the
     * honest answer there is no. Windows and Linux have no such notion and are not restricted.
     *
     * <p>The graphics backend is not a reason to say no. 26.2 can run on Vulkan as well as OpenGL and
     * both are supported here; which one is in use only decides how the frame reaches the window, see
     * {@link OsWindowBackend}.
     */
    public static boolean isAvailable() {
        if (!RenderSystem.isOnRenderThread()) return false;
        if (GLFW.glfwGetPlatform() == GLFW.GLFW_PLATFORM_NULL) return false;
        return GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_COCOA
                || !Minecraft.getInstance().getWindow().isFullscreen();
    }

    /**
     * Opens a window for {@code host}.
     *
     * @return {@code false} if the platform refused, in which case nothing was registered and the
     *         caller should fall back to an in-game panel
     */
    public static boolean open(OsWindowHost host, String title, int width, int height, boolean decorated) {
        RenderSystem.assertOnRenderThread();
        var window = OsWindow.create(title, width, height, decorated);
        if (window == null) return false;
        ENTRIES.put(host, new Entry(window));
        host.onAttached(window);
        return true;
    }

    /**
     * Destroys {@code host}'s window and forgets it. Safe to call more than once, and safe to call
     * from inside the host's own input dispatch — which is exactly what a close button does.
     */
    public static void close(OsWindowHost host) {
        var entry = ENTRIES.remove(host);
        if (entry == null) return;
        // The presenter's framebuffer object lives in the window's context, so it has to go first.
        entry.presenter.destroy();
        entry.window.destroy();
        host.onDestroyed();
    }

    public static List<OsWindowHost> hosts() {
        return List.copyOf(ENTRIES.keySet());
    }

    public static boolean hasWindows() {
        return !ENTRIES.isEmpty();
    }

    /**
     * How many times a host has thrown while being driven, since the game started.
     *
     * <p>Every such throw is swallowed so one bad window cannot kill the game's frame, which also
     * means nothing downstream would otherwise notice. A test that opens and closes windows can
     * assert this stays at zero and catch the whole class of teardown-ordering bugs that are
     * otherwise only visible as a line in the log.
     */
    public static int totalFailures() {
        return totalFailures;
    }

    /**
     * Hands {@code host}'s freshly rendered target to its presenter, whichever backend that is.
     */
    public static void present(OsWindowHost host, RenderTarget source) {
        var entry = ENTRIES.get(host);
        if (entry != null) {
            entry.presenter.present(source);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onFrameRendered(RenderFrameEvent.Post event) {
        if (ENTRIES.isEmpty()) return;
        var partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        // Copy: a host can close itself (or another) while being driven — a close button runs inside
        // drainInput, so by the time it returns this host may already be gone.
        for (var host : List.copyOf(ENTRIES.keySet())) {
            var entry = ENTRIES.get(host);
            if (entry == null) continue;
            if (entry.window.isDestroyed()) {
                close(host);
                continue;
            }
            try {
                host.drainInput();
                if (!isLive(host, entry)) continue;
                host.renderFrame(partialTick);
                if (!isLive(host, entry)) continue;
                host.present();
                entry.failures = 0;
            } catch (Throwable throwable) {
                totalFailures++;
                entry.failures++;
                LDLib2.LOGGER.error("[os-window] host failed to render (attempt {}/{})",
                        entry.failures, FAILURE_LIMIT, throwable);
                if (entry.failures >= FAILURE_LIMIT) {
                    LDLib2.LOGGER.error("[os-window] closing the window after repeated failures");
                    close(host);
                }
            }
        }
    }

    /**
     * Whether {@code host} is still registered with the same window — it may have closed itself
     * partway through being driven.
     */
    private static boolean isLive(OsWindowHost host, Entry entry) {
        return ENTRIES.get(host) == entry && !entry.window.isDestroyed();
    }

    /**
     * Closes every window while the GL context is still valid.
     *
     * <p>Not optional. {@code Minecraft#stop} fires this and then closes its own window, which calls
     * {@code glfwTerminate} — that destroys every remaining window out from under us and leaks the
     * native callback closures we allocated for them.
     */
    @SubscribeEvent
    public static void onGameShuttingDown(GameShuttingDownEvent event) {
        for (var host : List.copyOf(ENTRIES.keySet())) {
            try {
                close(host);
            } catch (Throwable throwable) {
                LDLib2.LOGGER.error("[os-window] failed to close a window during shutdown", throwable);
            }
        }
        ENTRIES.clear();
    }
}
