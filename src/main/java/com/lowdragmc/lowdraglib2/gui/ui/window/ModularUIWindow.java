package com.lowdragmc.lowdraglib2.gui.ui.window;

import com.lowdragmc.lowdraglib2.client.window.OsWindow;
import com.lowdragmc.lowdraglib2.client.window.OsWindowEvent;
import com.lowdragmc.lowdraglib2.client.window.OsWindowHost;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.OffscreenSurface;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;

import java.util.ArrayList;
import java.util.List;

/**
 * Hosts any {@link ModularUI} in its own operating-system window.
 *
 * <p>This is the general-purpose entry point — nothing here knows about the editor. Give it a UI and
 * a title, call {@link #open}, and the UI is drawn into a second window that can be moved to another
 * monitor:
 *
 * <pre>{@code
 * var window = new ModularUIWindow(new ModularUI(UI.of(myRoot)), "My Tool");
 * window.setDragArea(myTitleBar);        // optional: what moves the window
 * window.setStyleSource(parentUI);       // optional: keep the parent's theme
 * if (!window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, 640, 400, false)) {
 *     // no second window available - host myRoot in-game instead
 * }
 * }</pre>
 *
 * <p>The UI is drawn into an off-screen target using Minecraft's own context and the ordinary render
 * path — same shaders, same fonts, same nested framebuffers — and then blitted into the second
 * window. Nothing about how the UI draws itself changes; only where it lands.
 *
 * <p>Input arrives as raw GLFW callbacks queued by {@link OsWindow} and is replayed here through the
 * same {@code ModularUIWidget} methods a {@code Screen} would call, so focus tracking, click counting
 * and drag bookkeeping all behave exactly as they do in the game window. Two overrides are installed
 * for the duration: {@link KeyState} reads <em>this</em> window's keyboard (GLFW key state is
 * per-window, so the game window's would report nothing while this one is focused), and
 * {@link ModularUI#active()} points here so anything that opens a popup without an element in hand
 * parents it into this UI rather than the parent's.
 *
 * <p>Windows are opened undecorated, so moving and resizing are handled here rather than by the
 * platform. That is not only cosmetic: a decorated window's move and resize gestures run in a nested
 * modal event loop inside {@code glfwPollEvents}, which Minecraft calls every frame, so dragging one
 * would freeze the entire game for as long as the mouse is held.
 */
@OnlyIn(Dist.CLIENT)
public class ModularUIWindow implements OsWindowHost {

    /**
     * How far from an edge counts as grabbing it, in window pixels. Generous on purpose: the window
     * has no platform border to aim at, so this band plus the pointer shape is the only affordance.
     */
    protected static final int RESIZE_BORDER = 6;
    /**
     * The resize floor. Public because it is also the floor for a size <em>computed</em> by a caller
     * — a remembered rectangle, or one derived from wherever a pane used to sit — and a caller that
     * does not know it produces a window the user cannot get back.
     */
    public static final int MIN_WIDTH = 200;
    public static final int MIN_HEIGHT = 150;

    private static final int EDGE_LEFT = 1;
    private static final int EDGE_RIGHT = 1 << 1;
    private static final int EDGE_TOP = 1 << 2;
    private static final int EDGE_BOTTOM = 1 << 3;

    /**
     * How far outside the UI a synthetic pointer is parked when the cursor leaves the window, so
     * hover and tooltips clear. Large enough to miss any element, finite so the transform maths stays
     * well behaved.
     */
    private static final float CURSOR_OUTSIDE = -10_000f;

    protected enum Gesture {NONE, MOVE, RESIZE}

    @Getter
    private final ModularUI modularUI;
    @Getter
    private String title;

    /**
     * The element that moves the window when dragged — a title bar. Anything inside it still works
     * normally as long as {@link #isDragBlocker} recognises it; buttons do by default.
     */
    @Setter
    @Nullable
    private UIElement dragArea;

    /**
     * Whether the edges can be grabbed to resize.
     */
    @Setter
    private boolean resizable = true;

    /**
     * A UI whose stylesheets this window should follow.
     *
     * <p>Needed because a theme is usually not part of {@code UI.of(...)} — it is added to the style
     * engine at runtime — so a window built from a fresh {@code ModularUI} starts unstyled. Pointing
     * at the originating UI keeps the two looking the same, including after a live theme change.
     */
    @Setter
    @Nullable
    private ModularUI styleSource;

