package com.lowdragmc.lowdraglib2.editor.keymap;

import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.utils.KeyState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Optional;

/**
 * One key plus the modifiers held with it — what a user presses to run an {@link EditorAction}.
 *
 * <p>Modifiers are matched exactly: {@code Ctrl+S} does not fire on {@code Ctrl+Shift+S}, because that
 * is a different shortcut and usually a different action. Only Control, Shift and Alt take part;
 * Control means Command on macOS, which is where {@link KeyState#isCtrlOrCmdDown(int)} puts it.
 *
 * <p>The text form ({@code "ctrl+shift+s"}) is what ends up in the settings file, so it is part of the
 * config format: modifiers always in the order ctrl, shift, alt, then the key's
 * {@link KeyNames stable name}.
 *
 * <p>{@code keyCode} is an SDL <em>keycode</em> — the key's meaning under the current layout, the event's
 * {@link UIEvent#shortcutKey} — not the physical scancode in {@link UIEvent#keyCode}. So "ctrl+z" is the
 * key labelled Z on a QWERTY and an AZERTY keyboard alike, which is also how vanilla matches its own
 * copy and paste. Modifier bits are the {@code SDL_KMOD_*} masks, {@code InputConstants.MOD_*}.
 */
public record KeyChord(int keyCode, int modifiers) {
    public static final int MOD_SHIFT = InputConstants.MOD_SHIFT;
    public static final int MOD_CTRL = InputConstants.MOD_CONTROL;
    public static final int MOD_ALT = InputConstants.MOD_ALT;
    /** The modifiers a chord can carry. Super, caps lock and num lock are deliberately not among them. */
    public static final int MOD_MASK = MOD_SHIFT | MOD_CTRL | MOD_ALT;

    /** No key at all: an action that is not bound, and the value a cleared binding takes. */
    public static final KeyChord UNBOUND = new KeyChord(KeyNames.UNKNOWN, 0);

    public static final Codec<KeyChord> CODEC = Codec.STRING.comapFlatMap(
            text -> parse(text)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Not a key chord: " + text)),
            KeyChord::serialize);

    /**
     * Drops modifiers that take no part in a chord, and refuses a chord whose key is itself a modifier:
     * holding Shift is not a shortcut, and a keymap that accepted one would fire it while the user was
     * reaching for the real key.
     */
    public KeyChord {
        modifiers &= MOD_MASK;
        if (KeyNames.isModifier(keyCode) || KeyNames.nameOf(keyCode) == null) {
            keyCode = KeyNames.UNKNOWN;
            modifiers = 0;
        }
    }

    public static KeyChord of(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        return new KeyChord(keyCode, (ctrl ? MOD_CTRL : 0) | (shift ? MOD_SHIFT : 0) | (alt ? MOD_ALT : 0));
    }

    /**
     * The chord a key press <em>is</em>, read from the modifier bits the event itself carries.
     *
     * <p>⚠️ Not from {@link KeyState}'s polling methods, which ask what is held right now. The event's
     * bits are the ones the press was delivered with, so a test driver can state them outright. This is
     * the same source vanilla reads for {@code KeyEvent#isCopy} and friends.
     *
     * <p>{@code MOD_SUPER} is folded onto {@code MOD_CTRL} by
     * {@link KeyState#isCtrlOrCmdDown(int)} — a chord is stored as "ctrl+s" on every platform and
     * means whichever key that platform uses for shortcuts.
     */
    public static KeyChord fromEvent(UIEvent event) {
        return fromModifiers(event.shortcutKey, event.modifiers);
    }

    public static KeyChord fromModifiers(int keyCode, int modifiers) {
        return of(keyCode,
                KeyState.isCtrlOrCmdDown(modifiers),
                KeyState.isShiftDown(modifiers),
                KeyState.isAltDown(modifiers));
    }

    public static KeyChord key(int keyCode) {
        return new KeyChord(keyCode, 0);
    }

    public static KeyChord ctrl(int keyCode) {
        return new KeyChord(keyCode, MOD_CTRL);
    }

