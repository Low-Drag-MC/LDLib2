package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.uitest.InputMode;

/**
 * {@link InputMode#REAL}: move the OS cursor and let Minecraft's own {@code MouseHandler} deliver
 * the movement on the next frame, instead of refreshing the hover directly.
 *
 * <p>Worth the frame coupling when a scenario needs code that reads GLFW rather than the event to
 * behave — {@code ModularUI#onFilesDrop} queries {@code glfwGetCursorPos} directly, for instance.
 * The runner gives every step its own frame anyway, so gesture expansion is identical in both modes.
 *
 * <p>Note this mode does <em>not</em> change what modifier keys report: {@code glfwGetKey} reads the
 * physical keyboard and nothing inside the process can move it. Modifier-sensitive behaviour is
 * covered by {@link com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState}, which the runner overrides for
 * the duration of a run in either mode.
 */
public class RealInputDriver extends SyntheticInputDriver {

    @Override
    public void placeCursor(float x, float y) {
        cursorX = x;
        cursorY = y;
        warpOsCursor(x, y);
        // Deliberately no syncHover: the point of this mode is to let Minecraft's own cursor
        // pipeline resolve the hover on the next frame, so the test exercises the real path.
    }
}