    @Nullable
    private OsWindow window;
    @Nullable
    private OffscreenSurface surface;

    private float mouseX;
    private float mouseY;
    private double lastGuiScale;
    private final List<Stylesheet> mirroredStylesheets = new ArrayList<>();

    private Gesture gesture = Gesture.NONE;
    private int resizeEdges;
    private double grabGlobalX;
    private double grabGlobalY;
    private int grabWindowX;
    private int grabWindowY;
    private int grabWindowWidth;
    private int grabWindowHeight;

    @Setter
    @Nullable
    private Runnable onCloseRequested;

    @Nullable
    private WindowBounds restoredBounds;

    /**
     * Every {@code ModularUIWindow} currently open, in the order they were opened.
     */
    public static List<ModularUIWindow> openWindows() {
        var windows = new ArrayList<ModularUIWindow>();
        for (var host : OsWindowManager.hosts()) {
            if (host instanceof ModularUIWindow window) {
                windows.add(window);
            }
        }
        return windows;
    }

    /**
     * The window hosting {@code ui}, or {@code null} if it is drawn in the game window.
     */
    @Nullable
    public static ModularUIWindow windowOf(ModularUI ui) {
        for (var window : openWindows()) {
            if (window.getModularUI() == ui) return window;
        }
        return null;
    }

    public ModularUIWindow(ModularUI modularUI, String title) {
        this.modularUI = modularUI;
        this.title = title;
        // Nothing else ticks this UI: it has no Screen behind it, so animations and anything driven
        // by ModularUI#tick would simply never run. Same reason ModularHudLayer sets it.
        this.modularUI.setTickWhileRending(true);
    }

    /**
     * Creates and shows the window.
     *
     * @param x         window position, or {@link Integer#MIN_VALUE} to let the platform place it
     * @param decorated whether the OS draws the frame. Leave this false unless you know you want the
     *                  platform's modal move/resize loop and the game freeze that comes with it.
     * @return {@code false} if no second window could be opened, in which case the caller should host
     *         the UI in the game window instead
     */
    public boolean open(int x, int y, int width, int height, boolean decorated) {
        if (window != null) return true;
        if (!OsWindowManager.isAvailable()) return false;
        if (!OsWindowManager.open(this, title, width, height, decorated)) return false;
        var opened = window;
        if (opened == null) return false;
        if (x != Integer.MIN_VALUE && y != Integer.MIN_VALUE) {
            opened.setPosition(x, y);
        }
        opened.show();
        return true;
    }

    /**
     * Renames the window, including the OS-level title if it is already open.
     */
    public void setTitle(String title) {
        this.title = title;
        if (window != null) {
            window.setTitle(title);
        }
    }

    public void close() {
        OsWindowManager.close(this);
    }

    public boolean isOpen() {
        return window != null && !window.isDestroyed();
    }

    @Override
    public OsWindow window() {
        if (window == null) {
            throw new IllegalStateException("Window has not been opened");
        }
        return window;
    }

    /**
     * The off-screen target this window's UI is drawn into, or {@code null} before it opens. Reading
     * it back is how a test can look at what the window actually shows — nothing else can, since its
     * pixels never appear in the game's frame.
     */
    @Nullable
    public OffscreenSurface surface() {
        return surface;
    }

    @Override
    public void onAttached(OsWindow window) {
        this.window = window;
        this.surface = new OffscreenSurface(window.handle(),
                window.getFramebufferWidth(), window.getFramebufferHeight(),
                window.getWindowWidth(), window.getWindowHeight());
        this.lastGuiScale = surface.guiScale();
        modularUI.init(surface.guiScaledWidth(), surface.guiScaledHeight());
    }

    @Override
    public void onDestroyed() {
        if (surface != null) {
            surface.destroy();
            surface = null;
        }
        window = null;
        gesture = Gesture.NONE;
    }

    public boolean isMaximized() {
        return isOpen() && window().isMaximized();
    }

