package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.GraphViewLod;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.WireElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The thresholds that decide whether graph content draws in full, as flat silhouettes, or as plain
 * blocks of colour. Everything downstream of {@link GraphView#getLod()} is draw-path only and needs a
 * client, but the decision itself is pure and worth pinning: an off-by-one in the comparisons would
 * either disable LOD entirely or strip detail that was still perfectly readable.
 *
 * <p>The unit is <b>physical screen pixels per UI unit</b>, not canvas zoom. There is no GUI scale on
 * a game-test server, so {@link GraphView#getPixelScale()} degrades to the raw zoom here and the two
 * happen to coincide; the pixel-scale cases below go through {@link GraphView#resolveLod} directly so
 * they stay meaningful either way.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class GraphViewLodTest {

    private static final float SIMPLIFIED = 0.65f;
    private static final float BLOCK = 0.25f;

    private static GraphView viewAt(float scale) {
        var view = new GraphView();
        view.setScale(scale);
        return view;
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void defaultThresholdsSplitThePixelScaleRange(GameTestHelper helper) {
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
            var lod = GraphView.resolveLod(testCase.pixelScale(), true, SIMPLIFIED, BLOCK);
            if (lod != testCase.expected()) {
                helper.fail("At " + testCase.pixelScale() + " px/unit expected " + testCase.expected()
                        + " but got " + lod);
                return;
            }
        }
        helper.succeed();
    }

    /**
     * The point of keying on pixels: the same canvas zoom must give a different level depending on
     * the GUI scale, because it produces a genuinely different apparent size.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void guiScaleShiftsTheThresholds(GameTestHelper helper) {
        float zoom = 0.25f;
        var atScale2 = GraphView.resolveLod(zoom * 2f, true, SIMPLIFIED, BLOCK);
        var atScale4 = GraphView.resolveLod(zoom * 4f, true, SIMPLIFIED, BLOCK);
        if (atScale2 != GraphViewLod.SIMPLIFIED) {
            helper.fail("0.25 zoom at GUI scale 2 is 0.5 px/unit, expected SIMPLIFIED, got " + atScale2);
            return;
        }
        if (atScale4 != GraphViewLod.FULL) {
            helper.fail("0.25 zoom at GUI scale 4 is 1.0 px/unit and still readable, "
                    + "expected FULL, got " + atScale4);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void pixelScaleFoldsInTheGuiScale(GameTestHelper helper) {
        // No client on a game-test server, so the GUI scale term degrades to 1.
        var view = viewAt(0.4f);
        if (Math.abs(view.getPixelScale() - 0.4f) > 1e-5f) {
            helper.fail("Expected the pixel scale to fall back to the raw zoom, got " + view.getPixelScale());
            return;
        }
        if (view.getLod() != GraphViewLod.SIMPLIFIED) {
            helper.fail("0.4 px/unit should be SIMPLIFIED, got " + view.getLod());
            return;
        }
        helper.succeed();
    }

    /**
     * The style-driven half of the policy, exercised through {@link GraphView#resolveLod} rather
     * than {@link GraphView#getLod()}: style accessors read the value the style engine computed
     * last, and there is no style engine on a headless game-test server, so setters written here
     * would never be visible. {@code getLod()} is a thin wrapper over exactly this call.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void disablingLodForcesFullAtAnyScale(GameTestHelper helper) {
        for (var pixelScale : new float[]{0.05f, 0.24f, 0.3f, 2f}) {
            var lod = GraphView.resolveLod(pixelScale, false, SIMPLIFIED, BLOCK);
            if (lod != GraphViewLod.FULL) {
                helper.fail("lod-enabled=false must force FULL at " + pixelScale + " px/unit, got " + lod);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void customThresholdsShiftTheBands(GameTestHelper helper) {
        // Same pixel scale, three different threshold pairs - the bands must move with the style,
        // not with a value captured when the view was built.
        if (GraphView.resolveLod(1f, true, 2f, 1.5f) != GraphViewLod.BLOCK) {
            helper.fail("Below both thresholds should be BLOCK");
            return;
        }
        if (GraphView.resolveLod(1f, true, 2f, 0.5f) != GraphViewLod.SIMPLIFIED) {
            helper.fail("Between the thresholds should be SIMPLIFIED");
            return;
        }
        if (GraphView.resolveLod(1f, true, 0.5f, 0.25f) != GraphViewLod.FULL) {
            helper.fail("Above both thresholds should be FULL");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void scaleIsClampedToTheStyleRange(GameTestHelper helper) {
        var view = new GraphView();
        view.setScale(1000f);
        if (view.getScale() != view.getGraphViewStyle().maxScale()) {
            helper.fail("Expected the scale to clamp to maxScale, got " + view.getScale());
            return;
        }
        view.setScale(-5f);
        if (view.getScale() != view.getGraphViewStyle().minScale()) {
            helper.fail("Expected the scale to clamp to minScale, got " + view.getScale());
            return;
        }
        helper.succeed();
    }

    /**
     * A wire that has never been drawn has no geometry. That was already reachable through viewport
     * culling and is now reachable through {@link GraphViewLod#BLOCK}, which skips the endpoint
     * resolve entirely — so the hit-test path must report "no hit" rather than index into an empty
     * point list.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void wireHitTestsSurviveMissingGeometry(GameTestHelper helper) {
        var wire = new WireElement(new WireModel());
        try {
            // A zero-sized element at the origin overlaps this region, so the check gets past the
            // element-rect early-out and reaches the point list.
            if (wire.isOverlapping(-10f, -10f, 20f, 20f)) {
                helper.fail("A wire with no geometry must not report an overlap");
                return;
            }
            if (wire.isIntersectWithPoint(0d, 0d)) {
                helper.fail("A wire with no geometry must not report a point hit");
                return;
            }
        } catch (IndexOutOfBoundsException e) {
            helper.fail("Hit testing a wire with no geometry threw: " + e);
            return;
        }
        helper.succeed();
    }
}
