package com.lowdragmc.lowdraglib2.client.window;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Which of the two ways a second window can be given a picture, decided by the graphics backend the
 * game started on.
 *
 * <p>26.2 is the first version where this is a question at all: the renderer gained a Vulkan backend
 * alongside OpenGL, and the two present to a window in completely different ways. Everything else
 * about a hosted UI is backend-neutral — it is drawn through {@code RenderSystem} into a
 * {@code TextureTarget} like any other off-screen pass — so this is the only axis that has to fork.
 *
 * <p>The test is on the game window's own {@code GLFW_CLIENT_API} rather than on anything inside
 * {@code RenderSystem}, because that attribute <em>is</em> the precondition: it says whether the
 * window GLFW created has a GL context for a second one to share. Reading it back from GLFW also
 * survives a backend that falls back at startup, which the option value alone does not — asking for
 * Vulkan on a driver without it lands on OpenGL.
 */
public enum OsWindowBackend {

    /**
     * The game holds an OpenGL context. A second window gets a context that shares its textures, and
     * the frame arrives as a framebuffer blit.
     */
    OPENGL,

    /**
     * No client API on the game's window, i.e. Vulkan. A second window carries no context at all; it
     * gets a swapchain of its own through {@code GpuDevice#createSurface} and the frame arrives as a
     * device-side blit into an acquired swapchain image.
     */
    SURFACE;

    /**
     * The backend this game is running. Cheap enough to call per frame — one GLFW attribute read —
     * and deliberately not cached, so it cannot go stale against a window recreated underneath us.
     */
    public static OsWindowBackend current() {
        var handle = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_CLIENT_API) == GLFW.GLFW_NO_API ? SURFACE : OPENGL;
    }

    public boolean isOpenGl() {
        return this == OPENGL;
    }
}
