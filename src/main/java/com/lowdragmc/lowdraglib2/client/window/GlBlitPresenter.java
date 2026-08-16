package com.lowdragmc.lowdraglib2.client.window;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * {@link OsWindowBackend#OPENGL}: puts a texture rendered in Minecraft's context onto a second
 * window's screen by sharing the context and blitting.
 *
 * <p>Deliberately the smallest thing that can work: one framebuffer object, one
 * {@code glBlitFramebuffer}, one swap. No shader, no vertex array, no vertex data — which matters
 * because everything here runs with the other context current, where {@code RenderSystem} is
 * off-limits (see {@link GlContextScope}) and a forward-compatible core profile has no default
 * vertex array object to fall back on.
 *
 * <p>That constraint is also why this does not simply go through {@code GpuDevice#createSurface}
 * like {@link GpuSurfacePresenter} does. {@code GlSurface} would blit into the default framebuffer
 * of whichever context happens to be current — the game's — and routing it through the other one
 * means {@code GlStateManager} calls against a context its static mirrors do not describe, which is
 * exactly the corruption {@link GlContextScope} exists to forbid.
 *
 * <p>Textures are shared between the two contexts; framebuffer objects are <em>not</em>, so the FBO
 * has to be created here rather than reused from Minecraft's side.
 *
 * <p>No vertical flip: the source texture and the window's default framebuffer are both bottom-up.
 * The same convention shows up in {@code GlCommandEncoder#presentTexture}, which is how the game
 * gets its own frame onto its own window.
 */
public final class GlBlitPresenter implements OsWindowPresenter {

    private final OsWindow window;
    @Nullable
    private GLCapabilities capabilities;
    private int framebuffer;

    public GlBlitPresenter(OsWindow window) {
        this.window = window;
    }

    @Override
    public void present(RenderTarget source) {
        if (window.isDestroyed()) return;
        var textureId = glIdOf(source);
        if (textureId <= 0) return;
        var sourceWidth = source.width;
        var sourceHeight = source.height;
        var destinationWidth = window.getFramebufferWidth();
        var destinationHeight = window.getFramebufferHeight();
        if (destinationWidth <= 0 || destinationHeight <= 0) return; // iconified

        if (capabilities == null) {
            capabilities = GlContextScope.createCapabilitiesFor(window.handle());
        }

        try (var ignored = GlContextScope.enter(window.handle(), capabilities)) {
            if (framebuffer == 0) {
                framebuffer = GL30C.glGenFramebuffers();
            }
            GL30C.glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, framebuffer);
            // Re-attached every frame rather than cached against the last id. Resizing a RenderTarget
            // destroys and recreates its colour texture, and the driver is free to hand the new one
            // the same name — so an id comparison can miss the swap and silently blit from a deleted
            // texture. One glFramebufferTexture2D per frame is not worth the risk of that bug.
            GL30C.glFramebufferTexture2D(GL30C.GL_READ_FRAMEBUFFER, GL30C.GL_COLOR_ATTACHMENT0,
                    GL11C.GL_TEXTURE_2D, textureId, 0);
            GL30C.glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, 0);
            GL11C.glDisable(GL11C.GL_SCISSOR_TEST);
            var filter = sourceWidth == destinationWidth && sourceHeight == destinationHeight
                    ? GL11C.GL_NEAREST : GL11C.GL_LINEAR;
            GL30C.glBlitFramebuffer(0, 0, sourceWidth, sourceHeight,
                    0, 0, destinationWidth, destinationHeight,
                    GL11C.GL_COLOR_BUFFER_BIT, filter);
            GLFW.glfwSwapBuffers(window.handle());
        }
    }

    /**
     * The raw GL name of {@code source}'s colour attachment, or {@code -1} if there is none.
     *
     * <p>Reaching through the GPU abstraction is deliberate: this id is read with a <em>different</em>
     * GL context current, to blit the frame into another window's default framebuffer. Nothing in
     * {@code RenderSystem} can express that, because everything there assumes the game's own context.
     */
    private static int glIdOf(RenderTarget target) {
        return target.getColorTexture() instanceof GlTexture glTexture ? glTexture.glId() : -1;
    }

    /**
     * Releases the framebuffer object. Must run before the window is destroyed — the object belongs
     * to that window's context, and there is nowhere to delete it from afterwards.
     */
    @Override
    public void destroy() {
        if (framebuffer != 0 && capabilities != null && !window.isDestroyed()) {
            try (var ignored = GlContextScope.enter(window.handle(), capabilities)) {
                GL30C.glDeleteFramebuffers(framebuffer);
            }
        }
        framebuffer = 0;
        capabilities = null;
    }
}
