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
 * <p>Deliberately smaller than its 1.21 counterpart. There, a surface also had to correct the
 * scissor box, because {@code GuiGraphics#applyScissor} issued a GL scissor computed against the
 * game window. Here the gui renderer is deferred: a clip rectangle rides along on each
 * {@code GuiElementRenderState} and is resolved against whatever target that draw range is flushed
 * into, so an off-screen frame clips correctly with no help. What is left is the part 26.1 cannot
 * infer — which window the cursor and keyboard belong to, and which target to composite into.
 *
 * <p>Two consumers depend on this and are easy to miss:
 * <ul>
 *   <li>file drops and cursor queries, which must go to <em>this</em> window: GLFW reports cursor
 *       position per window, and the game's would be stale or plain wrong;</li>
 *   <li>frame capture, which reads back this target rather than the game's own frame — a UI in its
 *       own window never appears in the latter.</li>
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
     * The render target currently being drawn into.
     *
     * <p>The replacement for {@code Minecraft.getInstance().getMainRenderTarget()} in any code that
     * means "the destination I am compositing into" — reading its size, copying its colour or depth,
     * writing a result back into it. Outside a UI pass, and for a UI in the game window, this
     * <em>is</em> the main render target, so substituting it changes nothing there; inside a UI drawn
     * into an off-screen target it is that target instead, which is the whole point.
     *
     * <p>Code that genuinely means the game's own frame regardless of where UI is being drawn — a
     * shader-pack integration reading the world's depth buffer, say — should keep asking Minecraft
     * directly.
     */
    static RenderTarget currentTarget() {
        return current().target();
    }

    /**
     * Install {@code surface} as {@link #current()} until the returned scope is closed. Renders
     * happen on the render thread one at a time, so a plain stack is enough.
     */
    static Scope push(UISurface surface) {
        return SurfaceStack.push(surface);
    }

}
