package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap;

import java.util.List;

/**
 * How far the drag has to be nudged for it to line up, plus the guides worth drawing.
 *
 * @param offsetX correction to add to the dragged rectangle's x
 * @param offsetY correction to add to its y
 * @param guides  one per axis at most, and only for element alignment — a grid snap is not
 *                something the user needs a line drawn through, and drawing one every time
 *                anything moves would make the canvas flash constantly
 */
public record SnapResult(float offsetX, float offsetY, List<SnapGuide> guides) {

    public static final SnapResult NONE = new SnapResult(0f, 0f, List.of());

    public boolean isIdentity() {
        return offsetX == 0f && offsetY == 0f && guides.isEmpty();
    }
}
