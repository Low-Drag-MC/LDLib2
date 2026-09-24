package com.lowdragmc.lowdraglib2.editor.keymap;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLKeycode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Resolution is the part that has to be right: which action a chord reaches when several want it, and
 * what happens to a binding the user changed.
 */
class KeymapTest {
    private static final Identifier SAVE = Identifier.fromNamespaceAndPath("ldlib2", "editor.save");
    private static final Identifier DELETE_NODE = Identifier.fromNamespaceAndPath("ldlib2", "graph.delete");
    private static final Identifier DELETE_FILE = Identifier.fromNamespaceAndPath("ldlib2", "browser.delete");

    /** Stands in for the editor: the questions a context can ask, answered by the test. */
    private record TestContext(KeyChord chord, Set<Class<?>> focus, boolean typing,
                               boolean project) implements KeymapContext {
        static TestContext of(KeyChord chord) {
            return new TestContext(chord, Set.of(), false, true);
        }

        TestContext focusedOn(Class<?> type) {
            return new TestContext(chord, Set.of(type), typing, project);
        }

        TestContext typing(boolean typing) {
            return new TestContext(chord, focus, typing, project);
        }

        TestContext withProject(boolean project) {
            return new TestContext(chord, focus, typing, project);
        }

        @Override
        public boolean isFocusWithin(Class<?> elementType) {
            return focus.contains(elementType);
        }

        @Override
        public boolean isTextInputFocused() {
            return typing;
        }

        @Override
        public boolean hasProject() {
            return project;
        }
    }

    private static class GraphPanel {}

    private static class BrowserPanel {}

    private static EditorAction action(Identifier id, KeyChord chord, KeyContext when, List<Identifier> log) {
        return EditorAction.builder(id).defaultChord(chord).when(when).onAction(() -> log.add(id)).build();
    }

