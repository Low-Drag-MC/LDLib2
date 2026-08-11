package com.lowdragmc.lowdraglib2.client.window;

import org.lwjgl.glfw.GLFW;

/**
 * The GLFW window hints a second window has to be created with.
 *
 * <p>The context hints are Minecraft's own, replayed verbatim from
 * {@code com.mojang.blaze3d.platform.Window}'s constructor. Matching them is not cosmetic: two
 * contexts can only share objects if they were created against a compatible pixel format, and the
 * surest way to get that is to ask for exactly what the first one asked for.
 *
 * <p>{@code glfwWindowHint} state is process-global and sticky, so every call here starts from
 * {@link GLFW#glfwDefaultWindowHints()} — a launcher shim or another mod may have left hints set.
 */
public final class OsWindowHints {

    private OsWindowHints() {
    }

    /**
     * Resets the hints and applies Minecraft's context request: OpenGL 3.2 core, native context API,
     * forward-compatible. Note forward-compat is set on <em>every</em> platform, not just macOS, so
     * the new context has no default vertex array object either — which is fine, because presenting
     * is a framebuffer blit and needs neither a VAO nor a program.
     */
    private static void applyMinecraftContextHints() {
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    }

    /**
     * Applies the hints specific to a child window, on top of {@link #applyMinecraftContextHints()}.
     *
     * <p>Created hidden so the first frame is presented before anything is shown — otherwise the
     * window flashes white. {@code GLFW_SCALE_TO_MONITOR} is deliberately left off, matching
     * Minecraft: with it on, the framebuffer and window sizes stop tracking each other predictably
     * across monitors.
     *
     * @param decorated whether the OS draws a title bar and resize border. Undecorated avoids the
     *                  platform's modal move/resize loop, which runs nested inside
     *                  {@code glfwPollEvents} and freezes the whole game for as long as the user
     *                  drags the window.
     */
    public static void applyChildWindowHints(boolean decorated) {
        applyMinecraftContextHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    }
}
