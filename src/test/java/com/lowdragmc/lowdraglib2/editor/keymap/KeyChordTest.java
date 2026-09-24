package com.lowdragmc.lowdraglib2.editor.keymap;

import org.junit.jupiter.api.Test;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.sdl.SDLKeycode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The text form of a chord is a config format — it has to survive a round trip, and it has to refuse
 * input it cannot represent rather than silently binding the wrong key.
 */
class KeyChordTest {

    @Test
    void chordsRoundTripThroughTheirTextForm() {
        var chords = new KeyChord[] {
                KeyChord.ctrl(SDLKeycode.SDLK_S),
                KeyChord.ctrlShift(SDLKeycode.SDLK_S),
                KeyChord.ctrlAlt(SDLKeycode.SDLK_S),
                KeyChord.alt(SDLKeycode.SDLK_F4),
                KeyChord.key(SDLKeycode.SDLK_DELETE),
                KeyChord.key(SDLKeycode.SDLK_KP_ENTER),
                KeyChord.of(SDLKeycode.SDLK_TAB, true, true, true),
                KeyChord.UNBOUND,
        };
        for (var chord : chords) {
            assertEquals(chord, KeyChord.parse(chord.serialize()).orElseThrow(),
                    "round trip of " + chord.serialize());
        }
    }

    @Test
    void theTextFormIsTheOneWrittenIntoTheSettingsFile() {
        assertEquals("ctrl+shift+s", KeyChord.ctrlShift(SDLKeycode.SDLK_S).serialize());
        assertEquals("ctrl+alt+s", KeyChord.ctrlAlt(SDLKeycode.SDLK_S).serialize());
        assertEquals("delete", KeyChord.key(SDLKeycode.SDLK_DELETE).serialize());
        assertEquals("kp_add", KeyChord.key(SDLKeycode.SDLK_KP_PLUS).serialize());
        // an unbound action is stored as an empty string, which is how "cleared" is told apart from
        // "never touched" (the entry is absent in that case)
        assertEquals("", KeyChord.UNBOUND.serialize());
    }

    @Test
    void modifierOrderAndCaseDoNotMatterWhenReading() {
        var expected = KeyChord.ctrlShift(SDLKeycode.SDLK_S);
        assertEquals(expected, KeyChord.parse("shift+ctrl+s").orElseThrow());
        assertEquals(expected, KeyChord.parse("Ctrl+Shift+S").orElseThrow());
        assertEquals(expected, KeyChord.parse(" ctrl + shift + s ").orElseThrow());
        // macOS writes its own modifier names, and a config shared between machines must still load
        assertEquals(expected, KeyChord.parse("cmd+shift+s").orElseThrow());
    }

    @Test
    void unreadableChordsAreRejectedRatherThanGuessed() {
        assertTrue(KeyChord.parse("ctrl+nonsense").isEmpty());
        assertTrue(KeyChord.parse("hyper+s").isEmpty());
        assertTrue(KeyChord.parse("+").isEmpty());
        assertTrue(KeyChord.parse(null).isEmpty());
    }

    @Test
    void aModifierOnItsOwnIsNotAChord() {
        assertFalse(new KeyChord(SDLKeycode.SDLK_LSHIFT, 0).isBound());
        assertFalse(new KeyChord(SDLKeycode.SDLK_LCTRL, KeyChord.MOD_CTRL).isBound());
        assertFalse(KeyChord.parse("ctrl").isPresent());
    }

    @Test
    void modifiersOutsideTheThreeAreDropped() {
        var chord = new KeyChord(SDLKeycode.SDLK_S, InputConstants.MOD_CONTROL | InputConstants.MOD_CAPS_LOCK
                | InputConstants.MOD_NUM_LOCK | InputConstants.MOD_SUPER);

        assertEquals(KeyChord.ctrl(SDLKeycode.SDLK_S), chord);
    }

    @Test
    void matchingIsExactAboutModifiers() {
        var save = KeyChord.ctrl(SDLKeycode.SDLK_S);

        assertTrue(save.matches(SDLKeycode.SDLK_S, true, false, false));
        // Ctrl+Shift+S is save-as, a different action: a loose match would fire both
        assertFalse(save.matches(SDLKeycode.SDLK_S, true, true, false));
        assertFalse(save.matches(SDLKeycode.SDLK_S, false, false, false));
        assertFalse(save.matches(SDLKeycode.SDLK_A, true, false, false));
        assertFalse(KeyChord.UNBOUND.matches(KeyNames.UNKNOWN, false, false, false));
    }

    @Test
    void displayStringsReadLikeTheKeyboard() {
        assertEquals("Ctrl+Shift+S", KeyChord.ctrlShift(SDLKeycode.SDLK_S).toDisplayString());
        assertEquals("Delete", KeyChord.key(SDLKeycode.SDLK_DELETE).toDisplayString());
        assertEquals("Num Enter", KeyChord.key(SDLKeycode.SDLK_KP_ENTER).toDisplayString());
        assertEquals("F5", KeyChord.key(SDLKeycode.SDLK_F5).toDisplayString());
        assertEquals("", KeyChord.UNBOUND.toDisplayString());
    }

    @Test
    void everyNamedKeyHasADistinctNameAndReadsBack() {
        // the table is written out by hand, so a copy-paste slip that gives two keys the same name (or
        // a name that does not read back) would otherwise surface as a shortcut silently binding another key
        // SDL keycodes are Unicode code points for keys that type something, and scancodes tagged with
        // SDLK_SCANCODE_MASK for the rest; walk the start of both ranges
        for (int i = 0; i < 0x300; i++) {
            for (var keyCode : new int[]{i, i | SDLKeycode.SDLK_SCANCODE_MASK}) {
                var name = KeyNames.nameOf(keyCode);
                if (name == null) continue;
                assertEquals(keyCode, KeyNames.codeOf(name), "name " + name + " reads back to another key");
            }
        }
    }
}
