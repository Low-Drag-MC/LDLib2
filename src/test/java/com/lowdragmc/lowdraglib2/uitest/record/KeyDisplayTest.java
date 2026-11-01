package com.lowdragmc.lowdraglib2.uitest.record;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyDisplayTest {

    private static final long MS = 1_000_000L;

    private final KeyDisplay display = new KeyDisplay(code -> "K" + code);

    private List<String> shown(long now) {
        return display.visible(now).stream().map(KeyDisplay.Key::text).toList();
    }

    @Test
    void aChordShowsItsModifiersInCanonicalOrder() {
        display.keyDown(GLFW.GLFW_KEY_LEFT_SHIFT, 0);
        display.keyDown(GLFW.GLFW_KEY_LEFT_CONTROL, 10 * MS);
        assertEquals(List.of("Ctrl + Shift"), shown(20 * MS));
        display.keyDown(GLFW.GLFW_KEY_Z, 30 * MS);
        assertEquals(List.of("Ctrl + Shift + Z"), shown(40 * MS));
    }

    @Test
    void aRepeatedChordIsCounted() {
        display.keyDown(GLFW.GLFW_KEY_LEFT_CONTROL, 0);
        display.keyDown(GLFW.GLFW_KEY_Z, 10 * MS);
        display.keyUp(GLFW.GLFW_KEY_Z);
        display.keyDown(GLFW.GLFW_KEY_Z, 200 * MS);
        assertEquals(List.of("Ctrl + Z  ×2"), shown(300 * MS));
    }

    @Test
    void aModifierHeldThroughAClickBecomesPartOfIt() {
        display.keyDown(GLFW.GLFW_KEY_LEFT_SHIFT, 0);
        display.mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT, 50 * MS);
        assertEquals(List.of("Shift + Click"), shown(60 * MS));
    }

    @Test
    void releasedModifiersNoLongerPrefixKeys() {
        display.keyDown(GLFW.GLFW_KEY_LEFT_CONTROL, 0);
        display.keyDown(GLFW.GLFW_KEY_A, 10 * MS);
        display.keyUp(GLFW.GLFW_KEY_A);
        display.keyUp(GLFW.GLFW_KEY_LEFT_CONTROL);
        display.keyDown(GLFW.GLFW_KEY_BACKSPACE, 100 * MS);
        assertEquals(List.of("Ctrl + A", "Backspace"), shown(110 * MS));
    }

    @Test
    void typingGathersIntoOneKeyUntilAPause() {
        long t = 0;
        for (char c : "Steve".toCharArray()) display.charTyped(c, t += 80 * MS);
        assertEquals(List.of("Steve"), shown(t));
        display.charTyped('!', t + KeyDisplay.JOIN_NANOS + MS);
        assertEquals(List.of("Steve", "!"), shown(t + KeyDisplay.JOIN_NANOS + 2 * MS));
    }

    @Test
    void longTypingShowsItsTail() {
        long t = 0;
        var text = "the quick brown fox jumps over the lazy dog";
        for (char c : text.toCharArray()) display.charTyped(c, t += 50 * MS);
        var shown = shown(t).getFirst();
        assertEquals(KeyDisplay.TYPED_TAIL, shown.length());
        assertTrue(shown.startsWith("…") && text.endsWith(shown.substring(1)));
    }

    @Test
    void scrollingCountsNotches() {
        display.scroll(-1, 0);
        display.scroll(-1, 100 * MS);
        display.scroll(1, 200 * MS);
        assertEquals(List.of("Scroll ↓  ×2", "Scroll ↑"), shown(250 * MS));
    }

    @Test
    void keysFadeAndGo() {
        display.keyDown(GLFW.GLFW_KEY_ENTER, 0);
        var key = display.visible(0).getFirst();
        assertEquals(1f, key.alpha(KeyDisplay.LINGER_NANOS - 1), 1e-4);
        assertEquals(0.5f, key.alpha(KeyDisplay.LINGER_NANOS + KeyDisplay.FADE_NANOS / 2), 1e-3);
        assertEquals(List.of(), shown(KeyDisplay.LINGER_NANOS + KeyDisplay.FADE_NANOS));
    }

    @Test
    void onlyTheNewestFewKeysAreKept() {
        for (int i = 0; i < 10; i++) display.keyDown(GLFW.GLFW_KEY_F1 + i, i * 10 * MS);
        assertEquals(List.of("F7", "F8", "F9", "F10"), shown(100 * MS));
    }

    @Test
    void keysWithoutANameOfTheirOwnAskTheLayout() {
        display.keyDown(GLFW.GLFW_KEY_SEMICOLON, 0);
        assertEquals(List.of("K" + GLFW.GLFW_KEY_SEMICOLON), shown(0));
    }
}
