package com.lowdragmc.lowdraglib2.gui.ui.utils;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

/**
 * Where the game window's pointer is, as seen by everything that asks Minecraft rather than the
 * operating system — {@code MouseHandler#xpos()/ypos()}, and therefore the {@code mouseX}/{@code
 * mouseY} every {@code Screen#render} receives, and therefore {@code ModularUI}'s per-frame hover.
 *
 * <p>The sibling of {@link KeyState}, and it exists for the same reason. The only way to move the
 * pointer from inside the process is {@code glfwSetCursorPos}, which moves the pointer of whoever is
 * sitting at the machine, and which GLFW ignores entirely while the window is unfocused. Automation
 * built on it can therefore only run in the foreground of an idle machine. With this seam the
 * position is simply reported, so a run needs neither focus nor the user's mouse.
 *
 * <p>This describes the <em>game</em> window only. A UI hosted in its own OS window reads that
 * window's own cursor through {@code OsWindow}, and is unaffected.
 *
 * @see #setSource(Source)
 */
public final class CursorState {

    /** Where the pointer position comes from, in GUI-scaled screen coordinates. */
    public interface Source {
        float cursorX();

        float cursorY();
    }

    @Nullable
    private static Source source;

    private CursorState() {
    }

    /**
     * Overrides where the pointer is read from, or restores the real one with {@code null}.
     *
     * <p>Intended for automated tests and scripted playback. Anything that sets this <b>must</b>
     * clear it again, including on failure — a leaked override pins the pointer wherever the leak
     * left it and the real mouse stops working. Prefer {@link #clearSource(Source)} for that, so
     * overlapping owners cannot clear each other's.
     */
    public static void setSource(@Nullable Source source) {
        CursorState.source = source;
    }

    /**
     * Clears the override only if {@code owner} is the one currently installed.
     *
     * @see KeyState#clearSource(KeyState.Source)
     */
    public static void clearSource(Source owner) {
        if (source == owner) {
            source = null;
        }
    }

    @Nullable
    public static Source getSource() {
        return source;
    }

    /**
     * Converts a GUI-scaled x into the space {@code MouseHandler#xpos()} reports in — the exact
     * inverse of the expression {@code GameRenderer} uses to convert it back, so the round trip is
     * lossless apart from that method's own truncation to int.
     *
     * <p>Deliberately reads the window and nothing else: this is what the mixin that <em>overrides</em>
     * {@code xpos()} calls, and a path back through the getter would recurse.
     */
    public static double toPhysicalX(float guiX) {
        var window = Minecraft.getInstance().getWindow();
        return guiX * window.getScreenWidth() / (double) Math.max(1, window.getGuiScaledWidth());
    }

    /** @see #toPhysicalX(float) */
    public static double toPhysicalY(float guiY) {
        var window = Minecraft.getInstance().getWindow();
        return guiY * window.getScreenHeight() / (double) Math.max(1, window.getGuiScaledHeight());
    }
}