    public static KeyChord shift(int keyCode) {
        return new KeyChord(keyCode, MOD_SHIFT);
    }

    public static KeyChord alt(int keyCode) {
        return new KeyChord(keyCode, MOD_ALT);
    }

    public static KeyChord ctrlShift(int keyCode) {
        return new KeyChord(keyCode, MOD_CTRL | MOD_SHIFT);
    }

    public static KeyChord ctrlAlt(int keyCode) {
        return new KeyChord(keyCode, MOD_CTRL | MOD_ALT);
    }

    /** Whether this chord names a key at all. An unbound action never matches anything. */
    public boolean isBound() {
        return keyCode != KeyNames.UNKNOWN;
    }

    public boolean isCtrlDown() {
        return (modifiers & MOD_CTRL) != 0;
    }

    public boolean isShiftDown() {
        return (modifiers & MOD_SHIFT) != 0;
    }

    public boolean isAltDown() {
        return (modifiers & MOD_ALT) != 0;
    }

    /** Whether the chord is a bare key press, which is what must stand aside while the user is typing. */
    public boolean hasNoModifier() {
        return modifiers == 0;
    }

    /**
     * The text form stored in the settings file, or an empty string for {@link #UNBOUND}.
     *
     * <p>An unbound chord is normally left out of the file entirely rather than written as an empty
     * string; what says "the user cleared this" is the action having an entry at all, since an untouched
     * action has none. Both forms read back the same way.
     */
    public String serialize() {
        if (!isBound()) return "";
        var text = new StringBuilder();
        if (isCtrlDown()) text.append("ctrl+");
        if (isShiftDown()) text.append("shift+");
        if (isAltDown()) text.append("alt+");
        return text.append(KeyNames.nameOf(keyCode)).toString();
    }

    /**
     * Reads back {@link #serialize()}. Modifiers may be given in any order and any case; an empty string
     * is {@link #UNBOUND}. Returns empty for a name no keyboard here has, so a keymap written by a newer
     * version does not take the whole settings file down with it.
     */
    public static Optional<KeyChord> parse(String text) {
        if (text == null) return Optional.empty();
        var trimmed = text.trim().toLowerCase();
        if (trimmed.isEmpty()) return Optional.of(UNBOUND);
        var modifiers = 0;
        var parts = trimmed.split("\\+");
        for (int i = 0; i < parts.length - 1; i++) {
            switch (parts[i].trim()) {
                case "ctrl", "control", "cmd", "command" -> modifiers |= MOD_CTRL;
                case "shift" -> modifiers |= MOD_SHIFT;
                case "alt", "option" -> modifiers |= MOD_ALT;
                default -> {
                    return Optional.empty();
                }
            }
        }
        // "+" on its own splits to nothing at all, so the key name has to be guarded rather than indexed
        var keyName = parts.length == 0 ? "" : parts[parts.length - 1].trim();
        var keyCode = KeyNames.codeOf(keyName);
        if (keyCode == KeyNames.UNKNOWN) return Optional.empty();
        return Optional.of(new KeyChord(keyCode, modifiers));
    }

    /** Whether the key and the exact modifier state given are this chord. */
    public boolean matches(int keyCode, boolean ctrl, boolean shift, boolean alt) {
        return isBound() && this.keyCode == keyCode
                && isCtrlDown() == ctrl && isShiftDown() == shift && isAltDown() == alt;
    }

    /** How the chord is written in the UI, e.g. {@code Ctrl+Shift+S}. Empty when unbound. */
    public String toDisplayString() {
        if (!isBound()) return "";
        var text = new StringBuilder();
        if (isCtrlDown()) text.append("Ctrl+");
        if (isShiftDown()) text.append("Shift+");
        if (isAltDown()) text.append("Alt+");
        return text.append(KeyNames.displayOf(keyCode)).toString();
    }

    @Override
    public String toString() {
        return isBound() ? toDisplayString() : "Unbound";
    }
}