    @Test
    void aChordReachesTheActionBoundToIt() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        assertNotNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_S))));
        assertEquals(List.of(SAVE), log);
    }

    @Test
    void anUnboundChordReachesNothing() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        assertNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_D))));
        assertNull(keymap.dispatch(TestContext.of(KeyChord.UNBOUND)));
        assertTrue(log.isEmpty());
    }

    @Test
    void theMoreSpecificContextWins() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var delete = KeyChord.key(SDLKeycode.SDLK_DELETE);
        // registered first on purpose: specificity has to beat registration order, or the panel that
        // happened to load last would own every shared key
        keymap.register(action(DELETE_NODE, delete, KeyContext.focusWithin(GraphPanel.class), log));
        keymap.register(action(DELETE_FILE, delete, KeyContext.focusWithin(BrowserPanel.class), log));

        keymap.dispatch(TestContext.of(delete).focusedOn(BrowserPanel.class));
        keymap.dispatch(TestContext.of(delete).focusedOn(GraphPanel.class));

        assertEquals(List.of(DELETE_FILE, DELETE_NODE), log);
    }

    @Test
    void anActionOutOfContextStandsAsideForTheGlobalOne() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var delete = KeyChord.key(SDLKeycode.SDLK_DELETE);
        keymap.register(action(DELETE_NODE, delete, KeyContext.focusWithin(GraphPanel.class), log));
        keymap.register(action(DELETE_FILE, delete, KeyContext.global(), log));

        keymap.dispatch(TestContext.of(delete));

        assertEquals(List.of(DELETE_FILE), log);
    }

    @Test
    void aHandlerThatDeclinesPassesTheChordOn() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var delete = KeyChord.key(SDLKeycode.SDLK_DELETE);
        // "nothing is selected here" — the panel is focused but has nothing to delete
        keymap.register(EditorAction.builder(DELETE_NODE).defaultChord(delete)
                .when(KeyContext.focusWithin(GraphPanel.class))
                .onAction(context -> false).build());
        keymap.register(action(DELETE_FILE, delete, KeyContext.global(), log));

        var ran = keymap.dispatch(TestContext.of(delete).focusedOn(GraphPanel.class));

        assertEquals(DELETE_FILE, ran.id());
        assertEquals(List.of(DELETE_FILE), log);
    }

    @Test
    void priorityThenRegistrationOrderBreakTheTie() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var chord = KeyChord.ctrl(SDLKeycode.SDLK_S);
        keymap.register(action(SAVE, chord, KeyContext.global(), log));
        // same context, registered later: a project's action takes over from the editor's default
        keymap.register(action(DELETE_FILE, chord, KeyContext.global(), log));
        assertEquals(DELETE_FILE, keymap.dispatch(TestContext.of(chord)).id());

        // …unless the earlier one asked for priority
        var keymapWithPriority = new Keymap();
        keymapWithPriority.register(EditorAction.builder(SAVE).defaultChord(chord).priority(10)
                .onAction(() -> log.add(SAVE)).build());
        keymapWithPriority.register(action(DELETE_FILE, chord, KeyContext.global(), log));
        assertEquals(SAVE, keymapWithPriority.dispatch(TestContext.of(chord)).id());
    }

    @Test
    void registeringTheSameIdReplacesTheEarlierAction() {
        var runs = new AtomicInteger();
        var keymap = new Keymap();
        var chord = KeyChord.ctrl(SDLKeycode.SDLK_S);
        keymap.register(EditorAction.builder(SAVE).defaultChord(chord).onAction(runs::incrementAndGet).build());
        keymap.register(EditorAction.builder(SAVE).defaultChord(chord).onAction(runs::incrementAndGet).build());

        keymap.dispatch(TestContext.of(chord));

        assertEquals(1, keymap.getActions().size());
        assertEquals(1, runs.get(), "the replaced action must not run as well");
    }

    @Test
    void unregisteringIsIgnoredOnceSomethingElseTookTheId() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var chord = KeyChord.ctrl(SDLKeycode.SDLK_S);
        var first = keymap.register(action(SAVE, chord, KeyContext.global(), log));
        keymap.register(action(SAVE, chord, KeyContext.global(), log));

        // the disposed view's handle must not take its replacement down with it
        first.unsubscribe();

        assertEquals(1, keymap.getActions().size());
        assertNotNull(keymap.dispatch(TestContext.of(chord)));
    }

    @Test
    void unregisteringRemovesTheActionAndItsBinding() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var chord = KeyChord.ctrl(SDLKeycode.SDLK_S);
        keymap.register(action(SAVE, chord, KeyContext.global(), log)).unsubscribe();

        assertNull(keymap.dispatch(TestContext.of(chord)));
        assertTrue(keymap.getActions().isEmpty());
    }

    @Test
    void rebindingMovesTheActionToTheNewChord() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        keymap.setBindings(SAVE, Keymap.Bindings.of(KeyChord.ctrl(SDLKeycode.SDLK_W), KeyChord.key(SDLKeycode.SDLK_F2)));

        assertNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_S))));
        assertNotNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_W))));
        // the second chord is a full alternative, not decoration
        assertNotNull(keymap.dispatch(TestContext.of(KeyChord.key(SDLKeycode.SDLK_F2))));
        assertEquals(2, log.size());
    }

    @Test
    void clearingABindingLeavesTheActionUnreachableButListed() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        keymap.setBindings(SAVE, Keymap.Bindings.UNBOUND);

        assertNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_S))));
        assertTrue(keymap.bindingsOf(SAVE).isUnbound());
        assertEquals(1, keymap.getActions().size());
        assertTrue(keymap.isOverridden(SAVE), "a cleared binding is an override, not a missing one");
    }

    @Test
    void rebindingBackToTheDefaultStopsBeingAnOverride() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var defaults = Keymap.Bindings.of(KeyChord.ctrl(SDLKeycode.SDLK_S), KeyChord.UNBOUND);
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        keymap.setBindings(SAVE, Keymap.Bindings.of(KeyChord.ctrl(SDLKeycode.SDLK_W), KeyChord.UNBOUND));
        keymap.setBindings(SAVE, defaults);

        // stored as "not overridden", so the action keeps following its default if that ever changes
        assertFalse(keymap.isOverridden(SAVE));
        assertTrue(keymap.getOverrides().isEmpty());
    }

    @Test
    void resetRestoresTheDefaultChord() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));
        keymap.setBindings(SAVE, Keymap.Bindings.of(KeyChord.ctrl(SDLKeycode.SDLK_W), KeyChord.UNBOUND));

        keymap.resetBindings(SAVE);

        assertEquals(KeyChord.ctrl(SDLKeycode.SDLK_S), keymap.bindingsOf(SAVE).primary());
        assertNotNull(keymap.dispatch(TestContext.of(KeyChord.ctrl(SDLKeycode.SDLK_S))));
    }

    @Test
    void overridesForUnknownActionsSurvive() {
        var other = Identifier.fromNamespaceAndPath("othermod", "editor.thing");
        var keymap = new Keymap();
        keymap.register(EditorAction.builder(SAVE).defaultChord(KeyChord.ctrl(SDLKeycode.SDLK_S))
                .onAction(() -> {}).build());

        // as loaded from a settings file another editor wrote
        keymap.setOverrides(Map.of(other, Keymap.Bindings.of(KeyChord.key(SDLKeycode.SDLK_F6), KeyChord.UNBOUND)));

        assertEquals(KeyChord.key(SDLKeycode.SDLK_F6), keymap.bindingsOf(other).primary());
        assertTrue(keymap.getOverrides().containsKey(other),
                "an id this editor has no action for must still be written back, or the other editor loses its keymap");
    }

    @Test
    void conflictsAreReportedRatherThanPrevented() {
        var log = new ArrayList<Identifier>();
        var keymap = new Keymap();
        var chord = KeyChord.key(SDLKeycode.SDLK_DELETE);
        keymap.register(action(DELETE_NODE, chord, KeyContext.focusWithin(GraphPanel.class), log));
        keymap.register(action(DELETE_FILE, chord, KeyContext.focusWithin(BrowserPanel.class), log));
        keymap.register(action(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S), KeyContext.global(), log));

        var conflicts = keymap.conflicts();

        assertEquals(1, conflicts.size());
        assertEquals(chord, conflicts.get(0).chord());
        assertEquals(2, conflicts.get(0).actions().size());
        assertEquals(List.of(DELETE_NODE), keymap.conflictsWith(DELETE_FILE, chord).stream()
                .map(EditorAction::id).toList());
        assertTrue(keymap.conflictsWith(SAVE, KeyChord.ctrl(SDLKeycode.SDLK_S)).isEmpty());
    }
}
