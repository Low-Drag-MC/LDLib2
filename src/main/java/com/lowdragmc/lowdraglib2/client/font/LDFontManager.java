package com.lowdragmc.lowdraglib2.client.font;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.client.font.glyph.BitmapSdfSource;
import com.lowdragmc.lowdraglib2.client.font.glyph.GlyphSource;
import com.lowdragmc.lowdraglib2.client.font.glyph.SpaceSource;
import com.lowdragmc.lowdraglib2.client.font.glyph.TrueTypeSdfSource;
import com.lowdragmc.lowdraglib2.client.font.glyph.UnihexSdfSource;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.mojang.blaze3d.font.SpaceProvider;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.GlyphProviderType;
import net.minecraft.client.gui.font.providers.ProviderReferenceDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Owns LDLib's smooth font pipeline: it parses the same {@code assets/<namespace>/font/<name>.json} definitions
 * vanilla uses, turns every provider into an SDF {@link GlyphSource}, and hands out {@link LDFontSet}s through
 * a single {@link Font} instance that behaves exactly like {@code Minecraft#font}.
 * <p>
 * Two things differ from vanilla on purpose:
 * <ul>
 *     <li>the unifont provider is appended to every font's fallback chain, so a missing glyph falls back to
 *     the Minecraft unicode font instead of showing a box;</li>
 *     <li>glyphs are baked into a few large atlas pages rather than many 256x256 ones, which is what collapses
 *     a screen full of text into a single draw call.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class LDFontManager implements Function<ResourceLocation, FontSet>, ResourceManagerReloadListener {
    public static final LDFontManager INSTANCE = new LDFontManager();

    private static final FileToIdConverter FONT_DEFINITIONS = FileToIdConverter.json("font");
    /**
     * The vanilla font holding the unicode fallback, appended to the end of every chain.
     */
    public static final ResourceLocation UNIFONT = ResourceLocation.withDefaultNamespace("include/unifont");
    private static final ResourceLocation ATLAS_NAME = LDLib2.id("font/sdf");
    private static final ResourceLocation RASTER_ATLAS_NAME = LDLib2.id("font/raster");

    /**
     * One provider entry of a font definition, keeping the raw json around because the unihex definition hides
     * its fields behind private members.
     */
    private record ProviderEntry(GlyphProviderDefinition definition, JsonObject raw, FontOption.Filter filter) {
    }

    private final Map<ResourceLocation, List<ProviderEntry>> definitions = new HashMap<>();
    private final Map<ResourceLocation, LDFontSet> fontSets = new HashMap<>();
    private final Map<String, GlyphSource> sourceCache = new HashMap<>();

    @Nullable
    private LDGlyphAtlas atlas;
    private final Map<Integer, LDGlyphAtlas> rasterAtlases = new HashMap<>();
    @Nullable
    private Font font;
    /**
     * The vanilla font options the current fallback chains were built with.
     * <p>
     * These are polled rather than reacted to because vanilla offers nothing to react to: {@code
     * forceUnicodeFont} and {@code japaneseGlyphVariants} notify vanilla's own font manager through
     * {@code Minecraft#updateFontOptions}, and there is no event for mods. Mixing into that method was the
     * alternative, and is not worth the intrusion for two booleans that change once in a blue moon.
     */
    private boolean builtWithUniform;
    private boolean builtWithJapaneseVariants;

    private LDFontManager() {
    }

    /**
     * The drop in replacement for {@code Minecraft#font}.
     */
    public Font font() {
        if (font == null) {
            font = new Font(this, false);
        }
        return font;
    }

    /**
     * The atlas a glyph belongs in.
     * <p>
     * The distance field atlas is shared by everything. Rasterized glyphs get one atlas per size, because a
     * size is the unit that gets evicted: when nobody has drawn at that size for a while the whole atlas goes,
     * which is what stops zooming through many sizes from growing without bound.
     *
     * @param bucket {@link GlyphBucket#SDF} or a device pixel line height
     */
    public LDGlyphAtlas atlas(int bucket) {
        if (bucket == GlyphBucket.SDF) {
            if (atlas == null) {
                atlas = new LDGlyphAtlas(ATLAS_NAME, LDLibClientConfig.atlasSize(), true,
                        LDLibRenderTypes::sdfTextGlyphs);
            }
            return atlas;
        }
        var existing = rasterAtlases.get(bucket);
        if (existing != null) {
            return existing;
        }
        // Always nearest neighbour. The whole point of rasterizing at the drawn size is that one texel is one
        // device pixel; filtering it would hand back the softness this path exists to avoid.
        var pageSize = rasterPageSize(bucket);
        var created = new LDGlyphAtlas(RASTER_ATLAS_NAME.withSuffix("/" + bucket), pageSize, false,
                LDLibRenderTypes::rasterTextGlyphs);
        rasterAtlases.put(bucket, created);
        return created;
    }

    /**
     * A page big enough to hold a useful number of glyphs at this size without reserving a megabyte for text
     * that is nine pixels tall.
     */
    private static int rasterPageSize(int bucket) {
        if (bucket <= 16) return 256;
        if (bucket <= 32) return 512;
        return Math.min(1024, LDLibClientConfig.atlasSize());
    }

    /**
     * Frees rasterized sizes nobody has drawn with recently. Called once per client tick, between frames,
     * because dropping an atlas deletes its texture.
     */
    public void evictStaleRasterSizes() {
        if (GlyphBucket.knownBuckets().isEmpty()) {
            return;
        }
        var cutoff = System.currentTimeMillis() - LDLibClientConfig.rasterEvictSeconds() * 1000L;
        var stale = new ArrayList<Integer>();
        // walks the known sizes rather than the atlases, because a size that was selected but never baked
        // anything (nothing drawn, or every source fell back to the distance field) has no atlas to find it
        // by, and would otherwise stay 'known' for the rest of the session and keep bypassing the rate limiter
        for (var bucket : GlyphBucket.knownBuckets()) {
            if (GlyphBucket.lastUsed(bucket) < cutoff) {
                stale.add(bucket);
            }
        }
        for (var bucket : stale) {
            var evicted = rasterAtlases.remove(bucket);
            if (evicted != null) {
                evicted.close();
            }
            GlyphBucket.forget(bucket);
            LDTextLayoutCache.clearBucket(bucket);
            fontSets.values().forEach(fontSet -> fontSet.dropBucket(bucket));
        }
    }

    /**
     * Rebuilds the fallback chains when a font related video setting changes, the way vanilla's
     * {@code FontManager#updateOptions} does. Called once per client tick, which is also when it is safe to
     * throw an atlas away: doing it mid frame would leave already emitted glyphs pointing at a deleted texture.
     */
    public void refreshVanillaFontOptions() {
        var options = Minecraft.getInstance().options;
        var uniform = options.forceUnicodeFont().get();
        var japaneseVariants = options.japaneseGlyphVariants().get();
        if (uniform == builtWithUniform && japaneseVariants == builtWithJapaneseVariants) {
            return;
        }
        builtWithUniform = uniform;
        builtWithJapaneseVariants = japaneseVariants;
        invalidate();
    }

    public int rasterSizeCount() {
        return rasterAtlases.size();
    }

    /**
     * @return bytes of glyph atlas memory currently held, so the cost of keeping several sizes around is
     * visible rather than guessed at
     */
    public long atlasBytes(boolean raster) {
        if (raster) {
            return rasterAtlases.values().stream().mapToLong(LDGlyphAtlas::byteSize).sum();
        }
        return atlas == null ? 0L : atlas.byteSize();
    }

    @Override
    public FontSet apply(ResourceLocation fontId) {
        return fontSets.computeIfAbsent(fontId, this::buildFontSet);
    }

    public int atlasPageCount(boolean raster) {
        if (raster) {
            return rasterAtlases.values().stream().mapToInt(LDGlyphAtlas::pageCount).sum();
        }
        return atlas == null ? 0 : atlas.pageCount();
    }

    /**
     * Throws away every baked glyph and re-reads the font definitions. Called on resource reload, and safe to
     * call when a font related option changes.
     */
    public void invalidate() {
        // layouts hold direct BakedGlyph references, which point at atlas pages that are about to go away
        LDTextLayoutCache.clear();
        LDFontStatsOverlay.INSTANCE.reset();
        LDFontSet.resetBakedGlyphCount();
        fontSets.values().forEach(FontSet::close);
        fontSets.clear();
        // failed loads are cached as null so they are not retried every frame
        sourceCache.values().stream().filter(Objects::nonNull).forEach(GlyphSource::close);
        sourceCache.clear();
        if (atlas != null) {
            atlas.close();
            atlas = null;
        }
        rasterAtlases.values().forEach(LDGlyphAtlas::close);
        rasterAtlases.clear();
        GlyphBucket.reset();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        invalidate();
        definitions.clear();
        for (var entry : FONT_DEFINITIONS.listMatchingResourceStacks(resourceManager).entrySet()) {
            var fontId = FONT_DEFINITIONS.fileToId(entry.getKey());
            var providers = new ArrayList<ProviderEntry>();
            // resource stacks come in ascending pack priority, and the highest priority pack has to win
            var resources = entry.getValue();
            for (int i = resources.size() - 1; i >= 0; i--) {
                var resource = resources.get(i);
                try (var reader = resource.openAsReader()) {
                    var root = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var element : root.getAsJsonArray("providers")) {
                        var raw = element.getAsJsonObject();
                        var conditional = GlyphProviderDefinition.Conditional.CODEC
                                .parse(JsonOps.INSTANCE, raw)
                                .getOrThrow();
                        providers.add(new ProviderEntry(conditional.definition(), raw, conditional.filter()));
                    }
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("Unable to load font '{}' from resource pack '{}'",
                            fontId, resource.sourcePackId(), e);
                }
            }
            definitions.put(fontId, providers);
        }
    }

    private LDFontSet buildFontSet(ResourceLocation fontId) {
        var options = activeFontOptions();
        builtWithUniform = options.contains(FontOption.UNIFORM);
        builtWithJapaneseVariants = options.contains(FontOption.JAPANESE_VARIANTS);
        var sources = new ArrayList<GlyphSource>();
        collect(fontId, options, sources, new HashSet<>());
        // requirement: never show a tofu box when the unicode font could render the codepoint
        collect(UNIFONT, options, sources, new HashSet<>());
        if (sources.isEmpty()) {
            LDLib2.LOGGER.warn("Font {} resolved to no glyph sources", fontId);
        }
        return new LDFontSet(this, fontId, sources);
    }

    private void collect(ResourceLocation fontId, Set<FontOption> options, List<GlyphSource> out,
                         Set<ResourceLocation> visiting) {
        if (!visiting.add(fontId)) {
            LDLib2.LOGGER.warn("Cyclic font reference at {}", fontId);
            return;
        }
        var providers = definitions.get(fontId);
        if (providers == null) {
            return;
        }
        for (var entry : providers) {
            if (!entry.filter().apply(options)) {
                continue;
            }
            if (entry.definition() instanceof ProviderReferenceDefinition reference) {
                collect(reference.id(), options, out, visiting);
                continue;
            }
            var source = sourceFor(entry);
            if (source != null && !out.contains(source)) {
                out.add(source);
            }
        }
        visiting.remove(fontId);
    }

    /**
     * Builds (or reuses) the glyph source for a provider. Sources are shared between fonts, which matters most
     * for unifont: it is in every chain but is only parsed and held in memory once.
     */
    @Nullable
    private GlyphSource sourceFor(ProviderEntry entry) {
        var resourceManager = Minecraft.getInstance().getResourceManager();
        var definition = entry.definition();
        String key;
        if (definition instanceof TrueTypeGlyphProviderDefinition ttf) {
            key = "ttf|" + ttf.location() + "|" + ttf.size() + "|" + ttf.shift().x() + "|" + ttf.shift().y()
                    + "|" + ttf.skip() + "|" + LDLibClientConfig.emSize();
        } else if (definition instanceof BitmapProvider.Definition bitmap) {
            key = "bitmap|" + bitmap.file() + "|" + bitmap.height() + "|" + bitmap.ascent();
        } else if (definition instanceof SpaceProvider.Definition space) {
            key = "space|" + space.advances().hashCode();
        } else if (definition.type() == GlyphProviderType.UNIHEX) {
            key = "unihex|" + entry.raw().get("hex_file").getAsString();
        } else {
            LDLib2.LOGGER.warn("Unsupported glyph provider type {}", definition.type());
            return null;
        }

        if (sourceCache.containsKey(key)) {
            return sourceCache.get(key);
        }
        GlyphSource source;
        if (definition instanceof TrueTypeGlyphProviderDefinition ttf) {
            source = TrueTypeSdfSource.load(resourceManager, ttf, LDLibClientConfig.emSize());
        } else if (definition instanceof BitmapProvider.Definition bitmap) {
            source = BitmapSdfSource.load(resourceManager, bitmap);
        } else if (definition instanceof SpaceProvider.Definition space) {
            source = new SpaceSource(space.advances());
        } else {
            var hexFile = ResourceLocation.parse(entry.raw().get("hex_file").getAsString());
            source = UnihexSdfSource.load(resourceManager, hexFile, UnihexSdfSource.parseOverrides(entry.raw()));
        }
        sourceCache.put(key, source);
        return source;
    }

    private static Set<FontOption> activeFontOptions() {
        var options = EnumSet.noneOf(FontOption.class);
        var mcOptions = Minecraft.getInstance().options;
        if (mcOptions.forceUnicodeFont().get()) {
            options.add(FontOption.UNIFORM);
        }
        if (mcOptions.japaneseGlyphVariants().get()) {
            options.add(FontOption.JAPANESE_VARIANTS);
        }
        return options;
    }
}
