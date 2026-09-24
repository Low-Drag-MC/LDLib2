package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.uitest.InputMode;

/**
 * {@link InputMode#REAL}: move the OS cursor and let Minecraft's own {@code MouseHandler} deliver
 * the movement on the next frame, instead of refreshing the hover directly.
 *
 * <p>Worth the frame coupling when a scenario needs code that reads the platform rather than the
 * event to behave — {@code ModularUI#onFilesDrop} queries the pointer position directly, for instance.
 * The runner gives every step its own frame anyway, so gesture expansion is identical in both modes.
 *
 * <p>Note this mode does <em>not</em> change what modifier keys report: {@code SDL_GetKeyboardState}
 * reads the physical keyboard and nothing inside the process can move it. Modifier-sensitive behaviour is
 * covered by {@link com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState}, which the runner overrides for
 * the duration of a run in either mode.
 *
 * <p><b>Foreground only.</b> This is the one mode that moves the pointer of whoever is at the
 * machine and needs the game window focused and in front — the real pointer has to be over it for the
 * game to see the motion at all. Every other mode runs happily in the
 * background; see {@link com.lowdragmc.lowdraglib2.gui.ui.utils.CursorState}.
 */
public class RealInputDriver extends SyntheticInputDriver {

    @Override
    protected boolean usesVirtualCursor() {
        return false;
    }

    @Override
    public void placeCursor(float x, float y) {
        cursorX = x;
        cursorY = y;
        warpOsCursor(x, y);
        // Deliberately no syncHover: the point of this mode is to let Minecraft's own cursor
        // pipeline resolve the hover on the next frame, so the test exercises the real path.
    }
}
