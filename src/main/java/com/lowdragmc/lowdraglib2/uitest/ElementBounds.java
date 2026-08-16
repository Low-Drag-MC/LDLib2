package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import org.joml.Vector2f;

/**
 * An element's rectangle in <b>GUI-scaled screen space</b> — the coordinate space
 * {@link net.minecraft.client.gui.screens.Screen#mouseClicked} speaks.
 *
 * @param x      left edge of the axis-aligned bounding box
 * @param y      top edge of the axis-aligned bounding box
 * @param width  bounding-box width; larger than the element's layout width if it is rotated
 * @param height bounding-box height
 * @param center the transformed centre — the point input steps aim at
 * @param corners the four transformed corners, in order: top-left, top-right, bottom-right, bottom-left
 */
public record ElementBounds(float x, float y, float width, float height,
                            Vector2f center, Vector2f[] corners) {

    /**
     * Maps an element's layout rectangle into screen space.
     *
     * <p>{@link UIElement#getPositionX()} and friends are <em>layout</em> space: a plain sum up the
     * parent chain, with no {@code transform2D} and no GUI pose applied. Treating them as screen
     * coordinates is wrong for anything inside a scaled or translated container — a graph canvas, a
     * scroller, a scaled panel — and that mistake shows up as clicks landing in empty space.
     *
     * <p>{@link UIElement#getWorldMouse} applies {@code getLocalToWorldPose()}, which is captured
     * during {@code drawInBackground} <em>after</em> the element's own {@code transform2D} is pushed
     * and which chains all the way up to {@code ModularUI#getLastDrawPose()}. So it is the complete
     * mapping, for the element itself as well as every ancestor.
     *
     * <p>All four corners are transformed rather than one, because a rotated or skewed transform
     * makes the axis-aligned box larger than {@code width x height}.
     *
     * <p>Only meaningful once the element has been drawn at least once — the pose cache is populated
     * during rendering. The runner guarantees this by never running a step before a frame.
     */
    public static ElementBounds of(UIElement element) {
        float px = element.getPositionX();
        float py = element.getPositionY();
        float pw = element.getSizeWidth();
        float ph = element.getSizeHeight();

        var topLeft = element.getWorldMouse(px, py);
        var topRight = element.getWorldMouse(px + pw, py);
        var bottomRight = element.getWorldMouse(px + pw, py + ph);
        var bottomLeft = element.getWorldMouse(px, py + ph);

        float minX = Math.min(Math.min(topLeft.x, topRight.x), Math.min(bottomRight.x, bottomLeft.x));
        float maxX = Math.max(Math.max(topLeft.x, topRight.x), Math.max(bottomRight.x, bottomLeft.x));
        float minY = Math.min(Math.min(topLeft.y, topRight.y), Math.min(bottomRight.y, bottomLeft.y));
        float maxY = Math.max(Math.max(topLeft.y, topRight.y), Math.max(bottomRight.y, bottomLeft.y));

        var center = element.getWorldMouse(px + pw / 2f, py + ph / 2f);
        return new ElementBounds(minX, minY, maxX - minX, maxY - minY, center,
                new Vector2f[]{topLeft, topRight, bottomRight, bottomLeft});
    }

    public float centerX() {
        return center.x;
    }

    public float centerY() {
        return center.y;
    }

    public float right() {
        return x + width;
    }

    public float bottom() {
        return y + height;
    }

    /** Whether the rectangle has any area at all. A zero-size target cannot be clicked. */
    public boolean isEmpty() {
        return width <= 0f || height <= 0f;
    }

    /** Whether the centre lies inside the given viewport. Off-screen targets fail input steps. */
    public boolean isCenterOnScreen(int screenWidth, int screenHeight) {
        return center.x >= 0 && center.y >= 0 && center.x <= screenWidth && center.y <= screenHeight;
    }

    @Override
    public String toString() {
        return "(%.1f, %.1f %.1fx%.1f)".formatted(x, y, width, height);
    }
}
