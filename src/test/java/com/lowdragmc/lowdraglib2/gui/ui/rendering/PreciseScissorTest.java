package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.rendering.PreciseScissor.ClipRect;
import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [PreciseScissor] is pure arithmetic on purpose, so the rounding rules that decide whether a clip
/// jitters, bleeds or vanishes can be pinned down without a GL context.
class PreciseScissorTest {

    /// What vanilla's `GuiRenderer#enableScissor` computes, for the regression comparison below.
    private static PreciseScissor.PixelBox vanilla(int left, int top, int right, int bottom,
                                                   int guiScale, int targetHeight) {
        return new PreciseScissor.PixelBox(
                left * guiScale,
                targetHeight - bottom * guiScale,
                Math.max(0, (right - left) * guiScale),
                Math.max(0, (bottom - top) * guiScale));
    }

    @Test
    void integerClipMatchesVanillaAtEveryGuiScale() {
        for (int guiScale = 1; guiScale <= 4; guiScale++) {
            var clip = new ClipRect(10, 20, 60, 45);
            assertEquals(vanilla(10, 20, 60, 45, guiScale, 120 * guiScale),
                    PreciseScissor.quantize(clip, guiScale, guiScale, 160 * guiScale, 120 * guiScale),
                    "guiScale " + guiScale);
        }
    }

    @Test
    void edgeMovesOnePhysicalPixelPerPhysicalPixelOfInput() {
        // At guiScale 4 a quarter of a gui unit is exactly one physical pixel. Vanilla cannot express
        // this at all: every one of these clips floors to the same integer rectangle.
        var previous = PreciseScissor.quantize(new ClipRect(10f, 20, 60, 45), 4, 4, 640, 480);
        for (int step = 1; step <= 3; step++) {
            var box = PreciseScissor.quantize(new ClipRect(10f + step * 0.25f, 20, 60, 45), 4, 4, 640, 480);
            assertEquals(previous.x() + 1, box.x(), "step " + step);
            assertEquals(previous.width() - 1, box.width(), "step " + step);
            previous = box;
        }
    }

    @Test
    void subGuiPixelClipSurvivesInsteadOfCollapsing() {
        // The vanish bug: 0.3 gui units floors to zero width, and ScissorStack turns a zero-area
        // rectangle into ScreenRectangle.empty(), taking the whole clipped subtree with it.
        var box = PreciseScissor.quantize(new ClipRect(10f, 20f, 10.3f, 40f), 4, 4, 640, 480);
        assertEquals(1, box.width());
    }

    @Test
    void clipNarrowerThanHalfAPhysicalPixelCoversNothing() {
        // Deliberate, and the other half of the pixel-centre rule: no pixel centre lies inside it.
        var box = PreciseScissor.quantize(new ClipRect(10f, 20f, 10.1f, 40f), 1, 1, 640, 480);
        assertEquals(0, box.width());
    }

    @Test
    void yIsFlippedAgainstTheTarget() {
        var box = PreciseScissor.quantize(new ClipRect(10, 20.5f, 60, 45.5f), 2, 2, 640, 480);
        // y is GL's bottom edge, so it flips the clip's bottom; y + height flips the clip's top.
        assertEquals(480 - Math.round(45.5 * 2), box.y());
        assertEquals(480 - Math.round(20.5 * 2), box.y() + box.height());
        assertEquals(Math.round(45.5 * 2) - Math.round(20.5 * 2), box.height());
    }

    @Test
    void boxIsClampedToTheTarget() {
        var offTarget = PreciseScissor.quantize(new ClipRect(900, 700, 950, 750), 2, 2, 640, 480);
        assertBoxIsValid(offTarget);
        assertEquals(0, offTarget.width());
        assertEquals(0, offTarget.height());

        assertBoxIsValid(PreciseScissor.quantize(new ClipRect(-50, -50, 20, 20), 2, 2, 640, 480));
        assertBoxIsValid(PreciseScissor.quantize(new ClipRect(300, 200, 400, 300), 2, 2, 640, 480));
        assertBoxIsValid(PreciseScissor.quantize(new ClipRect(-1e9f, -1e9f, 1e9f, 1e9f), 4, 4, 640, 480));
    }

    private static void assertBoxIsValid(PreciseScissor.PixelBox box) {
        assertTrue(box.x() >= 0, "x >= 0, was " + box.x());
        assertTrue(box.y() >= 0, "y >= 0, was " + box.y());
        assertTrue(box.width() >= 0, "width >= 0, was " + box.width());
        assertTrue(box.height() >= 0, "height >= 0, was " + box.height());
    }

    @Test
    void intersectNests() {
        var outer = new ClipRect(0, 0, 100, 100);
        var inner = new ClipRect(10.25f, 20.5f, 200, 60);
        assertEquals(new ClipRect(10.25f, 20.5f, 100, 60), PreciseScissor.intersect(inner, outer));
    }

    @Test
    void intersectTreatsNullAsUnclipped() {
        var clip = new ClipRect(10, 20, 60, 45);
        assertEquals(clip, PreciseScissor.intersect(clip, null));
        assertEquals(clip, PreciseScissor.intersect(null, clip));
        assertNull(PreciseScissor.intersect(null, null));
    }

    @Test
    void intersectReturnsNullWhenDisjoint() {
        assertNull(PreciseScissor.intersect(new ClipRect(0, 0, 10, 10), new ClipRect(20, 20, 30, 30)));
        // Touching edges enclose no pixel either.
        assertNull(PreciseScissor.intersect(new ClipRect(0, 0, 10, 10), new ClipRect(10, 0, 20, 10)));
    }

    @Test
    void transformIsExactUnderAScale() {
        var pose = new Matrix3x2f().translate(5, 7).scale(0.5f);
        assertEquals(new ClipRect(10, 12, 20, 22), PreciseScissor.transform(pose, 10, 10, 20, 20));
    }

    @Test
    void transformKeepsAPositiveExtentUnderRotation() {
        // transformAxisAligned maps only the top-left and bottom-right corners, so a quarter turn
        // hands it a bottom-right that is up and to the left of the top-left: a negative extent,
        // which intersects to nothing.
        var pose = new Matrix3x2f().rotate((float) (Math.PI / 2));
        var clip = PreciseScissor.transform(pose, 10, 10, 20, 30);
        assertTrue(clip.right() > clip.left(), "right > left, was " + clip);
        assertTrue(clip.bottom() > clip.top(), "bottom > top, was " + clip);
        assertEquals(30f, clip.right() - clip.left(), 1e-4);
        assertEquals(20f, clip.bottom() - clip.top(), 1e-4);
    }

    @Test
    void clipTracksAGraphViewPanContinuously() {
        // GraphView.refreshContentTransform(): translate(-(offset * scale), ...).scale(scale).
        // At scale 0.35 and guiScale 3 a one-physical-pixel pan is 1/(0.35*3) graph units, and the
        // clip must follow it every time rather than every third step.
        var scale = 0.35f;
        var guiScale = 3;
        var step = 1f / (scale * guiScale);

        int previous = 0;
        for (int i = 0; i < 6; i++) {
            var offset = 12.7f + i * step;
            var pose = new Matrix3x2f().translate(-(offset * scale), 0).scale(scale);
            var box = PreciseScissor.quantize(
                    PreciseScissor.transform(pose, 40, 10, 160, 80), guiScale, guiScale, 1280, 720);
            if (i > 0) {
                assertEquals(previous - 1, box.x(), "pan step " + i);
            }
            previous = box.x();
        }
    }
}
