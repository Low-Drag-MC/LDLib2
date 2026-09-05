package com.lowdragmc.lowdraglib2.uitest.input;

import com.lowdragmc.lowdraglib2.client.window.OsWindowEvent;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Drives a UI hosted in its own operating-system window.
 *
 * <p>{@link InputDriver} cannot reach one: every mode it has ends at
 * {@code Minecraft.getInstance().screen}, and a {@link ModularUIWindow} has no screen — it takes its
 * input from raw GLFW callbacks queued on its own window. So this posts into that same queue, which
 * is what makes it a test of the window's real dispatch path rather than a way around it: the events
 * are drained, hit-tested and dispatched by exactly the code a physical click goes through, including
 * the move/resize gesture handling that decides whether the UI sees a press at all.
 *
 * <p>Coordinates are the window's own GUI-scaled space — which is what {@link ElementBounds} reports
 * for elements in that window's UI, and is <em>not</em> the game window's space even when the two
 * share a gui scale, because the window has its own size.
 *
 * <p>Post one primitive per scenario step, as with {@link InputDriver}. The queue is drained once per
 * frame, so several primitives posted together are all dispatched against the same layout and the
 * same hover, and any gesture that depends on a frame passing in between — a drag — will not start.
 */
//@OnlyIn(Dist.CLIENT)
public final class WindowInput {

    private final ModularUIWindow window;
    /** Where {@link #moveTo} last aimed, in the window's GUI space. NaN until it has been called. */
    private float aimedX = Float.NaN;
    private float aimedY = Float.NaN;

    private WindowInput(ModularUIWindow window) {
        this.window = window;
    }

    public static WindowInput of(ModularUIWindow window) {
        if (!window.isOpen()) {
            throw new IllegalStateException("The window is not open, so nothing would drain its events");
        }
        return new WindowInput(window);
    }

    private UISurface surface() {
        var surface = window.surface();
        if (surface == null) {
            throw new IllegalStateException("The window has no surface yet - it has not been attached");
        }
        return surface;
    }

    private double toWindowX(float guiX) {
        var surface = surface();
        return guiX * surface.screenWidth() / (double) Math.max(1, surface.guiScaledWidth());
    }

    private double toWindowY(float guiY) {
        var surface = surface();
        return guiY * surface.screenHeight() / (double) Math.max(1, surface.guiScaledHeight());
    }

    // ------------------------------------------------------------------------------------ pointer

    public WindowInput moveTo(float guiX, float guiY) {
        aimedX = guiX;
        aimedY = guiY;
        postCursor();
        return this;
    }

    private void postCursor() {
        window.window().post(new OsWindowEvent.CursorPos(toWindowX(aimedX), toWindowY(aimedY)));
    }

    /**
     * Re-states where this driver last aimed, immediately before a button event.
     *
     * <p>Not redundant with {@link #moveTo}. A window reads its own cached cursor when a button
     * arrives rather than trusting the last move it was told about, and that cache is also written by
     * the platform's real callbacks — so a physical mouse drifting across the window between the aim
     * and the press moves the synthetic click with it. Which is precisely the failure the whole
     * harness exists to be immune to: a run must not depend on where the person at the machine left
     * their pointer.
     */
    private void reaim() {
        if (!Float.isNaN(aimedX)) {
            postCursor();
        }
    }

    /**
     * Aims at an element's transformed centre. The element must belong to this window's UI and have
     * been drawn at least once, or its bounds are not yet in this window's coordinate space.
     */
    public WindowInput moveTo(UIElement element) {
        var bounds = ElementBounds.of(element);
        if (bounds.isEmpty()) {
            throw new IllegalStateException("Target " + element.getElementName() + " has zero size "
                    + bounds + " - it is probably not laid out yet");
        }
        return moveTo(bounds.centerX(), bounds.centerY());
    }

    /** Presses a button where {@link #moveTo} last aimed. */
    public WindowInput mouseDown(int button) {
        reaim();
        window.window().post(new OsWindowEvent.MouseButton(button, GLFW.GLFW_PRESS, 0));
        return this;
    }

    public WindowInput mouseUp(int button) {
        reaim();
        window.window().post(new OsWindowEvent.MouseButton(button, GLFW.GLFW_RELEASE, 0));
        return this;
    }

    public WindowInput scroll(double deltaX, double deltaY) {
        reaim();
        window.window().post(new OsWindowEvent.Scroll(deltaX, deltaY));
        return this;
    }

    /** Whether the pointer is inside the window. Leaving clears hover, as the platform's would. */
    public WindowInput cursorEnter(boolean entered) {
        window.window().post(new OsWindowEvent.CursorEnter(entered));
        return this;
    }

    // ----------------------------------------------------------------------------------- keyboard

    public WindowInput keyDown(int keyCode, int modifiers) {
        window.window().post(new OsWindowEvent.Key(keyCode, Keys.scanCodeOf(keyCode), GLFW.GLFW_PRESS, modifiers));
        return this;
    }

    public WindowInput keyUp(int keyCode, int modifiers) {
        window.window().post(new OsWindowEvent.Key(keyCode, Keys.scanCodeOf(keyCode), GLFW.GLFW_RELEASE, modifiers));
        return this;
    }

    /**
     * A press and a release in one go. Both land in the same drain, which is right for a chord
     * nothing observes mid-press — a shortcut — and wrong for one that does, such as a held modifier.
     */
    public WindowInput key(int keyCode, int modifiers) {
        return keyDown(keyCode, modifiers).keyUp(keyCode, modifiers);
    }

    public WindowInput charTyped(int codepoint, int modifiers) {
        window.window().post(new OsWindowEvent.Char(codepoint, modifiers));
        return this;
    }

    // -------------------------------------------------------------------------------------- window

    public WindowInput focus(boolean focused) {
        window.window().post(new OsWindowEvent.Focus(focused));
        return this;
    }

    /** What the platform's close button posts. Goes through the host's own close handling. */
    public WindowInput closeRequest() {
        window.window().post(new OsWindowEvent.CloseRequest());
        return this;
    }
}
