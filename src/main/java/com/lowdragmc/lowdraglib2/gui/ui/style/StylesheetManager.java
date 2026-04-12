package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class StylesheetManager implements SimpleSynchronousResourceReloadListener {
    public static final StylesheetManager INSTANCE = new StylesheetManager();
    public static final String PATH = "lss";

    @Override
    public ResourceLocation getFabricId() {
        return LDLib2.id("stylesheet_manager");
    }


    public static final ResourceLocation GDP = LDLib2.id(PATH + "/gdp.lss");
    public static final ResourceLocation MC = LDLib2.id(PATH + "/mc.lss");
    public static final ResourceLocation MODERN = LDLib2.id(PATH + "/modern.lss");

    private final Map<ResourceLocation, Stylesheet> builtinStylesheets = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Stylesheet> packStylesheets = new ConcurrentHashMap<>();

    /** Live StyleEngine instances (held via WeakReference for auto-cleanup). */
    private final Set<WeakReference<StyleEngine>> activeEngines =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    /** Register a StyleEngine so it receives reload notifications. */
    public void registerEngine(StyleEngine engine) {
        activeEngines.add(new WeakReference<>(engine));
    }

    /** Unregister a StyleEngine (e.g. when the UI closes). */
    public void unregisterEngine(StyleEngine engine) {
        activeEngines.removeIf(ref -> {
            var e = ref.get();
            return e == null || e == engine;
        });
    }

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager resourceManager) {
        var resources = resourceManager.listResources(PATH,
                location -> location.getPath().endsWith(".lss"));

        // Parse new stylesheets
        var newSheets = new HashMap<ResourceLocation, Stylesheet>();
        for (var entry : resources.entrySet()) {
            var key = entry.getKey();
            var res = entry.getValue();
            try (var reader = res.openAsReader()) {
                var lss = String.join("\n", reader.lines().toList());
                var stylesheet = Stylesheet.parse(lss);
                stylesheet.setName(key.getPath());
                newSheets.put(key, stylesheet);
            } catch (Exception e) {
                LDLib2.LOGGER.error("Failed to load style sheet {} of {}", res.sourcePackId(), key, e);
            }
        }

        // Update existing Stylesheet objects in-place so existing references stay valid,
        // and add/remove entries for changed keys.
        for (var entry : newSheets.entrySet()) {
            var key = entry.getKey();
            var newSheet = entry.getValue();
            var existing = packStylesheets.get(key);
            if (existing != null) {
                existing.clear();
                existing.merge(newSheet);
            } else {
                packStylesheets.put(key, newSheet);
            }
        }
        packStylesheets.keySet().removeIf(key -> !newSheets.containsKey(key));

        // Notify all live engines to re-match elements
        notifyEnginesReload();
    }

    private void notifyEnginesReload() {
        activeEngines.removeIf(ref -> {
            var engine = ref.get();
            if (engine == null) return true; // clean up dead ref
            engine.scheduleFullReload();
            return false;
        });
    }
}
