package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * The destination a UI frame is presented in: its render target, its gui scale and its OS window.
 *
 * <p>Until this existed, everything read {@link Minecraft#getWindow()} and
 * {@link Minecraft#getMainRenderTarget()} directly, which is correct exactly as long as there is one
 * place a UI can appear. {@link #main()} is still the default and still resolves to those, so
 * nothing changes for an on-screen UI; the point is that a UI can now be rendered into an off-screen
 * target of a different size and presented in a different window.
 *
 * <p>Three consumers depend on this and are easy to miss:
 * <ul>
 *   <li>file drops and cursor queries, which must go to <em>this</em> window: GLFW reports cursor
 *       position per window, and the game's would be stale or plain wrong;</li>
 *   <li>frame capture, which reads back this target rather than the game's own frame — a UI in its
 *       own window never appears in the latter;</li>
 *   <li>clipping. The gui renderer is deferred, so a clip rectangle rides along on each
 *       {@code GuiElementRenderState} — but {@code GuiRenderer#enableScissor} still resolves it
 *       against the game window's pixel height and gui scale, unconditionally. An off-screen frame
 *       of a different size therefore needs this surface's metrics pushed as an override, or every
 *       clipped element lands outside the target and silently is not drawn.</li>
 * </ul>
 */
public interface UISurface {

    /**
     * The render target being drawn into. Never null; the main surface reports Minecraft's.
     */
    RenderTarget target();

    /**
     * Physical target width in pixels, i.e. gui units multiplied by {@link #guiScale()}.
     */
    default int framebufferWidth() {
        return target().width;
    }

    default int framebufferHeight() {
        return target().height;
    }

    double guiScale();

    /**
     * Window size in screen coordinates — the space {@code glfwGetCursorPos} reports in, which is
     * not the framebuffer size on a HiDPI display.
     */
    int screenWidth();

    int screenHeight();

    /**
     * The GLFW window this surface is presented in, for cursor and key queries.
     */
    long windowHandle();

    /**
     * Whether this is Minecraft's own window. Vanilla's own gui code assumes it is, so this is the
     * flag that says "the vanilla path already did the right thing, leave it alone".
     */
    default boolean isMainWindow() {
        return false;
    }

    /**
     * Width in gui units — what a {@code ModularUI} is laid out against.
     */
    default int guiScaledWidth() {
        return Mth.ceil(framebufferWidth() / guiScale());
    }

    default int guiScaledHeight() {
        return Mth.ceil(framebufferHeight() / guiScale());
    }

    /**
     * Minecraft's own window and main render target.
     */
    static UISurface main() {
        return MainWindowSurface.INSTANCE;
    }

    /**
     * The surface currently being drawn into, or {@link #main()} outside of a render pass.
     */
    static UISurface current() {
        return SurfaceStack.current();
    }

    /**
     * Install {@code surface} as {@link #current()} until the returned scope is closed. Renders
     * happen on the render thread one at a time, so a plain stack is enough.
     */
    static Scope push(UISurface surface) {
        return SurfaceStack.push(surface);
    }

}
