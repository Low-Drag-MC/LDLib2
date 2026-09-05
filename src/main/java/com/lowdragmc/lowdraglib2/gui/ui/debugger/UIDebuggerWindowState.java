package com.lowdragmc.lowdraglib2.gui.ui.debugger;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Optional;

/**
 * Where the debugger window was last left: its size, its position, and whether it was pinned above
 * the other windows.
 *
 * <p>Remembered because the debugger is opened and closed constantly — that is what it is for — and
 * a tool that reverts to a default rectangle in the middle of the screen every time has to be
 * re-arranged every time. Kept across game sessions as well as across closes, in one small NBT file
 * next to the editor's own saved layouts, for the same reason.
 *
 * <p>The maximized state is deliberately <em>not</em> remembered. {@code glfwMaximizeWindow} is
 * {@code ShowWindow(SW_MAXIMIZE)} on Win32, which activates the window — restoring it on open would
 * make the debugger steal the keyboard, and would do so from inside an automated run's frame hook.
 * Restoring the pre-maximize rectangle is what "restore" means anyway.
 *
 * @param x          window position, or {@link #UNPLACED} to let the platform decide
 * @param alwaysOnTop whether the window was pinned above the others
 */
//@OnlyIn(Dist.CLIENT)
public record UIDebuggerWindowState(int x, int y, int width, int height, boolean alwaysOnTop) {

    /** No remembered position; the platform places the window. */
    public static final int UNPLACED = Integer.MIN_VALUE;

    /**
     * Roomier than the 200x200 in-game panel, which has to stay small to leave the inspected UI
     * visible around it. A window has no such constraint, and the hierarchy plus the inspector is a
     * two-pane layout that is genuinely unusable narrow.
     */
    private static final UIDebuggerWindowState DEFAULT =
            new UIDebuggerWindowState(UNPLACED, UNPLACED, 720, 520, false);

    /** Matches {@code ModularUIWindow}'s own resize floor, so a stored size cannot undercut it. */
    private static final int MIN_WIDTH = 200;
    private static final int MIN_HEIGHT = 150;

    private static final String FILE_NAME = "debugger_window.nbt";

    /** Read once per session; every later get and put goes through here. */
    @Nullable
    private static UIDebuggerWindowState cached;

    public static UIDebuggerWindowState get() {
        if (cached == null) {
            cached = read().orElse(DEFAULT);
        }
        return cached;
    }

    /** Records the new state and writes it out. Cheap enough: this happens when a window closes. */
    public static void put(UIDebuggerWindowState state) {
        if (state.equals(cached)) return;
        cached = state;
        write(state);
    }

    /**
     * This position shifted by a cascade offset, or {@link #UNPLACED} if there is no position to
     * shift — adding to {@code Integer.MIN_VALUE} would land the window somewhere unrecoverable.
     */
    public int xOffsetBy(int offset) {
        return x == UNPLACED ? UNPLACED : x + offset;
    }

    public int yOffsetBy(int offset) {
        return y == UNPLACED ? UNPLACED : y + offset;
    }

    private static File file() {
        return new File(LDLib2.getAssetsDir().getParentFile(), FILE_NAME);
    }

    private static Optional<UIDebuggerWindowState> read() {
        var file = file();
        if (!file.exists()) return Optional.empty();
        try {
            var tag = NbtIo.read(file.toPath());
            if (tag == null) return Optional.empty();
            return Optional.of(new UIDebuggerWindowState(
                    tag.getIntOr("x", UNPLACED),
                    tag.getIntOr("y", UNPLACED),
                    // A missing or nonsensical size falls back to the default rather than to zero:
                    // a one-pixel window is worse than a badly placed one, and unrecoverable without
                    // finding this file.
                    Math.max(MIN_WIDTH, tag.getIntOr("width", DEFAULT.width)),
                    Math.max(MIN_HEIGHT, tag.getIntOr("height", DEFAULT.height)),
                    tag.getBooleanOr("alwaysOnTop", false)));
        } catch (Exception e) {
            // A corrupt file must cost the user a remembered rectangle, not the debugger.
            return Optional.empty();
        }
    }

    private static void write(UIDebuggerWindowState state) {
        var tag = new CompoundTag();
        if (state.x != UNPLACED) tag.putInt("x", state.x);
        if (state.y != UNPLACED) tag.putInt("y", state.y);
        tag.putInt("width", state.width);
        tag.putInt("height", state.height);
        tag.putBoolean("alwaysOnTop", state.alwaysOnTop);
        try {
            NbtIo.write(tag, file().toPath());
        } catch (Exception e) {
            LDLib2.LOGGER.warn("[ui-debugger] could not save the debugger window's position", e);
        }
    }
}
