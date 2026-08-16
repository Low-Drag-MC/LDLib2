package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The thresholds that decide whether graph content draws in full, as flat silhouettes, or as plain
 * blocks of colour. Everything downstream of {@link GraphView#getLod()} is draw-path only and needs a
 * client, but the decision itself is pure and worth pinning: an off-by-one in the comparisons would
 * either disable LOD entirely or strip detail that was still perfectly readable.
 *
 * <p>The unit is <b>physical screen pixels per UI unit</b>, not canvas zoom. There is no GUI scale
 * here, so {@link GraphView#getPixelScale()} degrades to the raw zoom and the two happen to
 * coincide; the pixel-scale cases below go through {@link GraphView#resolveLod} directly so they
 * stay meaningful either way.
 */
class GraphViewLodTest {

    private static final float SIMPLIFIED = 0.65f;
    private static final float BLOCK = 0.25f;

    private static GraphView viewAt(float scale) {
        var view = new GraphView();
        view.setScale(scale);
        return view;
    }

    @Test
    void defaultThresholdsSplitThePixelScaleRange() {
        record Case(float pixelScale, GraphViewLod expected) {}
        var cases = new Case[]{
                new Case(4.0f, GraphViewLod.FULL),
                new Case(1.0f, GraphViewLod.FULL),
                new Case(0.65f, GraphViewLod.FULL),        // boundary is inclusive at the top
                new Case(0.64f, GraphViewLod.SIMPLIFIED),
                new Case(0.4f, GraphViewLod.SIMPLIFIED),
                new Case(0.25f, GraphViewLod.SIMPLIFIED),  // boundary is inclusive at the top
                new Case(0.24f, GraphViewLod.BLOCK),
                new Case(0.05f, GraphViewLod.BLOCK),
        };
        for (var testCase : cases) {
            assertEquals(testCase.expected(),
                    GraphView.resolveLod(testCase.pixelScale(), true, SIMPLIFIED, BLOCK),
                    "at " + testCase.pixelScale() + " px/unit");
        }
    }

    /**
     * The point of keying on pixels: the same canvas zoom must give a different level depending on
     * the GUI scale, because it produces a genuinely different apparent size.
     */
    @Test
    void guiScaleShiftsTheThresholds() {
        float zoom = 0.25f;
        assertEquals(GraphViewLod.SIMPLIFIED, GraphView.resolveLod(zoom * 2f, true, SIMPLIFIED, BLOCK),
                "0.25 zoom at GUI scale 2 is 0.5 px/unit");
        assertEquals(GraphViewLod.FULL, GraphView.resolveLod(zoom * 4f, true, SIMPLIFIED, BLOCK),
                "0.25 zoom at GUI scale 4 is 1.0 px/unit and still readable");
    }

    @Test
    void pixelScaleFoldsInTheGuiScale() {
        // No client here, so the GUI scale term degrades to 1.
        var view = viewAt(0.4f);
        assertEquals(0.4f, view.getPixelScale(), 1e-5f, "pixel scale should fall back to the raw zoom");
        assertEquals(GraphViewLod.SIMPLIFIED, view.getLod(), "0.4 px/unit should be SIMPLIFIED");
    }

    /**
     * The style-driven half of the policy, exercised through {@link GraphView#resolveLod} rather
     * than {@link GraphView#getLod()}: style accessors read the value the style engine computed
     * last, and there is no style engine here, so setters written in this test would never be
     * visible. {@code getLod()} is a thin wrapper over exactly this call.
     */
    @Test
    void disablingLodForcesFullAtAnyScale() {
        for (var pixelScale : new float[]{0.05f, 0.24f, 0.3f, 2f}) {
            assertEquals(GraphViewLod.FULL, GraphView.resolveLod(pixelScale, false, SIMPLIFIED, BLOCK),
                    "lod-enabled=false must force FULL at " + pixelScale + " px/unit");
        }
    }

    @Test
    void customThresholdsShiftTheBands() {
        // Same pixel scale, three different threshold pairs - the bands must move with the style,
        // not with a value captured when the view was built.
        assertEquals(GraphViewLod.BLOCK, GraphView.resolveLod(1f, true, 2f, 1.5f),
                "below both thresholds should be BLOCK");
        assertEquals(GraphViewLod.SIMPLIFIED, GraphView.resolveLod(1f, true, 2f, 0.5f),
                "between the thresholds should be SIMPLIFIED");
        assertEquals(GraphViewLod.FULL, GraphView.resolveLod(1f, true, 0.5f, 0.25f),
                "above both thresholds should be FULL");
    }

    @Test
    void scaleIsClampedToTheStyleRange() {
        var view = new GraphView();
        view.setScale(1000f);
        assertEquals(view.getGraphViewStyle().maxScale(), view.getScale(), "scale should clamp to maxScale");
        view.setScale(-5f);
        assertEquals(view.getGraphViewStyle().minScale(), view.getScale(), "scale should clamp to minScale");
    }

    /**
     * A wire that has never been drawn has no geometry. That was already reachable through viewport
     * culling and is now reachable through {@link GraphViewLod#BLOCK}, which skips the endpoint
     * resolve entirely — so the hit-test path must report "no hit" rather than index into an empty
     * point list.
     */
    @Test
    void wireHitTestsSurviveMissingGeometry() {
        var wire = new WireElement(new WireModel());
        assertDoesNotThrow(() -> {
            // A zero-sized element at the origin overlaps this region, so the check gets past the
            // element-rect early-out and reaches the point list.
            assertFalse(wire.isOverlapping(-10f, -10f, 20f, 20f),
                    "a wire with no geometry must not report an overlap");
            assertFalse(wire.isIntersectWithPoint(0d, 0d),
                    "a wire with no geometry must not report a point hit");
        });
    }
}
