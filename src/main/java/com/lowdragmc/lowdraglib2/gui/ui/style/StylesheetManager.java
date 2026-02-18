package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@KJSBindings
public final class StylesheetManager implements ResourceManagerReloadListener {
    public static final StylesheetManager INSTANCE = new StylesheetManager();
    public static final String PATH = "lss";

    public static final ResourceLocation GDP = LDLib2.id(PATH + "/gdp.lss");
    public static final ResourceLocation MC = LDLib2.id(PATH + "/mc.lss");
    public static final ResourceLocation MODERN = LDLib2.id(PATH + "/modern.lss");

    private final Map<ResourceLocation, Stylesheet> builtinStylesheets = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Stylesheet> packStylesheets = new HashMap<>();

    private StylesheetManager() {}

    public void registerBuiltinStylesheet(ResourceLocation location, Stylesheet sheet) {
        builtinStylesheets.put(location, sheet);
    }

    public void unregisterBuiltinStylesheet(ResourceLocation location) {
        builtinStylesheets.remove(location);
    }

    public Collection<ResourceLocation> getAllPackStylesheets() {
        return packStylesheets.keySet();
    }

    @Nullable
    public Stylesheet getStylesheet(ResourceLocation location) {
        var result = packStylesheets.get(location);
        if (result == null) {
            result = builtinStylesheets.get(location);
        }
        return result;
    }

    public Stylesheet getStylesheetOrElse(ResourceLocation location, Stylesheet fallback) {
        return Optional.ofNullable(getStylesheet(location)).orElse(fallback);
    }

    public Stylesheet getStylesheetSafe(ResourceLocation location) {
        return getStylesheetOrElse(location, Stylesheet.EMPTY);
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