    /**
     * Pins the window above the others. Silently a no-op where the platform refuses — see
     * {@link OsWindow#supportsAlwaysOnTop()}, which a caller offering a control for this should check.
     */
    public void setAlwaysOnTop(boolean onTop) {
        if (isOpen()) {
            window().setAlwaysOnTop(onTop);
        }
    }

    public boolean isAlwaysOnTop() {
        return isOpen() && window().isAlwaysOnTop();
    }

    /**
     * Where this window was the last time it was open and not maximized, or {@code null} if it has
     * never been either. What to reopen it at.
     *
     * <p>Sampled every frame rather than read on close, and that is not laziness: by the time
     * {@code onDestroyed} runs the handle is already destroyed, so {@code isMaximized()} answers
     * false and the maximized bounds would be recorded as though they were an ordinary size —
     * leaving a window with no way back to its real one. Sampling also survives the paths where
     * nothing gets to run at all, such as the game being killed.
     */
    @Nullable
    public WindowBounds restoredBounds() {
        return restoredBounds;
    }

    /** Four ints the platform's own callbacks have already cached; no syscall but the maximize query. */
    private void sampleRestoredBounds() {
        if (!isOpen()) return;
        var current = window();
        if (current.isMaximized()) return;
        restoredBounds = new WindowBounds(current.getPositionX(), current.getPositionY(),
                current.getWindowWidth(), current.getWindowHeight());
    }

    public void toggleMaximized() {
        if (!isOpen()) return;
        var current = window();
        if (current.isMaximized()) {
            current.restore();
        } else {
            current.maximize();
        }
    }

    // ------------------------------------------------------------------------------------- input

    @Override
    public void drainInput() {
        var current = window;
        // Checked before the scopes are opened: on a frame where nothing happened — most of them —
        // installing and restoring two ambient overrides is pure overhead.
        if (current == null || current.isDestroyed() || !current.hasPendingEvents()) return;
        try (var ignoredKeys = KeyState.scoped(current::isKeyDown);
             var ignoredActive = ModularUI.scopedActive(modularUI)) {
            current.drain(this::handleEvent);
        }
    }

