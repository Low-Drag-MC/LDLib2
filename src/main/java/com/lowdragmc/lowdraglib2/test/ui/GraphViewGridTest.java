package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * The adaptive background grid, checked for the one property that matters and that a screenshot
 * cannot practically catch: <b>continuity through a level change</b>.
 *
 * <p>Zooming continuously promotes each lattice one step coarser. The positions never move, so any
 * visible pop has to come from a lattice's <em>colour</em> jumping as it changes role. That happens
 * on exactly one frame out of a zoom, which is why it was reported from watching the animation
 * rather than from any single capture — and why it is worth pinning numerically instead.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphViewGridTest {

    private static final float BASE = 64f;
    private static final float MIN_PIXELS = 14f;
    private static final int SUBDIVISIONS = 4;
    private static final int LINE_COLOR = 0x1AFFFFFF;
    private static final int ACCENT_COLOR = 0x40FFFFFF;

    /**
     * What a line at the given world spacing is actually drawn as.
     *
     * <p>Each level skips the lines a coarser level owns, so every lattice is drawn by exactly one
     * level. A lattice with no level of its own is invisible — except for ones coarser than the last
     * level, whose positions the last level draws anyway.
     */
    private static Map<Float, Integer> renderedColorsBySpacing(float scale) {
        var fine = GraphView.resolveGridFineLevel(scale, BASE, MIN_PIXELS, SUBDIVISIONS);
        var levels = GraphView.resolveGridLevels(fine.size(), fine.fade(), SUBDIVISIONS,
                LINE_COLOR, ACCENT_COLOR);
        var colors = new HashMap<Float, Integer>();
        for (var level : levels) {
            colors.put(level.cellSize(), level.color());
        }
        return colors;
    }

    private static int maxChannelDelta(int a, int b) {
        int worst = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            worst = Math.max(worst, Math.abs(((a >>> shift) & 0xFF) - ((b >>> shift) & 0xFF)));
        }
        return worst;
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void everyLatticeChangesColourSmoothlyAcrossLevelBoundaries(GameTestHelper helper) {
        // Boundaries sit where base * scale * 4^k crosses MIN_PIXELS, i.e. around 0.22 and 0.875.
        // 400 steps over the range puts several samples either side of each of them.
        final int steps = 400;
        final float from = 0.12f;
        final float to = 2.0f;
        // One step of this sweep moves the fade by at most ~1.5%, and the widest colour span a level
        // interpolates over is 0x40-0x1A = 38. Eight gives that real headroom while still being far
        // below the 0x1A -> 0x40 jump a two-level scheme produced.
        final int allowedDelta = 8;

        var previous = renderedColorsBySpacing(from);
        for (int i = 1; i <= steps; i++) {
            float scale = from + (to - from) * i / steps;
            var current = renderedColorsBySpacing(scale);

            for (var spacing : union(previous, current)) {
                int before = previous.getOrDefault(spacing, transparent(previous, spacing));
                int after = current.getOrDefault(spacing, transparent(current, spacing));
                int delta = maxChannelDelta(before, after);
                if (delta > allowedDelta) {
                    helper.fail(("Grid lattice at %.2f units jumps by %d at zoom %.4f "
                            + "(%08X -> %08X) - that is a visible pop mid-zoom")
                            .formatted(spacing, delta, scale, before, after));
                    return;
                }
            }
            previous = current;
        }
        helper.succeed();
    }

    /**
     * A spacing absent from a frame is either genuinely invisible, or coarser than the last drawn
     * level and therefore covered by it. Resolving that here keeps the continuity check honest — it
     * must not report a pop for a lattice that is still being drawn, just by a different level.
     */
    private static int transparent(Map<Float, Integer> frame, float spacing) {
        int covering = 0;
        float coveringSpacing = 0f;
        for (var entry : frame.entrySet()) {
            // A coarser lattice's positions are a subset of a finer one's.
            if (spacing % entry.getKey() < 1e-3f && entry.getKey() > coveringSpacing) {
                coveringSpacing = entry.getKey();
                covering = entry.getValue();
            }
        }
        return covering == 0 ? (LINE_COLOR & 0x00FFFFFF) : covering;
    }

    private static java.util.Set<Float> union(Map<Float, Integer> a, Map<Float, Integer> b) {
        var all = new java.util.TreeSet<>(a.keySet());
        all.addAll(b.keySet());
        return all;
    }

    /** The densest level must never be allowed to get tighter than the configured minimum. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void densestLevelStaysWithinTheSpacingBand(GameTestHelper helper) {
        for (int i = 0; i <= 200; i++) {
            float scale = 0.1f + (2.0f - 0.1f) * i / 200f;
            var fine = GraphView.resolveGridFineLevel(scale, BASE, MIN_PIXELS, SUBDIVISIONS);
            float spacing = fine.size() * scale;
            if (spacing < MIN_PIXELS - 1e-3f || spacing > MIN_PIXELS * SUBDIVISIONS + 1e-3f) {
                helper.fail("At zoom %.3f the densest grid spacing is %.2f, outside [%.0f, %.0f]"
                        .formatted(scale, spacing, MIN_PIXELS, MIN_PIXELS * SUBDIVISIONS));
                return;
            }
            float fade = fine.fade();
            if (fade < 0f || fade > 1f) {
                helper.fail("Fade out of range at zoom " + scale + ": " + fade);
                return;
            }
        }
        helper.succeed();
    }
}
