package com.lowdragmc.lowdraglib2.editor.settings;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class BehaviorSettings implements Settings {
    public static final ResourceLocation ID = LDLib2.id("behavior");
    public static final int DEFAULT_RECENT_PROJECT_COUNT = 10;
    public static final Codec<BehaviorSettings> CODEC =
            PersistedParser.createCodec(BehaviorSettings::new);

    /**
     * The behavior settings of an editor, or a default instance when it has none registered, so that
     * callers do not each have to spell out the lookup and a fallback for every field they read.
     */
    public static BehaviorSettings of(Editor editor) {
        return editor.getEditorSettings().getSettings(ID)
                .filter(BehaviorSettings.class::isInstance)
                .map(BehaviorSettings.class::cast)
                .orElseGet(BehaviorSettings::new);
    }

    /**
     * @deprecated replaced by the {@code ldlib2:editor.close} keymap action, which the editor binds to
     *             Escape on first load when this was on — see {@code Editor#migrateLegacySettings}.
     *             Still persisted so that migration can read it, but no longer shown as a setting of
     *             its own: "which key closes the editor" belongs in one place.
     */
    @Deprecated(since = "1.22")
    @Persisted @Getter @Setter private boolean shouldCloseOnEsc = false;
    @DefaultValue(booleanValue = true)
    @Configurable(name = "settings.ldlib2.behavior.restoreLayoutOnProjectOpen")
    @Getter @Setter private boolean restoreLayoutOnProjectOpen = true;
    @DefaultValue(booleanValue = true)
    @Configurable(name = "settings.ldlib2.behavior.restoreAssetBrowserPath")
    @Getter @Setter private boolean restoreAssetBrowserPath = true;
    @DefaultValue(numberValue = DEFAULT_RECENT_PROJECT_COUNT)
    @Configurable(name = "settings.ldlib2.behavior.recentProjectCount")
    @ConfigNumber(range = {0, 50}, type = ConfigNumber.Type.INTEGER)
    @Getter @Setter private int recentProjectCount = DEFAULT_RECENT_PROJECT_COUNT;

    // runtime
    @Nullable private Stylesheet currentStylesheet;

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public String getPath() {
        return "Behavior";
    }

    /**
     * Nothing to do: the shortcuts that used to live here — Escape, the settings panel, save and save
     * as — are {@code EditorActions} now, so they are listed in the keymap and can be rebound.
     */
    @Override
    public void onApply(Editor editor) {
    }
}
