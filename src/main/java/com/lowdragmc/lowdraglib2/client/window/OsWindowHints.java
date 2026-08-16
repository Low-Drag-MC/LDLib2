package com.lowdragmc.lowdraglib2.client.window;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * The GLFW window hints a second window has to be created with.
 *
 * <p>The context hints are Minecraft's own, replayed by calling the very same method the game does.
 * Matching them is not cosmetic: two contexts can only share objects if they were created against a
 * compatible pixel format, and the surest way to get that is to ask for exactly what the first one
 * asked for.
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
     * Resets the hints and asks for exactly what the game asked for, by asking the same object.
     *
     * <p>{@code GpuBackend#setWindowHints} is what {@code Window.createGlfwWindow} calls before
     * {@code glfwCreateWindow}, and {@code Window#backend()} hands back the backend that survived
     * startup — including the fallback case, where a request for Vulkan on a driver without it ends
     * up on OpenGL. Replaying it is therefore both shorter than copying the hints out and immune to
     * Mojang changing them; and drift there would not be cosmetic, since two contexts can only share
     * objects if their pixel formats are compatible.
     *
     * <p>What it sets, for reference: under OpenGL, 3.3 core with the native context API and
     * forward-compat — note the latter on <em>every</em> platform, not just macOS, so the new context
     * has no default vertex array object either, which is fine because presenting is a framebuffer
     * blit and needs neither a VAO nor a program. Under Vulkan, no client API at all, which is not
     * merely thrift: {@code glfwCreateWindowSurface} rejects a window that has one.
     */
    private static void applyMinecraftContextHints() {
        GLFW.glfwDefaultWindowHints();
        Minecraft.getInstance().getWindow().backend().setWindowHints();
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
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, focusOnShow ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, decorated ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    }
}
