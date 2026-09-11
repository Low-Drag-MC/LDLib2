package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorAction;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.keymap.Keymap;
import com.lowdragmc.lowdraglib2.editor.keymap.ui.KeymapConfigurator;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The bindings the user changed, and the settings page that changes them.
 *
 * <p>Only <b>overrides</b> are stored. An action left alone is absent from the file and keeps following
 * its default, so a later version can move a default without overruling a user who never touched it —
 * and a fresh install carries no keymap at all.
 *
 * <p>The file is shared by every editor, while each editor only knows its own actions, so ids with no
 * action here are carried through untouched: dropping them would let opening the UI editor wipe the
 * graph editor's keymap.
 */
public class KeymapSettings implements Settings {
    public static final ResourceLocation ID = LDLib2.id("keymap");

    private static final Codec<Keymap.Bindings> BINDINGS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            KeyChord.CODEC.optionalFieldOf("primary", KeyChord.UNBOUND).forGetter(Keymap.Bindings::primary),
            KeyChord.CODEC.optionalFieldOf("secondary", KeyChord.UNBOUND).forGetter(Keymap.Bindings::secondary)
    ).apply(instance, Keymap.Bindings::new));

    public static final Codec<KeymapSettings> CODEC = Codec
            .unboundedMap(ResourceLocation.CODEC, BINDINGS_CODEC)
            .xmap(KeymapSettings::new, settings -> settings.overrides);

    private final Map<ResourceLocation, Keymap.Bindings> overrides = new LinkedHashMap<>();

    /** The editor these settings were last applied to, so the page can list its actions. */
    @Nullable
    private Editor editor;

    public KeymapSettings() {}

    public KeymapSettings(Map<ResourceLocation, Keymap.Bindings> overrides) {
        this.overrides.putAll(overrides);
    }

    /**
     * The keymap settings of an editor, or a detached instance when it has none registered — same
     * contract as {@link BehaviorSettings#of}, so callers need no null dance.
     */
    public static KeymapSettings of(Editor editor) {
        return editor.getEditorSettings().getSettings(ID)
                .filter(KeymapSettings.class::isInstance)
                .map(KeymapSettings.class::cast)
                .orElseGet(KeymapSettings::new);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getPath() {
        return "Keymap";
    }

    @Override
    public void onLoaded(Editor editor) {
        this.editor = editor;
    }

    @Override
    public void onApply(Editor editor) {
        this.editor = editor;
        editor.getKeymap().setOverrides(overrides);
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        father.addConfigurator(new KeymapConfigurator(this));
    }

    /** The actions the page lists: those of the editor this was applied to. */
    public Collection<EditorAction> getActions() {
        return editor == null ? List.of() : editor.getKeymap().getActions();
    }

    /** What this action answers to as the page currently stands, pending changes included. */
    public Keymap.Bindings bindingsOf(EditorAction action) {
        var override = overrides.get(action.id());
        return override != null ? override : defaultsOf(action);
    }

    public Keymap.Bindings defaultsOf(EditorAction action) {
        return new Keymap.Bindings(action.defaultPrimary(), action.defaultSecondary());
    }

    public boolean isOverridden(EditorAction action) {
        return overrides.containsKey(action.id());
    }

    /** Rebinds an action. Back to its defaults means "no override", not "an override that matches". */
    public void setBindings(EditorAction action, Keymap.Bindings bindings) {
        if (bindings.equals(defaultsOf(action))) {
            overrides.remove(action.id());
        } else {
            overrides.put(action.id(), bindings);
        }
        markDirty();
    }

    public void reset(EditorAction action) {
        overrides.remove(action.id());
        markDirty();
    }

    /**
     * Binds a chord only if the action has no binding of the user's own — the seam legacy settings are
     * migrated through, where "the user never said otherwise" is exactly the condition.
     */
    public void bindIfUnset(ResourceLocation id, KeyChord chord) {
        if (overrides.containsKey(id)) return;
        overrides.put(id, Keymap.Bindings.of(chord, KeyChord.UNBOUND));
        markDirty();
    }

    /** Actions other than this one that already answer to the chord, as the page currently stands. */
    public List<EditorAction> conflictsWith(EditorAction action, KeyChord chord) {
        return Keymap.conflictsWith(getActions(), this::bindingsOf, action.id(), chord);
    }

    private void markDirty() {
        if (editor != null) {
            editor.getEditorSettings().markDirty();
        }
    }
}
