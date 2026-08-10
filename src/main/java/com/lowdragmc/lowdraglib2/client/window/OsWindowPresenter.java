package com.lowdragmc.lowdraglib2.client.window;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Puts a texture rendered in Minecraft's context onto a second window's screen.
 *
 * <p>Deliberately the smallest thing that can work: one framebuffer object, one
 * {@code glBlitFramebuffer}, one swap. No shader, no vertex array, no vertex data — which matters
 * because everything here runs with the other context current, where {@code RenderSystem} is
 * off-limits (see {@link GlContextScope}) and a forward-compatible core profile has no default
 * vertex array object to fall back on.
 *
 * <p>Textures are shared between the two contexts; framebuffer objects are <em>not</em>, so the FBO
 * has to be created here rather than reused from Minecraft's side.
 *
 * <p>No vertical flip. The source texture and the window's default framebuffer are both bottom-up,
 * which is also why {@code RenderTarget#blitToScreen} does not flip either.
 */
@OnlyIn(Dist.CLIENT)
public final class OsWindowPresenter {

    private final OsWindow window;
    @Nullable
    private GLCapabilities capabilities;
    private int framebuffer;

    public OsWindowPresenter(OsWindow window) {
        this.window = window;
    }

    /**
     * Blits {@code textureId} into the window and swaps.
     *
     * @param textureId the colour attachment of the render target the UI was drawn into, in
     *                  Minecraft's context
     * @param sourceWidth  the source's framebuffer width
     * @param sourceHeight the source's framebuffer height
     */
    public void present(int textureId, int sourceWidth, int sourceHeight) {
        if (window.isDestroyed() || textureId <= 0) return;
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
     * Releases the framebuffer object. Must run before the window is destroyed — the object belongs
     * to that window's context, and there is nowhere to delete it from afterwards.
     */
    public void destroy() {
        if (framebuffer == 0 || capabilities == null || window.isDestroyed()) {
            framebuffer = 0;
            capabilities = null;
            return;
        }
        try (var ignored = GlContextScope.enter(window.handle(), capabilities)) {
            GL30C.glDeleteFramebuffers(framebuffer);
        } finally {
            framebuffer = 0;
            capabilities = null;
        }
    }
}
