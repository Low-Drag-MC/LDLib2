package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.TranslatableEnum;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Locale;

/**
 * Client only settings for LDLib2, everything currently under the {@code font} section.
 * <p>
 * This is the only place these are configured. Changes reach
 * {@link com.lowdragmc.lowdraglib2.client.font.LDFontManager} through {@code ModConfigEvent}, so editing the
 * file, using the config screen and calling a setter here all take effect on their own.
 */
public class LDLibClientConfig {

    /**
     * Prefix for the translation keys the configuration screen looks up. It matches the key NeoForge uses for
     * the screen title ({@code <modid>.configuration.title}), and every entry additionally gets a
     * {@code .tooltip} key. Without these the screen falls back to showing raw field names.
     */
    private static final String LANG = LDLib2.MOD_ID + ".configuration.";

    /**
     * How LDLib turns an outline into pixels. Implements {@link TranslatableEnum} so the configuration screen
     * shows the translated names rather than the raw constants.
     */
    public enum FontRenderMode implements TranslatableEnum {
        /**
         * Hand text to Minecraft's own font renderer and stay out of the way entirely. The baseline everything
         * else is compared against.
         */
        VANILLA,
        /**
         * Always sample a signed distance field. One atlas serves every size, scales and rotates smoothly, but
         * has no hinting so small text is softer than a hand tuned bitmap font, and sharp corners round off.
         */
        SDF,
        /**
         * Rasterize every glyph at the size it is actually drawn at, the way desktop UI toolkits do. Nothing is
         * approximated, so this is as sharp as the font gets, but each size needs its own atlas and a new size
         * has to be baked before it can be drawn. Only text beyond {@code fontRasterMaxSize} falls back, and
         * that is a memory guard rather than a judgement about how it looks.
         */
        RASTER,
        /**
         * Rasterize text that is sitting still on the pixel grid, and use the distance field for anything
         * scaled, rotated, skewed or animated, where it is both smoother and free of re-baking.
         */
        AUTO;

        @Override
        public Component getTranslatedName() {
            return Component.translatable(LANG + "font.fontRenderMode." + name().toLowerCase(Locale.ROOT));
        }
    }

    public static final ModConfigSpec SPEC;
    public static final LDLibClientConfig INSTANCE;

    static {
        Pair<LDLibClientConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(LDLibClientConfig::new);
        SPEC = pair.getRight();
        INSTANCE = pair.getLeft();
    }

    public final ModConfigSpec.IntValue fontAtlasSize;
    public final ModConfigSpec.IntValue sdfEmSize;
    public final ModConfigSpec.DoubleValue sdfSharpness;
    public final ModConfigSpec.DoubleValue sdfWeight;
    public final ModConfigSpec.BooleanValue textLayoutCache;
    public final ModConfigSpec.EnumValue<FontRenderMode> fontRenderMode;
    public final ModConfigSpec.IntValue fontRasterMaxSize;
    public final ModConfigSpec.IntValue fontRasterEvictSeconds;

