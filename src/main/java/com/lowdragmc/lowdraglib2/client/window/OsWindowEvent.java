package com.lowdragmc.lowdraglib2.client.window;


import net.minecraft.client.input.PreeditEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

/**
 * An SDL event addressed to an {@link OsWindow}, recorded rather than acted on.
 *
 * <p>SDL has one event queue for the whole process, and vanilla drains it from two places that are
 * both bad times to run UI logic: the middle of {@code RenderSystem.flipFrame}, between the present
 * and the next frame; and inside {@code RenderSystem.limitDisplayFPS}, where the game is deliberately
 * idling. So {@link OsWindowEvents} takes the events addressed to our windows out of that queue as
 * they arrive and only copies them into these records; the host drains them at one defined point per
 * frame, immediately before it renders. That ordering also happens to be the one the UI requires:
 * nothing on {@code ModularUIWidget} hit-tests, so the hovered element has to be resolved before any
 * pointer event is dispatched.
 *
 * <p>Values are SDL's, the same ones vanilla's own input records carry: key codes are
 * {@code InputConstants.KEY_*} scancodes plus the layout-aware keycode, mouse buttons are
 * {@code InputConstants.MOUSE_BUTTON_*}, modifiers are {@code InputConstants.MOD_*}. Positions are in
 * window coordinates, which on a HiDPI display are not framebuffer pixels.
 */
public sealed interface OsWindowEvent {

    /**
     * Cursor moved. Coalesced on enqueue — the device reports far faster than the UI needs and only
     * the latest position matters.
     */
    record CursorPos(double x, double y) implements OsWindowEvent {
    }

    /**
     * @param button an {@code InputConstants.MOUSE_BUTTON_*} value, SDL's numbering
     * @param action {@code InputConstants.PRESS} or {@code RELEASE}
     */
    record MouseButton(int button, int action, int mods) implements OsWindowEvent {
    }

    record Scroll(double deltaX, double deltaY) implements OsWindowEvent {
    }

    /**
     * @param key     the physical key, an {@code InputConstants.KEY_*} scancode
     * @param keycode what the key means under the current layout, an SDL keycode
     * @param action  {@code InputConstants.PRESS}, {@code REPEAT} or {@code RELEASE}
     */
    record Key(int key, int keycode, int action, int mods) implements OsWindowEvent {
    }

    /**
     * Committed text. Only delivered while text input is started for the window, see
     * {@link OsWindow#startTextInput()}; one event can carry several characters — a whole IME
     * composition, say.
     */
    record Text(String text) implements OsWindowEvent {
    }

    /**
     * The input method's in-progress composition changed, or ended when {@code preedit} is null.
     */
    record Preedit(@Nullable PreeditEvent preedit) implements OsWindowEvent {
    }

    record CursorEnter(boolean entered) implements OsWindowEvent {
    }

    record FramebufferSize(int width, int height) implements OsWindowEvent {
    }

    record WindowPos(int x, int y) implements OsWindowEvent {
    }

    record Focus(boolean focused) implements OsWindowEvent {
    }

    /**
     * Files dragged in from outside the game and dropped at {@code (x, y)}.
     */
    record FileDrop(List<File> files, double x, double y) implements OsWindowEvent {
    }

    /**
     * The user asked to close the window. Purely advisory — nothing has been closed, the host decides.
     */
    record CloseRequest() implements OsWindowEvent {
    }

}