    protected void handleEvent(OsWindowEvent event) {
        var current = window;
        var currentSurface = surface;
        if (current == null || currentSurface == null) return;
        var widget = modularUI.getWidget();
        switch (event) {
            case OsWindowEvent.CursorPos cursor -> {
                if (gesture != Gesture.NONE) {
                    applyGesture();
                    return;
                }
                var previousX = mouseX;
                var previousY = mouseY;
                mouseX = toGuiX(cursor.x(), currentSurface);
                mouseY = toGuiY(cursor.y(), currentSurface);
                updateCursorShape();
                // Nothing downstream hit-tests; every mouse method reads the cached hover, which is
                // normally only recomputed during render. Resolve it here or the event lands on
                // whatever was under the cursor last frame.
                modularUI.refreshHoveredElementAtScreen(mouseX, mouseY);
                widget.mouseMoved(mouseX, mouseY);
                for (int button = GLFW.GLFW_MOUSE_BUTTON_1; button <= GLFW.GLFW_MOUSE_BUTTON_3; button++) {
                    if (current.isMouseButtonDown(button)) {
                        widget.mouseDragged(mouseX, mouseY, button, mouseX - previousX, mouseY - previousY);
                        break;
                    }
                }
            }
            case OsWindowEvent.MouseButton mouse -> {
                // Re-read the cursor rather than trusting the last CursorPos: a window created under
                // the pointer, or one re-entered after the pointer was parked off-screen by
                // CursorEnter, can take a click before any movement is reported.
                mouseX = toGuiX(current.getCursorX(), currentSurface);
                mouseY = toGuiY(current.getCursorY(), currentSurface);
                if (mouse.button() == GLFW.GLFW_MOUSE_BUTTON_1) {
                    if (mouse.action() == GLFW.GLFW_PRESS && beginGesture()) {
                        return; // the press drives the window, the UI must not also see it
                    }
                    if (mouse.action() == GLFW.GLFW_RELEASE && gesture != Gesture.NONE) {
                        gesture = Gesture.NONE;
                        updateCursorShape();
                        return;
                    }
                }
                modularUI.refreshHoveredElementAtScreen(mouseX, mouseY);
                if (mouse.action() == GLFW.GLFW_PRESS) {
                    widget.mouseClicked(mouseX, mouseY, mouse.button());
                } else if (mouse.action() == GLFW.GLFW_RELEASE) {
                    widget.mouseReleased(mouseX, mouseY, mouse.button());
                }
            }
            case OsWindowEvent.Scroll scroll -> {
                modularUI.refreshHoveredElementAtScreen(mouseX, mouseY);
                widget.mouseScrolled(mouseX, mouseY, scroll.deltaX(), scroll.deltaY());
            }
            case OsWindowEvent.Key key -> {
                if (key.action() == GLFW.GLFW_RELEASE) {
                    widget.keyReleased(key.key(), key.scancode(), key.mods());
                } else {
                    // PRESS and REPEAT both, so held arrows and backspace behave in a text field.
                    widget.keyPressed(key.key(), key.scancode(), key.mods());
                }
            }
            case OsWindowEvent.Char typed -> {
                for (var character : Character.toChars(typed.codepoint())) {
                    widget.charTyped(character, typed.mods());
                }
            }
            case OsWindowEvent.CursorEnter enter -> {
                if (!enter.entered()) {
                    // Park the pointer well outside so MOUSE_LEAVE fires and hover state clears;
                    // otherwise an element stays highlighted after the cursor has gone.
                    mouseX = CURSOR_OUTSIDE;
                    mouseY = CURSOR_OUTSIDE;
                    modularUI.refreshHoveredElementAtScreen(mouseX, mouseY);
                    widget.mouseMoved(mouseX, mouseY);
                }
            }
            case OsWindowEvent.FramebufferSize size -> {
                currentSurface.resize(size.width(), size.height(),
                        current.getWindowWidth(), current.getWindowHeight());
                modularUI.init(currentSurface.guiScaledWidth(), currentSurface.guiScaledHeight());
            }
            case OsWindowEvent.Focus focus -> widget.setFocused(focus.focused());
            case OsWindowEvent.FileDrop drop -> modularUI.onFilesDrop(drop.files(), currentSurface);
            case OsWindowEvent.CloseRequest ignored -> onCloseRequested();
            case OsWindowEvent.WindowPos ignored -> {
                // Recorded on the window; nothing in the UI depends on where it sits.
            }
        }
    }

    private static float toGuiX(double windowX, OffscreenSurface surface) {
        return (float) (windowX * surface.guiScaledWidth() / Math.max(1, surface.screenWidth()));
    }

    private static float toGuiY(double windowY, OffscreenSurface surface) {
        return (float) (windowY * surface.guiScaledHeight() / Math.max(1, surface.screenHeight()));
    }

    @Override
    public void onCloseRequested() {
        if (onCloseRequested != null) {
            onCloseRequested.run();
        } else {
            close();
        }
    }

    // --------------------------------------------------------------------- move and resize gestures

    /**
     * Whether {@code element} should keep a press to itself rather than let it move the window.
     *
     * <p>Buttons by default — a close button in the title bar has to stay clickable. Override to add
     * your own controls.
     */
    protected boolean isDragBlocker(UIElement element) {
        return element instanceof Button;
    }

    /**
     * @return whether the press landed on a window edge or the drag area, in which case it drives the
     *         window instead of the UI
     */
    protected boolean beginGesture() {
        var current = window();
        var edges = resizable && !current.isMaximized()
                ? edgesAt(current.getCursorX(), current.getCursorY(),
                          current.getWindowWidth(), current.getWindowHeight())
                : 0;

        if (edges != 0) {
            gesture = Gesture.RESIZE;
        } else if (isOverDragArea()) {
            gesture = Gesture.MOVE;
        } else {
            return false;
        }

        resizeEdges = edges;
        var global = current.queryGlobalCursor();
        grabGlobalX = global[0];
        grabGlobalY = global[1];
        grabWindowX = current.getPositionX();
        grabWindowY = current.getPositionY();
        grabWindowWidth = current.getWindowWidth();
        grabWindowHeight = current.getWindowHeight();
        return true;
    }

