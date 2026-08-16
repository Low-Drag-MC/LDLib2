package com.lowdragmc.lowdraglib2.gui.ui.elements;

/**
 * Level of detail for content rendered inside a {@link GraphView}.
 *
 * <p>A {@link GraphView} scaled far out draws content whose detail is sub-pixel yet still fully
 * paid for: every child element is traversed, culled, transformed and — for SDF-styled surfaces —
 * flushed and drawn in its own GL draw call. Content elements can consult
 * {@link GraphView#getLod()} and collapse themselves to flat coloured rectangles once the scale
 * makes the difference invisible.
 *
 * <p>This is purely a <em>draw path</em> concern. Never implement it by toggling
 * {@code display}/{@code isVisible} per frame — those go through the style pipeline and dirty the
 * layout tree, which costs far more than the drawing it saves.
 *
 * <p>Thresholds come from {@link GraphView.GraphViewStyle#lodSimplifiedScale()} and
 * {@link GraphView.GraphViewStyle#lodBlockScale()}, and the whole mechanism can be switched off
 * with {@link GraphView.GraphViewStyle#lodEnabled()}.
 */
public enum GraphViewLod {
    /** Draw everything. */
    FULL,
    /** Draw a recognisable silhouette — a few flat rects, straight lines, no text or icons. */
    SIMPLIFIED,
    /** Draw a single flat rect per item; omit anything that is not a solid block of colour. */
    BLOCK
}
