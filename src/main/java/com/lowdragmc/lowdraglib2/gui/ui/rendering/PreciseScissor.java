package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.ui.data.Clip;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

/**
 * Clip rectangles kept at the resolution the render target actually has.
 *
 * <p>A scissor box is an integer rectangle in <em>physical</em> pixels. Vanilla arrives at one by
 * rounding three times: once in gui space when the rectangle is pushed, again when the pose is
 * applied ({@code ScreenRectangle#transformAxisAligned} floors the origin <em>and</em> the extent),
 * and a third time when it is multiplied by the gui scale on the way to the render pass. The first
 * two alone throw away everything below a whole gui pixel, so at guiScale 3 the clip edge can only
 * land on every third physical pixel the target has.
 *
 * <p>Under a fractional pose — a graph view's zoom — that makes the clip edge snap to the gui pixel
 * grid while the content pans smoothly underneath, and a box narrower than one gui pixel floors to
 * nothing at all and takes the subtree it was clipping with it.
 *
 * <p>So the rectangle is carried unrounded, in gui space, and quantised exactly once, against the
 * target's real pixel grid.
 */
public final class PreciseScissor {
    private PreciseScissor() {}

    /**
     * A clip rectangle in gui space, unrounded.
     *
     * <p>Edges rather than origin+size: intersecting and quantising both work on edges, and a size
     * would have to be reconstructed — at a different rounding — for each.
     *
     * <p>Named {@code ClipRect} rather than {@code Clip} because {@link Clip} is the style enum that
     * decides whether an element clips at all, and both appear in {@link GUIContext}.
     */
    public record ClipRect(float left, float top, float right, float bottom) {
        /**
         * Clips everything away. What an element whose box fell outside its parent's pushes, so the
         * degenerate case does not need a null to travel alongside the rectangle.
         */
        public static final ClipRect EMPTY = new ClipRect(0, 0, 0, 0);

        public boolean isDegenerate() {
            return !(right > left) || !(bottom > top);
        }
    }

    /**
     * A scissor box in physical pixels, in GL's convention: origin bottom-left.
     */
    public record PixelBox(int x, int y, int width, int height) {}

    /**
     * The screen-space bounding box of a local rectangle under {@code pose}.
     *
     * <p>The unrounded twin of {@code ScreenRectangle#transformMaxBounds}, which does the same
     * four-corner reduction but floors and ceils into ints. Four corners and not two, unlike
     * {@code ScreenRectangle#transformAxisAligned}, which is what the scissor path used to go
     * through: mapping only the top-left and bottom-right turns a rotation into a negative extent,
     * which then intersects to nothing and takes the whole clipped subtree with it.
     */
    public static ClipRect transform(Matrix3x2fc pose, float x, float y, float width, float height) {
        var x1 = x + width;
        var y1 = y + height;
        var p = new Vector2f();

        pose.transformPosition(x, y, p);
        float minX = p.x, maxX = p.x, minY = p.y, maxY = p.y;

        pose.transformPosition(x1, y, p);
        minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
        minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);

        pose.transformPosition(x, y1, p);
        minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
        minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);

        pose.transformPosition(x1, y1, p);
        minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
        minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);

        return new ClipRect(minX, minY, maxX, maxY);
    }

    /**
     * Nesting. {@code null} means "no clip" on input and "clips away everything" on output.
     *
     * <p>The float counterpart of {@code ScreenRectangle#intersection}, which can only answer in
     * whole gui pixels.
     */
    public static @Nullable ClipRect intersect(@Nullable ClipRect a, @Nullable ClipRect b) {
        if (a == null) return b;
        if (b == null) return a;
        var result = new ClipRect(
                Math.max(a.left(), b.left()),
                Math.max(a.top(), b.top()),
                Math.min(a.right(), b.right()),
                Math.min(a.bottom(), b.bottom()));
        return result.isDegenerate() ? null : result;
    }

    /**
     * The one quantisation: gui space to physical pixels.
     *
     * <p>{@link Math#round} rather than a floor/ceil hull, because a scissor test is a coverage
     * test. A physical pixel is inside when its centre is, i.e. {@code ceil(edge - 0.5)}, which is
     * {@code round(edge)}. That is the same rule the rasteriser applies to the geometry itself, so a
     * clip at x=10.5 cuts exactly where a drawn edge at x=10.5 would. A hull here would leak up to a
     * pixel of unclipped content on each side; flooring both origin and extent, as vanilla does,
     * makes the box jitter by a pixel as the content pans under it.
     *
     * <p>One consequence, accepted deliberately: a clip narrower than half a physical pixel rounds
     * to width 0 and draws nothing. That is what the pixel-centre rule says — no centre is inside
     * it.
     *
     * <p>Two scales rather than one because the caller's projection is what defines the mapping, and
     * it is not always the gui scale: an off-target whose gui extent is {@code ceil(pixels /
     * guiScale)} maps gui to pixels at slightly less than {@code guiScale}.
     *
     * <p>Clamped to the target because nothing upstream is: {@code ScissorStack#push} intersects
     * only with the enclosing rectangle, never with the screen, so an off-target box can reach here.
     */
    public static PixelBox quantize(ClipRect clip, double scaleX, double scaleY,
                                    int targetWidth, int targetHeight) {
        var x0 = clamp(Math.round(clip.left() * scaleX), targetWidth);
        var x1 = clamp(Math.round(clip.right() * scaleX), targetWidth);
        var y0 = clamp(Math.round(clip.top() * scaleY), targetHeight);
        var y1 = clamp(Math.round(clip.bottom() * scaleY), targetHeight);
        return new PixelBox(x0, targetHeight - y1, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    /**
     * {@code Math.round(double)} returns a long on purpose: an absurd coordinate would otherwise
     * saturate into a negative int before it ever reached the clamp.
     */
    private static int clamp(long v, int max) {
        return (int) Math.clamp(v, 0L, max);
    }
}