    /**
     * Whether the pointer is over something that should move the window.
     *
     * <p>Walks up from the element under the cursor and takes whichever comes first: a blocker, or
     * the drag area. That is what makes the whole title bar draggable including its label — testing
     * for one specific element instead would leave the window only draggable in the empty gap beside
     * the text, which is exactly as annoying as it sounds.
     */
    protected boolean isOverDragArea() {
        var area = dragArea;
        if (area == null) return false;
        var hit = modularUI.hitTestAtScreen(mouseX, mouseY);
        for (var element = hit; element != null; element = element.getParent()) {
            if (isDragBlocker(element)) return false;
            if (element == area) return true;
        }
        return false;
    }

    private static int edgesAt(double x, double y, int width, int height) {
        var edges = 0;
        if (x <= RESIZE_BORDER) edges |= EDGE_LEFT;
        if (x >= width - RESIZE_BORDER) edges |= EDGE_RIGHT;
        if (y <= RESIZE_BORDER) edges |= EDGE_TOP;
        if (y >= height - RESIZE_BORDER) edges |= EDGE_BOTTOM;
        return edges;
    }

    /**
     * Points the cursor at whatever the current position would do — a resize arrow on an edge, the
     * normal arrow everywhere else. Without it an undecorated window gives no sign that its edges are
     * grabbable at all.
     */
    protected void updateCursorShape() {
        var current = window;
        if (current == null || current.isDestroyed()) return;
        if (gesture != Gesture.NONE) return;
        var edges = resizable && !current.isMaximized()
                ? edgesAt(current.getCursorX(), current.getCursorY(),
                          current.getWindowWidth(), current.getWindowHeight())
                : 0;
        current.setCursorShape(cursorShapeFor(edges));
    }

    private static int cursorShapeFor(int edges) {
        var horizontal = (edges & (EDGE_LEFT | EDGE_RIGHT)) != 0;
        var vertical = (edges & (EDGE_TOP | EDGE_BOTTOM)) != 0;
        if (horizontal && vertical) {
            var topLeft = (edges & EDGE_LEFT) != 0 && (edges & EDGE_TOP) != 0;
            var bottomRight = (edges & EDGE_RIGHT) != 0 && (edges & EDGE_BOTTOM) != 0;
            // The diagonal shapes need GLFW 3.4 and a desktop that provides them; OsWindow falls back
            // to leaving the previous shape when one is missing.
            return topLeft || bottomRight ? GLFW.GLFW_RESIZE_NWSE_CURSOR : GLFW.GLFW_RESIZE_NESW_CURSOR;
        }
        if (horizontal) return GLFW.GLFW_HRESIZE_CURSOR;
        if (vertical) return GLFW.GLFW_VRESIZE_CURSOR;
        return GLFW.GLFW_ARROW_CURSOR;
    }

    /**
     * Applies the in-progress gesture from the cursor's current position.
     *
     * <p>Everything is measured against the grab point in virtual-screen coordinates, never
     * incrementally: a moving window changes its own coordinate space under the cursor, so an
     * incremental delta either collapses to zero (move, left, top) or compounds (right, bottom).
     */
    protected void applyGesture() {
        var current = window();
        var global = current.queryGlobalCursor();
        var deltaX = (int) Math.round(global[0] - grabGlobalX);
        var deltaY = (int) Math.round(global[1] - grabGlobalY);

        if (gesture == Gesture.MOVE) {
            current.setPosition(grabWindowX + deltaX, grabWindowY + deltaY);
            return;
        }

        var x = grabWindowX;
        var y = grabWindowY;
        var width = grabWindowWidth;
        var height = grabWindowHeight;
        if ((resizeEdges & EDGE_LEFT) != 0) {
            // Clamp the delta rather than the result, so the edge stops instead of the window sliding
            // once the minimum is hit.
            var clamped = Math.min(deltaX, grabWindowWidth - MIN_WIDTH);
            x = grabWindowX + clamped;
            width = grabWindowWidth - clamped;
        } else if ((resizeEdges & EDGE_RIGHT) != 0) {
            width = Math.max(MIN_WIDTH, grabWindowWidth + deltaX);
        }
        if ((resizeEdges & EDGE_TOP) != 0) {
            var clamped = Math.min(deltaY, grabWindowHeight - MIN_HEIGHT);
            y = grabWindowY + clamped;
            height = grabWindowHeight - clamped;
        } else if ((resizeEdges & EDGE_BOTTOM) != 0) {
            height = Math.max(MIN_HEIGHT, grabWindowHeight + deltaY);
        }

        if (x != grabWindowX || y != grabWindowY) {
            current.setPosition(x, y);
        }
        current.setSize(width, height);
    }

