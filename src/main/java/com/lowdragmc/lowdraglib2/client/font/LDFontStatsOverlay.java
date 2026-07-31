package com.lowdragmc.lowdraglib2.client.font;

import com.lowdragmc.lowdraglib2.client.LDLibClientConfig;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Development only readout of what the text pipeline is doing, drawn in a corner of the screen.
 * <p>
 * Built from LDLib's own elements and rendered through {@link ModularHudLayer}, so it goes through the very
 * layout and text path it is reporting on rather than a hand rolled pile of {@code drawString} calls.
 * <p>
 * Read it while cycling {@code fontRenderMode}: the same interface drawn every way gives a direct before and
 * after for draw calls and layout cost, which is the only honest way to tell whether a mode helped or just
 * looks different.
 */
public final class LDFontStatsOverlay implements ModularHudLayer {
    public static final LDFontStatsOverlay INSTANCE = new LDFontStatsOverlay();

    private static final int HEADING_COLOR = 0xFF88CCFF;
    private static final int VALUE_COLOR = 0xFFFFFFFF;
    private static final int WARN_COLOR = 0xFFFFAA55;
    /**
     * More draw batches than this means the text is costing several draw calls, which is worth a second look.
     */
    private static final int BATCH_WARN_THRESHOLD = 4;

    /**
     * One line of the readout: the label holding it, how to phrase it, and whether it deserves attention.
     */
    private record Row(Label label, Supplier<String> text, IntSupplier color) {
    }

    @Nullable
    private ModularUI ui;
    private final List<Row> rows = new ArrayList<>();
    private LDFontStats.Snapshot snapshot = LDFontStats.Snapshot.EMPTY;

    private LDFontStatsOverlay() {
    }

    public static void toggle() {
        LDFontStats.setEnabled(!LDFontStats.isEnabled());
    }

    /**
     * @return a one line summary, used by the development command to confirm the toggle
     */
    public static String describe() {
        return LDFontStats.isEnabled()
                ? "LDLib font stats overlay enabled (mode " + LDLibClientConfig.fontRenderMode() + ")"
                : "LDLib font stats overlay disabled";
    }

    /**
     * Drops the built interface. The labels hold laid out text, which holds baked glyphs, so it has to go
     * whenever the atlases do.
     */
    public void reset() {
        ui = null;
        rows.clear();
    }

    @Nullable
    @Override
    public ModularUI getModularUI() {
        if (!LDFontStats.isEnabled()) {
            return null;
        }
        if (ui == null) {
            ui = buildUI();
        }
        return ui;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!LDFontStats.isEnabled()) {
            return;
        }
        // Close the frame off before drawing and report what was just captured, so the overlay's own text is
        // never part of what it claims to measure. Suspending collection covers the rest.
        LDFontStats.endFrame(LDTextLayoutCache.size(), LDFontSet.bakedGlyphCount(), LDFontSet.bakedRasterCount(),
                LDFontManager.INSTANCE.atlasPageCount(false), LDFontManager.INSTANCE.atlasPageCount(true));
        snapshot = LDFontStats.last();
        getModularUI();
        for (var row : rows) {
            row.label().setText(row.text().get(), false);
            row.label().textStyle(style -> style.textColor(row.color().getAsInt()));
        }

        LDFontStats.suspend(true);
        try {
            ModularHudLayer.super.render(graphics, deltaTracker);
        } finally {
            LDFontStats.suspend(false);
        }
    }

    private ModularUI buildUI() {
        rows.clear();
        var panel = new UIElement();
        panel.layout(layout -> layout.paddingAll(3).gapAll(1).alignSelf(AlignItems.FLEX_START));
        panel.style(style -> style.backgroundTexture(ColorPattern.BLACK.rectTexture()));

        row(panel, HEADING_COLOR, () -> "LDLib text  [" + LDLibClientConfig.fontRenderMode() + "]");
        // vanilla expands text into glyph states inside the renderer, so there is nothing for LDLib to count
        row(panel, () -> snapshot.drawBatches() > BATCH_WARN_THRESHOLD ? WARN_COLOR : VALUE_COLOR,
                () -> "draw calls (batches)       "
                        + (LDLibClientConfig.fontRenderMode() == LDLibClientConfig.FontRenderMode.VANILLA
                        ? "n/a in vanilla mode" : String.valueOf(snapshot.drawBatches())));
        row(panel, VALUE_COLOR, () -> "text draws                 " + snapshot.textDraws());
        row(panel, VALUE_COLOR,
                () -> String.format(Locale.ROOT, "layout + emit              %.3f ms", snapshot.layoutMillis()));
        row(panel, VALUE_COLOR, () -> String.format(Locale.ROOT, "layout cache               %.1f%% of %d",
                snapshot.cacheHitRate() * 100f, snapshot.cacheHits() + snapshot.cacheMisses()));
        row(panel, VALUE_COLOR, () -> "cached layouts             " + snapshot.cachedLayouts());
        // no rasterized glyphs outside SDF mode means the raster path is silently declining every glyph
        row(panel, () -> LDLibClientConfig.fontRenderMode() != LDLibClientConfig.FontRenderMode.SDF
                        && snapshot.bakedRasterGlyphs() == 0 ? WARN_COLOR : VALUE_COLOR,
                () -> "glyphs baked               " + snapshot.bakedGlyphs()
                        + "  (raster " + snapshot.bakedRasterGlyphs()
                        + ", sdf " + (snapshot.bakedGlyphs() - snapshot.bakedRasterGlyphs()) + ")");
        row(panel, VALUE_COLOR, () -> String.format(Locale.ROOT,
                "atlas memory               sdf %.2f MB, raster %.2f MB",
                LDFontManager.INSTANCE.atlasBytes(false) / 1048576f,
                LDFontManager.INSTANCE.atlasBytes(true) / 1048576f));
        row(panel, VALUE_COLOR, () -> "raster sizes live          " + LDFontManager.INSTANCE.rasterSizeCount()
                + " " + GlyphBucket.knownBuckets());

        var root = new UIElement();
        root.layout(layout -> layout.widthPercent(100).heightPercent(100).paddingAll(4));
        root.addChild(panel);
        return ModularUI.of(UI.of(root));
    }

    private void row(UIElement panel, int color, Supplier<String> text) {
        row(panel, () -> color, text);
    }

    private void row(UIElement panel, IntSupplier color, Supplier<String> text) {
        var label = new Label();
        label.textStyle(style -> style.adaptiveWidth(true).adaptiveHeight(true).textShadow(false));
        panel.addChild(label);
        rows.add(new Row(label, text, color));
    }
}
