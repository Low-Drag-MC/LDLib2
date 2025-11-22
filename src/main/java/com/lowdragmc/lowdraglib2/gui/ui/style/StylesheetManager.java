package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StylesheetManager implements ResourceManagerReloadListener {
    public static final StylesheetManager INSTANCE = new StylesheetManager();
    public static final String PATH = "lss";
    private final Map<ResourceLocation, Stylesheet> builtinStylesheets = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Stylesheet> packStylesheets = new HashMap<>();

    private StylesheetManager() {}

    public void registerBuiltinStylesheet(ResourceLocation location, Stylesheet sheet) {
        builtinStylesheets.put(location, sheet);
    }

    public void unregisterBuiltinStylesheet(ResourceLocation location) {
        builtinStylesheets.remove(location);
    }

    @Nullable
    public Stylesheet getStylesheet(ResourceLocation location) {
        var result = packStylesheets.get(location);
        if (result == null) {
            result = builtinStylesheets.get(location);
        }
        return result;
    }

    public boolean hasStylesheet(ResourceLocation location) {
        return builtinStylesheets.containsKey(location) || packStylesheets.containsKey(location);
    }

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        var resources = resourceManager.listResources(PATH,
                location -> location.getPath().endsWith(".lss"));
        packStylesheets.clear();
        for (var entry : resources.entrySet()) {
            var key = entry.getKey();
            var res = entry.getValue();
            try (var reader = res.openAsReader()) {
                var lss = String.join("\n", reader.lines().toList());
                var stylesheet = Stylesheet.parse(lss);
                packStylesheets.put(key, stylesheet);
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to load style sheet {} of {}", res.sourcePackId(), key, e);
            }
        }
    }
}