    private LDLibClientConfig(ModConfigSpec.Builder builder) {
        builder.translation(LANG + "font").push("font");
        fontRenderMode = builder
                .comment("How LDLib UI text is rendered.",
                        "VANILLA: Minecraft's own font renderer, no LDLib involvement.",
                        "SDF: one scalable distance field atlas, smooth under any transform, softer up close.",
                        "RASTER: rasterize at the size actually drawn, sharpest, one atlas per size in use.",
                        "AUTO: raster for small text that lands on the pixel grid, SDF everywhere else.")
                .translation(LANG + "font.fontRenderMode")
                .defineEnum("fontRenderMode", FontRenderMode.AUTO);
        fontAtlasSize = builder
                .comment("Edge length in pixels of a glyph atlas page. Larger pages mean fewer draw calls.")
                .translation(LANG + "font.fontAtlasSize")
                .defineInRange("fontAtlasSize", 1024, 256, 4096);
        sdfEmSize = builder
                .comment("Pixel height glyphs are rasterized at when generating the distance field.",
                        "Higher values keep sharp corners at large font sizes at the cost of atlas space.")
                .translation(LANG + "font.sdfEmSize")
                .defineInRange("sdfEmSize", 48, 16, 128);
        sdfSharpness = builder
                .comment("Multiplier on the anti aliasing width. Above 1 is crisper, below 1 is softer.")
                .translation(LANG + "font.sdfSharpness")
                .defineInRange("sdfSharpness", 1.0, 0.1, 4.0);
        sdfWeight = builder
                .comment("Stem thickening in pixels. Raise slightly if small text looks too thin.")
                .translation(LANG + "font.sdfWeight")
                .defineInRange("sdfWeight", 0.0, -0.5, 0.5);
        fontRasterMaxSize = builder
                .comment("Largest device pixel line height the raster path will bake; above this the distance",
                        "field is used instead. A text element's line height works out to fontSize * guiScale,",
                        "so 256 covers font size 64 at every GUI scale.")
                .translation(LANG + "font.fontRasterMaxSize")
                .defineInRange("fontRasterMaxSize", 256, 16, 512);
        fontRasterEvictSeconds = builder
                .comment("Drop a rasterized size that has not been drawn for this many seconds, freeing its",
                        "atlas. Keeps zooming through many sizes from growing without bound.")
                .translation(LANG + "font.fontRasterEvictSeconds")
                .defineInRange("fontRasterEvictSeconds", 30, 1, 600);
        textLayoutCache = builder
                .comment("Reuse the glyph positions of a string between frames instead of walking the text",
                        "again every frame. Turn off if text ever renders with stale styling.")
                .translation(LANG + "font.textLayoutCache")
                .define("textLayoutCache", true);
        builder.pop();
    }

    /**
     * @return true when LDLib renders text itself rather than handing it to the vanilla renderer
     */
    public static boolean isSmoothFont() {
        // fallback to modern ui if installed
        return fontRenderMode() != FontRenderMode.VANILLA && !LDLib2.isModLoaded("modernui");
    }

    public static int atlasSize() {
        return SPEC.isLoaded() ? INSTANCE.fontAtlasSize.get() : 1024;
    }

    public static int emSize() {
        return SPEC.isLoaded() ? INSTANCE.sdfEmSize.get() : 48;
    }

    public static float sharpness() {
        return SPEC.isLoaded() ? INSTANCE.sdfSharpness.get().floatValue() : 1f;
    }

    public static float weight() {
        return SPEC.isLoaded() ? INSTANCE.sdfWeight.get().floatValue() : 0f;
    }

    public static boolean isTextLayoutCache() {
        return SPEC.isLoaded() ? INSTANCE.textLayoutCache.get() : true;
    }

    public static FontRenderMode fontRenderMode() {
        return SPEC.isLoaded() ? INSTANCE.fontRenderMode.get() : FontRenderMode.AUTO;
    }

    /**
     * Writes the mode back to the config file, which is what the development command uses. Saving the config
     * fires {@code ModConfigEvent.Reloading}, which is what rebuilds the glyph atlases.
     */
    public static void setFontRenderMode(FontRenderMode mode) {
        if (SPEC.isLoaded()) {
            // ConfigValue#set only touches the in memory value; the save is what writes the file and fires
            // ModConfigEvent.Reloading, so without it the mode neither survives a restart nor rebuilds anything
            INSTANCE.fontRenderMode.set(mode);
            SPEC.save();
        }
    }

    public static int rasterMaxSize() {
        return SPEC.isLoaded() ? INSTANCE.fontRasterMaxSize.get() : 256;
    }

    public static int rasterEvictSeconds() {
        return SPEC.isLoaded() ? INSTANCE.fontRasterEvictSeconds.get() : 30;
    }
}
