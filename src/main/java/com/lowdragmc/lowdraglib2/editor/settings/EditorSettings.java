package com.lowdragmc.lowdraglib2.editor.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Inspector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TreeList;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLLoader;
import org.appliedenergistics.yoga.YogaEdge;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class EditorSettings implements IPersistedSerializable {
    public final Editor editor;
    private final Map<ResourceLocation, Settings> settings = new LinkedHashMap<>();
    private final Map<ResourceLocation, Codec<? extends Settings>> codecs = new HashMap<>();
    @Getter @Setter @Accessors(chain = true)
    private File settingsFile = FMLLoader.getGamePath().resolve("config").resolve(LDLib2.MOD_ID).resolve("editor.json").toFile();
    // runtime
    private boolean isDirty = false;
    private JsonObject storedSettings = new JsonObject();

    public EditorSettings(Editor editor) {
        this.editor = editor;
    }

    public <T extends Settings> void registerSettings(T settings, Codec<T> codec) {
        this.settings.put(settings.getId(), settings);
        this.codecs.put(settings.getId(), codec);
    }

    public void unregisterSettings(ResourceLocation id) {
        settings.remove(id);
        codecs.remove(id);
    }

    public Optional<Settings> getSettings(ResourceLocation id) {
        return Optional.ofNullable(settings.get(id));
    }

    public UIElement createSettingsPanel() {
        var splitView = new SplitView.Horizontal();
        var inspector = new Inspector();
        var settingsTree = createSettingsTree().build();
        var treeList = new TreeList<>(settingsTree, true);

        splitView.layout(layout -> layout.flexAuto().height(200).widthPercent(100));
        inspector.layout(layout -> layout.widthPercent(100).heightPercent(100).marginLeft(2));
        treeList.layout(layout -> layout.widthPercent(100).heightPercent(100));
        treeList.setOnSelectedChanged(selected -> {
            inspector.clear();
            if (selected.size() == 1) {
                var node = selected.iterator().next();
                var settings = node.getContent();
                if (settings == null) return;
                inspector.inspect(new IConfigurable() {
                    @Override
                    public void buildConfigurator(ConfiguratorGroup father) {
                        for (Settings setting : settings) {
                            setting.buildConfigurator(father);
                        }
                    }
                });
            }
        });

        splitView.setPercentage(30);
        splitView.left(new ScrollerView().addScrollViewChild(treeList).layout(layout -> layout.heightPercent(100).widthPercent(100)));
        splitView.right(inspector);

        return splitView;
    }

    protected TreeBuilder<String, List<Settings>> createSettingsTree() {
        var treeBuilder = TreeBuilder.<String, List<Settings>>start("editor.settings");
        for (Settings settings : settings.values()) {
            var paths = Arrays.stream(settings.getPath().split("\\.")).toList();
            treeBuilder.diveBranch(paths, builder -> {
                var list = builder.peek().getContent();
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(settings);
                builder.content(list);
            });
        }
        return treeBuilder;
    }

    /**
     * Save all settings to the file.
     */
    public void saveAllSettingsToFile() {
        if (settingsFile.getParentFile() != null) {
            settingsFile.getParentFile().mkdirs();
        }
        try (var writer = new FileWriter(settingsFile)){
            var json = serializeSettings();
            writer.write(json.toString());
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to save settings file {}", settingsFile, e);
        }
    }

    /**
     * Load all settings from the file.
     */
    public void loadAllSettingsFromFile() {
        if (!settingsFile.exists()) return;
        try (var reader = new FileReader(settingsFile)){
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            deserializeSettings(json);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load settings file {}", settingsFile, e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private JsonObject serializeSettings() {
        var json = new JsonObject();
        for (var entry : settings.entrySet()) {
            var id = entry.getKey();
            var settings = entry.getValue();
            Codec codec = codecs.get(id);
            if (codec != null) {
                var data = codec.encodeStart(Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE), settings)
                        .resultOrPartial(e -> LDLib2.LOGGER.error("Failed to serialize settings for {}, Error {}", id, e));
                if (data.isPresent()) {
                    json.add(id.toString(), (JsonElement) data.get());
                }
            }
        }
        return json;
    }

    private void deserializeSettings(JsonObject json) {
        for (var entry : codecs.entrySet()) {
            var id = entry.getKey();
            var codec = entry.getValue();
            if (json.has(id.toString())) {
                var data = json.get(id.toString());
                codec.parse(Platform.getFrozenRegistry().createSerializationContext(JsonOps.INSTANCE), data)
                        .ifSuccess(settings -> this.settings.put(id, settings))
                        .ifError(e -> LDLib2.LOGGER.error("Failed to load settings for {}, Error {}", id, e));
            }
        }
    }

    public void applyCurrentSettings() {
        var currentSettings = serializeSettings();
        for (var entry : this.settings.entrySet()) {
            var id = entry.getKey().toString();
            // skip if the current settings don't have the setting or the value is the same'
            if (currentSettings.has(id) && currentSettings.get(id).equals(storedSettings.get(id))) continue;
            entry.getValue().onApply(editor);
        }
        storedSettings = currentSettings;
        isDirty = false;
    }

    public void restoreSettings() {
        deserializeSettings(storedSettings);
    }

    public boolean isDirty() {
        if (isDirty) return true;
        isDirty = !storedSettings.equals(serializeSettings());
        return isDirty;
    }

    public void markDirty() {
        isDirty = true;
    }
}
