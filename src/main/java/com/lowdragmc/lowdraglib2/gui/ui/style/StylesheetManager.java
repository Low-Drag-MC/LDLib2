package com.lowdragmc.lowdraglib2.gui.ui.style;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@KJSBindings
public final class StylesheetManager implements ResourceManagerReloadListener {
    public static final StylesheetManager INSTANCE = new StylesheetManager();
    public static final String PATH = "lss";

    public static final ResourceLocation GDP = LDLib2.id(PATH + "/gdp.lss");
    public static final ResourceLocation GDP_MERGED = LDLib2.id(PATH + "/gdp");
    public static final ResourceLocation ORE = LDLib2.id(PATH + "/ore.lss");
    public static final ResourceLocation ORE_MERGED = LDLib2.id(PATH + "/ore");
    public static final ResourceLocation MC = LDLib2.id(PATH + "/mc.lss");
    public static final ResourceLocation MC_MERGED = LDLib2.id(PATH + "/mc");
    public static final ResourceLocation MODERN = LDLib2.id(PATH + "/modern.lss");
    public static final ResourceLocation MODERN_MERGED = LDLib2.id(PATH + "/modern");

    private final Map<ResourceLocation, Stylesheet> builtinStylesheets = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Stylesheet> packStylesheets = new ConcurrentHashMap<>();
    private final Map<String, Stylesheet> mergedStylesheets = new ConcurrentHashMap<>();

    /** Live StyleEngine instances (held via WeakReference for auto-cleanup). */
    private final Set<WeakReference<StyleEngine>> activeEngines =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private StylesheetManager() {}

    /**
     * Registers a built-in stylesheet shipped by code.
     * <p>
     * Built-in stylesheets are used as the fallback for exact {@code .lss} lookups, and are also
     * included before pack stylesheets when building the merged stylesheet for the same path.
     */
    public void registerBuiltinStylesheet(ResourceLocation location, Stylesheet sheet) {
        builtinStylesheets.put(location, sheet);
        rebuildMergedStylesheets();
    }

    /**
     * Removes a previously registered built-in stylesheet and rebuilds any merged stylesheet that
     * may have included it.
     */
    public void unregisterBuiltinStylesheet(ResourceLocation location) {
        builtinStylesheets.remove(location);
        rebuildMergedStylesheets();
    }

    public Collection<ResourceLocation> getAllBuiltinStylesheets() {
        return builtinStylesheets.keySet();
    }

    /**
     * Returns all stylesheets loaded from resource packs, keyed by their exact resource location.
     * Built-in and merged stylesheet names are not included.
     */
    public Collection<ResourceLocation> getAllPackStylesheets() {
        return packStylesheets.keySet();
    }

    /**
     * Looks up a stylesheet by resource location.
     * <p>
     * When the path ends with {@code .lss}, this performs the legacy exact lookup: resource-pack
     * stylesheet first, then built-in stylesheet fallback. This is useful when callers want one
     * concrete file such as {@code moda:lss/mc.lss}.
     * <p>
     * When the path does not end with {@code .lss}, the path is treated as a merged stylesheet name.
     * The returned stylesheet contains all built-in and pack stylesheets with the same path after
     * appending {@code .lss}; for example {@code ldlib2:lss/mc} resolves the merged result for
     * {@code lss/mc.lss}.
     */
    @Nullable
    public Stylesheet getStylesheet(ResourceLocation location) {
        var path = location.getPath();
        if (!path.endsWith(".lss")) {
            return getMergedStylesheets(path + ".lss");
        }
        var result = packStylesheets.get(location);
        if (result == null) {
            result = builtinStylesheets.get(location);
        }
        return result;
    }

    /** Returns {@link #getStylesheet(ResourceLocation)} or {@code fallback} when no stylesheet exists. */
    public Stylesheet getStylesheetOrElse(ResourceLocation location, Stylesheet fallback) {
        return Optional.ofNullable(getStylesheet(location)).orElse(fallback);
    }

    /** Returns {@link #getStylesheet(ResourceLocation)} or {@link Stylesheet#EMPTY} when no stylesheet exists. */
    public Stylesheet getStylesheetSafe(ResourceLocation location) {
        return getStylesheetOrElse(location, Stylesheet.EMPTY);
    }

    /**
     * Returns the merged stylesheet for an exact stylesheet path.
     * <p>
     * The {@code name} parameter should include the {@code .lss} suffix, for example
     * {@code lss/mc.lss}. The namespace is intentionally ignored: every built-in or pack stylesheet
     * with the same path contributes to the merged result. Built-in stylesheets are merged first,
     * followed by pack stylesheets in stable resource-location order.
     */
    @Nullable
    public Stylesheet getMergedStylesheets(String name) {
        var result = mergedStylesheets.get(name);
        if (result == null) {
            rebuildMergedStylesheets();
            result = mergedStylesheets.get(name);
        }
        return result;
    }

    /** Returns {@link #getMergedStylesheets(String)} or {@code fallback} when no merged stylesheet exists. */
    public Stylesheet getMergedStylesheetsOrElse(String name, Stylesheet fallback) {
        return Optional.ofNullable(getMergedStylesheets(name)).orElse(fallback);
    }

    /** Returns {@link #getMergedStylesheets(String)} or {@link Stylesheet#EMPTY} when no merged stylesheet exists. */
    public Stylesheet getMergedStylesheetsSafe(String name) {
        return getMergedStylesheetsOrElse(name, Stylesheet.EMPTY);
    }

    /**
     * Checks whether {@link #getStylesheet(ResourceLocation)} can resolve the given location.
     * Exact {@code .lss} paths check pack then built-in stylesheets; extensionless paths check the
     * merged stylesheet for the same path with {@code .lss} appended.
     */
    public boolean hasStylesheet(ResourceLocation location) {
        return getStylesheet(location) != null;
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

    /**
     * Reloads all pack-provided {@code lss/*.lss} resources.
     * <p>
     * Existing exact pack stylesheet objects are updated in place when possible, then the merged
     * path index is rebuilt from both built-in and pack stylesheets. Live style engines are notified
     * after the indexes are ready.
     */
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
        rebuildMergedStylesheets();

        // Notify all live engines to re-match elements
        notifyEnginesReload();
    }

    private void rebuildMergedStylesheets() {
        var newSheets = new HashMap<String, Stylesheet>();
        mergeStylesheetsByPath(newSheets, builtinStylesheets);
        mergeStylesheetsByPath(newSheets, packStylesheets);

        for (var entry : newSheets.entrySet()) {
            var key = entry.getKey();
            var newSheet = entry.getValue();
            var existing = mergedStylesheets.get(key);
            if (existing != null) {
                existing.clear();
                existing.merge(newSheet);
                existing.setName(newSheet.getName());
            } else {
                mergedStylesheets.put(key, newSheet);
            }
        }
        mergedStylesheets.keySet().removeIf(key -> !newSheets.containsKey(key));
    }

    private void mergeStylesheetsByPath(Map<String, Stylesheet> merged, Map<ResourceLocation, Stylesheet> source) {
        source.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    var path = entry.getKey().getPath();
                    var stylesheet = merged.computeIfAbsent(path, key -> {
                        var sheet = new Stylesheet(new ArrayList<>());
                        sheet.setName(key);
                        return sheet;
                    });
                    stylesheet.merge(entry.getValue());
                });
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
