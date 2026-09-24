package com.lowdragmc.lowdraglib2.editor.keymap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLKeycode;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shape of the keymap in the settings file. It is a user-editable config and it is shared by every
 * editor, so both halves matter: what it looks like, and what happens to entries this editor does not
 * recognise.
 *
 * <p>Mirrors the codec {@code KeymapSettings} uses — that class reaches into the editor UI and cannot be
 * loaded without a game, while the format it writes can and should be pinned down here.
 */
class KeymapPersistenceTest {
    private static final Codec<Keymap.Bindings> BINDINGS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KeyChord.CODEC.optionalFieldOf("primary", KeyChord.UNBOUND).forGetter(Keymap.Bindings::primary),
            KeyChord.CODEC.optionalFieldOf("secondary", KeyChord.UNBOUND).forGetter(Keymap.Bindings::secondary)
    ).apply(instance, Keymap.Bindings::new));

    private static final Codec<Map<Identifier, Keymap.Bindings>> CODEC =
            Codec.unboundedMap(Identifier.CODEC, BINDINGS_CODEC);

    private static final Identifier SAVE = Identifier.fromNamespaceAndPath("ldlib2", "editor.save");

    @Test
    void bindingsRoundTripThroughJson() {
        var overrides = new LinkedHashMap<Identifier, Keymap.Bindings>();
        overrides.put(SAVE, Keymap.Bindings.of(KeyChord.ctrl(SDLKeycode.SDLK_W), KeyChord.key(SDLKeycode.SDLK_F2)));

        var json = CODEC.encodeStart(JsonOps.INSTANCE, overrides).getOrThrow();
        var read = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(overrides, read);
    }

    @Test
    void theStoredShapeIsTheOneDocumented() {
        var overrides = Map.of(SAVE, Keymap.Bindings.of(KeyChord.ctrlShift(SDLKeycode.SDLK_S), KeyChord.UNBOUND));

        var json = CODEC.encodeStart(JsonOps.INSTANCE, overrides).getOrThrow().getAsJsonObject();

        var entry = json.getAsJsonObject("ldlib2:editor.save");
        assertEquals("ctrl+shift+s", entry.get("primary").getAsString());
        // an unbound chord is left out rather than written as an empty string - what says "the user
        // cleared this" is the entry existing at all, since an untouched action has no entry
        assertFalse(entry.has("secondary"));
    }

    @Test
    void aFullyClearedActionIsAnEmptyEntryRatherThanNoEntry() {
        var overrides = Map.of(SAVE, Keymap.Bindings.UNBOUND);

        var json = CODEC.encodeStart(JsonOps.INSTANCE, overrides).getOrThrow().getAsJsonObject();

        assertTrue(json.has("ldlib2:editor.save"), "dropping the entry would let the default come back");
        assertEquals(0, json.getAsJsonObject("ldlib2:editor.save").size());
    }

    @Test
    void aClearedBindingSurvivesTheRoundTrip() {
        var overrides = Map.of(SAVE, Keymap.Bindings.UNBOUND);

        var read = CODEC.parse(JsonOps.INSTANCE, CODEC.encodeStart(JsonOps.INSTANCE, overrides).getOrThrow())
                .getOrThrow();

        assertTrue(read.get(SAVE).isUnbound(),
                "a cleared binding has to come back cleared, or the default would creep back in");
    }

    @Test
    void aMissingFieldMeansUnbound() {
        var json = JsonParser.parseString("{\"ldlib2:editor.save\": {\"primary\": \"ctrl+w\"}}");

        var read = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(KeyChord.ctrl(SDLKeycode.SDLK_W), read.get(SAVE).primary());
        assertEquals(KeyChord.UNBOUND, read.get(SAVE).secondary());
    }

    @Test
    void anEditorWritesBackTheEntriesItDoesNotKnow() {
        // as another editor's keymap would appear in the shared file
        var stored = JsonParser.parseString("""
                {
                  "ldlib2:editor.save": {"primary": "ctrl+w"},
                  "othermod:editor.frobnicate": {"primary": "f6"}
                }
                """);
        var keymap = new Keymap();
        keymap.register(EditorAction.builder(SAVE).defaultChord(KeyChord.ctrl(SDLKeycode.SDLK_S))
                .onAction(() -> {}).build());

        keymap.setOverrides(CODEC.parse(JsonOps.INSTANCE, stored).getOrThrow());
        var written = CODEC.encodeStart(JsonOps.INSTANCE, keymap.getOverrides()).getOrThrow().getAsJsonObject();

        assertTrue(written.has("othermod:editor.frobnicate"),
                "opening one editor must not wipe another editor's keymap out of the shared file");
        assertEquals("f6", written.getAsJsonObject("othermod:editor.frobnicate").get("primary").getAsString());
    }

    @Test
    void anUnreadableChordFailsItsOwnEntryRatherThanTheFile() {
        var stored = JsonParser.parseString("{\"ldlib2:editor.save\": {\"primary\": \"ctrl+nonsense\"}}");

        var result = CODEC.parse(JsonOps.INSTANCE, stored);

        // the whole map is one value, so a bad chord fails it - what must not happen is a silent
        // binding to the wrong key
        assertTrue(result.isError(), "a chord that cannot be read must not decode to something else");
        assertTrue(result.error().orElseThrow().message().contains("nonsense"));
    }

    @Test
    void anEmptyKeymapWritesAnEmptyObject() {
        var json = CODEC.encodeStart(JsonOps.INSTANCE, Map.<Identifier, Keymap.Bindings>of()).getOrThrow();

        assertEquals(new JsonObject(), json);
    }
}
