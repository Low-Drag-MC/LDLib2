package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState;
import com.lowdragmc.lowdraglib2.uitest.InputMode;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Delivers one primitive input event. Gestures (click, drag, type) are built out of these by the
 * scenario builder, one primitive per step so that each gets its own rendered frame.
 *
 * <p>All coordinates are GUI-scaled screen space — the same space
 * {@link Screen#mouseClicked(double, double, int)} takes, and the space {@code ElementBounds}
 * produces.
 */
@OnlyIn(Dist.CLIENT)
public abstract class InputDriver {

    /** The logical cursor position, kept so relative moves and drag deltas have something to work from. */
    protected float cursorX;
    protected float cursorY;
    /** The position the last {@code mouseMoved}/{@code mouseDragged} was dispatched at. */
    protected float dispatchedX;
    protected float dispatchedY;
    /**
     * Keys the harness considers held. Backs the {@link KeyState} override so that
     * {@code UIElement.isShiftDown()} and friends see synthetic modifiers — {@code glfwGetKey} reads
     * the physical keyboard, which nothing inside the process can move.
     */
    private final IntOpenHashSet heldKeys = new IntOpenHashSet();
    /** Kept so {@link #uninstall()} can prove the installed override is this driver's. */
    @Nullable
    private KeyState.Source keyStateSource;

    public static InputDriver of(InputMode mode) {
        return mode == InputMode.REAL ? new RealInputDriver() : new SyntheticInputDriver();
    }

    /**
     * Routes {@link KeyState} through this driver's held-key set. Must be paired with
     * {@link #uninstall()}; a leaked override makes the real keyboard stop working.
     */
    public void install() {
        if (keyStateSource == null) {
            keyStateSource = heldKeys::contains;
        }
        KeyState.setSource(keyStateSource);
    }

    public void uninstall() {
        heldKeys.clear();
        if (keyStateSource != null) {
            // Only ours, so an overlapping owner's override survives our teardown.
            KeyState.clearSource(keyStateSource);
        }
    }

    protected void markHeld(int keyCode, boolean held) {
        if (held) {
            heldKeys.add(keyCode);
        } else {
            heldKeys.remove(keyCode);
        }
    }

    /** The modifier bitmask implied by the currently held keys, for events that carry one. */
    public int heldModifiers() {
        int modifiers = 0;
        if (heldKeys.contains(GLFW.GLFW_KEY_LEFT_SHIFT) || heldKeys.contains(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (heldKeys.contains(GLFW.GLFW_KEY_LEFT_CONTROL) || heldKeys.contains(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (heldKeys.contains(GLFW.GLFW_KEY_LEFT_ALT) || heldKeys.contains(GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        return modifiers;
    }

    /**
     * Moves the cursor without telling the UI about it. Used as the first half of a two-step gesture,
     * where the hover has to settle before the event is dispatched.
     */
    public abstract void placeCursor(float x, float y);

    /** Moves the cursor and dispatches {@code mouseMoved}, so hover enter/leave fire. */
    public abstract void moveTo(float x, float y);

    /** Dispatches {@code mouseMoved} + {@code mouseDragged} with the delta since the last dispatch. */
    public abstract void dragTo(float x, float y, int button);

    public abstract void mouseDown(float x, float y, int button);

    public abstract void mouseUp(float x, float y, int button);

    public abstract void scroll(float x, float y, double amount);

    public abstract void keyDown(int keyCode, int modifiers);

    public abstract void keyUp(int keyCode, int modifiers);

    public abstract void charTyped(char codePoint, int modifiers);

    /**
     * Makes the UI's cached hover match the given position.
     *
     * <p>Nothing on {@code ModularUIWidget} hit-tests: every mouse method routes to
     * {@code getLastHoveredElement()}, which is only recomputed while rendering. Without this call an
     * event dispatched right after a cursor move resolves against the <em>previous</em> hover, and
     * the click silently does nothing — the single most confusing failure mode in UI automation.
     */
    protected void syncHover(float x, float y) {
        var modularUI = currentModularUI();
        if (modularUI != null) {
            modularUI.refreshHoveredElementAtScreen(x, y);
        }
    }

    @Nullable
    protected static Screen screen() {
        return Minecraft.getInstance().screen;
    }

    @Nullable
    protected static ModularUI currentModularUI() {
        return ModularUI.of(screen());
    }

    /**
     * Mirrors the OS cursor so the visible pointer, tooltips and the carried-item render agree with
     * the logical position. Fire-and-forget in synthetic mode; load-bearing in real mode.
     */
    protected static void warpOsCursor(float guiX, float guiY) {
        var window = Minecraft.getInstance().getWindow();
        double physicalX = guiX * window.getScreenWidth() / (double) Math.max(1, window.getGuiScaledWidth());
        double physicalY = guiY * window.getScreenHeight() / (double) Math.max(1, window.getGuiScaledHeight());
        GLFW.glfwSetCursorPos(window.getWindow(), physicalX, physicalY);
    }
}