    // ----------------------------------------------------------------------------------- drawing

    /**
     * Brings this window's stylesheets in line with {@link #setStyleSource}'s.
     *
     * <p>Re-checked every frame rather than copied once, so a theme switched while the window is open
     * reaches it too. The comparison is by identity over a handful of entries, which costs nothing.
     */
    protected void syncStylesheets() {
        var source = styleSource;
        if (source == null) return;
        var sheets = source.getStyleEngine().globalSheets;
        if (mirroredStylesheets.size() == sheets.size() && mirroredStylesheets.equals(sheets)) return;
        var engine = modularUI.getStyleEngine();
        for (var sheet : mirroredStylesheets) {
            if (!sheets.contains(sheet)) {
                engine.removeStylesheet(sheet);
            }
        }
        for (var sheet : sheets) {
            if (!mirroredStylesheets.contains(sheet)) {
                engine.addStylesheet(sheet);
            }
        }
        mirroredStylesheets.clear();
        mirroredStylesheets.addAll(sheets);
    }

    @Override
    public void renderFrame(float partialTick) {
        var current = window;
        var currentSurface = surface;
        if (current == null || currentSurface == null || current.isIconified()) return;

        sampleRestoredBounds();
        syncStylesheets();

        // The gui scale is shared with the game window (see OffscreenSurface), so a change to the
        // option relays out this UI too.
        var guiScale = currentSurface.guiScale();
        if (guiScale != lastGuiScale
                || modularUI.getScreenWidth() != currentSurface.guiScaledWidth()
                || modularUI.getScreenHeight() != currentSurface.guiScaledHeight()) {
            lastGuiScale = guiScale;
            modularUI.init(currentSurface.guiScaledWidth(), currentSurface.guiScaledHeight());
        }

        var minecraft = Minecraft.getInstance();
        var target = currentSurface.target();
        var modelView = RenderSystem.getModelViewStack();
        RenderSystem.backupProjectionMatrix();
        try (var ignoredActive = ModularUI.scopedActive(modularUI)) {
            target.bindWrite(true);
            RenderSystem.clearColor(0, 0, 0, 1);
            RenderSystem.clear(GL11C.GL_COLOR_BUFFER_BIT | GL11C.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.viewport(0, 0, target.width, target.height);

            // Start from a known state rather than inheriting whatever the last thing to draw this
            // frame left behind — a shader mod's composite pass, most likely.
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            // The gui projection Minecraft sets up for its own frame, against our target's size.
            var farPlane = ClientHooks.getGuiFarPlane();
            var projection = new Matrix4f().setOrtho(0f,
                    (float) (target.width / guiScale),
                    (float) (target.height / guiScale),
                    0f, 1000f, farPlane);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
            modelView.pushMatrix();
            modelView.translation(0f, 0f, 10000f - farPlane);
            RenderSystem.applyModelViewMatrix();
            Lighting.setupFor3DItems();

            var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            renderContents(graphics, partialTick);
            graphics.flush();

            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        } finally {
            RenderSystem.restoreProjectionMatrix();
            // Minecraft unbinds and blits its own target immediately after this hook, so put both the
            // binding and the viewport back exactly as they were.
            var mainTarget = minecraft.getMainRenderTarget();
            mainTarget.bindWrite(true);
            RenderSystem.viewport(0, 0, mainTarget.width, mainTarget.height);
        }
    }

    /**
     * Draws the UI itself. Split out so a subclass can put something behind or in front of it.
     */
    protected void renderContents(GuiGraphics graphics, float partialTick) {
        var currentSurface = surface;
        if (currentSurface == null) return;
        modularUI.getWidget().render(graphics, (int) mouseX, (int) mouseY, partialTick, currentSurface);
    }

    @Override
    public void present() {
        var currentSurface = surface;
        if (currentSurface == null) return;
        OsWindowManager.present(this, currentSurface.colorTextureId(),
                currentSurface.framebufferWidth(), currentSurface.framebufferHeight());
    }
}
