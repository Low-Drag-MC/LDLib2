package com.lowdragmc.lowdraglib2.client.window;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * The GLFW window hints a second window has to be created with.
 *
 * <p>The context hints are read back from the game window's actual context rather than assumed.
 * Matching them is not cosmetic: two contexts can only share objects if they were created against a
 * compatible pixel format, and the surest way to get that is to ask for exactly what the first one got.
 *
 * <p>{@code glfwWindowHint} state is process-global and sticky, so every call here starts from
 * {@link GLFW#glfwDefaultWindowHints()} — a launcher shim or another mod may have left hints set.
 */
public final class OsWindowHints {

    private static boolean focusOnShow = true;

    private OsWindowHints() {
    }

    /**
     * Whether a child window takes the operating system's focus when it is shown.
     *
     * <p>Turned off for the duration of an automated run: a scenario can tear a view out into a real
     * window halfway through, and with focus-on-show that window takes the keyboard away from
     * whatever the person at the machine is doing. Only <em>focus</em> is governed — the window is
     * still raised, because "show this window behind the others" has no portable spelling.
     */
    public static void setFocusOnShow(boolean focusOnShow) {
        OsWindowHints.focusOnShow = focusOnShow;
    }

    /**
     * Resets the hints and asks for the context the game window has.
     *
     * <p>Not Minecraft's own request, OpenGL 3.2: under NeoForge the game window is the early loading
     * screen's, which takes the highest version it can get — 4.1 on macOS — and macOS will not share
     * objects between a 3.2 and a 4.x core context, so a child asking for 3.2 there is never created.
     * The creation API is replayed for the same reason: an EGL and a native context cannot share
     * either.
     *
     * <p>Forward-compat comes along with the core profile, so the new context has no default vertex
     * array object — which is fine, because presenting is a framebuffer blit and needs neither a VAO
     * nor a program.
     */
    private static void applyGameContextHints() {
        GLFW.glfwDefaultWindowHints();
        var game = Minecraft.getInstance().getWindow().handle();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_CLIENT_API));
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_CONTEXT_CREATION_API));
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_CONTEXT_VERSION_MAJOR));
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_CONTEXT_VERSION_MINOR));
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_OPENGL_PROFILE));
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.glfwGetWindowAttrib(game, GLFW.GLFW_OPENGL_FORWARD_COMPAT));
    }

    /**
     * Applies the hints specific to a child window, on top of {@link #applyGameContextHints()}.
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
        applyGameContextHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, focusOnShow ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    }
}
