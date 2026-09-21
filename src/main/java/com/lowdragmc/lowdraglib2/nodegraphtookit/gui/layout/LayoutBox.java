package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

/**
 * One thing a layout moves — a single node, or a whole placemat carrying its contents.
 *
 * <p>{@link #x}/{@link #y} are the box's top-left corner. They come in seeded with where the element
 * currently is (algorithms that tidy rather than rebuild use that as a starting point) and are
 * overwritten with the result. The result is <em>not</em> anchored anywhere in particular: the
 * caller translates the whole set to wherever the selection should end up.</p>
 */
public final class LayoutBox {
    /** Index into the caller's own list; {@link LayoutEdge} endpoints refer to these. */
    public final int id;
    public final float width;
    public final float height;
    public float x;
    public float y;

    public LayoutBox(int id, float width, float height, float x, float y) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }

    /** Extent along the flow axis — the direction layers advance in. */
    public float flowSize(LayoutDirection direction) {
        return direction.isHorizontal() ? width : height;
    }

    /** Extent along the cross axis — the direction nodes stack within one layer. */
    public float crossSize(LayoutDirection direction) {
        return direction.isHorizontal() ? height : width;
    }

    public float flow(LayoutDirection direction) {
        return direction.isHorizontal() ? x : y;
    }

    public float cross(LayoutDirection direction) {
        return direction.isHorizontal() ? y : x;
    }

    public void setFlow(LayoutDirection direction, float value) {
        if (direction.isHorizontal()) x = value;
        else y = value;
    }

    public void setCross(LayoutDirection direction, float value) {
        if (direction.isHorizontal()) y = value;
        else x = value;
    }

    @Override
    public String toString() {
        return "LayoutBox[" + id + " @ (" + x + ", " + y + ") " + width + "x" + height + "]";
    }
}
