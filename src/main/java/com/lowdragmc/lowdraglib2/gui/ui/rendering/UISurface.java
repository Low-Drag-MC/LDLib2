package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.utils.Scope;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The destination a UI frame is drawn into: its framebuffer, its gui scale and its OS window.
 *
 * <p>Until this existed, everything read {@link Minecraft#getWindow()} and
 * {@link Minecraft#getMainRenderTarget()} directly, which is correct exactly as long as there is one
 * place a UI can appear. {@link #main()} is still the default and still resolves to those, so
 * nothing changes for an on-screen UI; the point is that a UI can now be rendered into an off-screen
 * target of a different size.
 *
 * <p>Two consumers depend on this and are easy to miss:
 * <ul>
 *   <li>the scissor box, which is computed by flipping the rect against the framebuffer height —
 *       get the height wrong and every clip in the frame is vertically offset;</li>
 *   <li>{@link UIVisualLayer}, which allocates its targets at the destination's size.</li>
 * </ul>
 *
 * <p>{@link #current()} exists for the draw code that only ever receives a
 * {@link net.minecraft.client.gui.GuiGraphics} — {@link com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture}
 * implementations — and so cannot be handed the surface explicitly.
 */
@OnlyIn(Dist.CLIENT)
public interface UISurface {

    /**
     * The render target being drawn into. Never null; the main surface reports Minecraft's.
     */
    RenderTarget target();

    /**
     * Physical framebuffer width in pixels, i.e. gui units multiplied by {@link #guiScale()}.
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
