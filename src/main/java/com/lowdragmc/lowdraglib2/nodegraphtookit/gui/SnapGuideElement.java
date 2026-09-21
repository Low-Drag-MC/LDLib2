package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.snap.SnapGuide;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.List;

/**
 * Draws the alignment guides for the drag currently in progress.
 *
 * <p>Lives inside the canvas content root rather than on top of the whole view, so the guides pan
 * and zoom with the graph without any coordinate maths of their own — a guide is a line through a
 * node's edge, and it has to stay on that edge.</p>
 *
 * <p>Added last so it draws over the nodes, and re-added by {@link GraphView#setLayers} for the
 * same reason: a layer set added afterwards would otherwise cover it.</p>
 */
public class SnapGuideElement extends UIElement {

    /** Deliberately not a theme colour: a guide has to stand out against whatever the theme is. */
    @Getter @Setter
    private int guideColor = 0xFFFF3D7F;
    /**
     * Stroke thickness in screen pixels, kept constant by dividing out the canvas zoom. A hairline
     * on purpose: a guide is a measurement, not a thing on the canvas.
     */
    @Getter @Setter
    private float guideWidth = 1f;
    /** Length of the little perpendicular tick drawn at each end of a guide, in screen pixels. */
    @Getter @Setter
    private float capLength = 5f;

    private final GraphView graphView;

    public SnapGuideElement(GraphView graphView) {
        this.graphView = graphView;
        addClass("__node-graph-view_snap-guides__");
        setAllowHitTest(false);
        layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).width(0).height(0));
    }

    @Override
    protected void drawBackgroundAdditional(@NotNull IGUIContext guiContext) {
        if (!(guiContext instanceof GUIContext context)) return;
        super.drawBackgroundAdditional(context);
        var guides = graphView.getSnapGuides();
        if (guides.isEmpty()) return;

        // The element sits at the content root's origin, so its own absolute layout position is
        // exactly the offset content coordinates have to be shifted by to be drawn.
        var originX = getPositionX();
        var originY = getPositionY();
        var scale = Math.max(0.01f, graphView.graphView.getScale());
        // The line strip's stroke parameter is the HALF width — FloatLineStripRenderState names it
        // so — and the perpendicular is offset by it in both directions. Halved here so guideWidth
        // means what it says.
        var width = guideWidth / 2f / scale;
        var cap = capLength / scale;

        for (var guide : guides) {
            draw(context, line(guide, originX, originY), width);
            for (var capLine : caps(guide, originX, originY, cap)) {
                draw(context, capLine, width);
            }
        }
    }

    private void draw(GUIContext context, List<Vector2f> points, float width) {
        DrawerHelperClient.drawLines(context, points, guideColor, guideColor, width);
    }

    private static List<Vector2f> line(SnapGuide guide, float originX, float originY) {
        return guide.vertical()
                ? List.of(new Vector2f(originX + guide.position(), originY + guide.start()),
                new Vector2f(originX + guide.position(), originY + guide.end()))
                : List.of(new Vector2f(originX + guide.start(), originY + guide.position()),
                new Vector2f(originX + guide.end(), originY + guide.position()));
    }

    /**
     * A short tick across each end of the guide. Without them a vertical guide running off both
     * ends of the screen says which column things lined up in but not which elements did.
     */
    private static List<List<Vector2f>> caps(SnapGuide guide, float originX, float originY, float cap) {
        if (guide.vertical()) {
            var x = originX + guide.position();
            return List.of(
                    List.of(new Vector2f(x - cap, originY + guide.start()), new Vector2f(x + cap, originY + guide.start())),
                    List.of(new Vector2f(x - cap, originY + guide.end()), new Vector2f(x + cap, originY + guide.end())));
        }
        var y = originY + guide.position();
        return List.of(
                List.of(new Vector2f(originX + guide.start(), y - cap), new Vector2f(originX + guide.start(), y + cap)),
                List.of(new Vector2f(originX + guide.end(), y - cap), new Vector2f(originX + guide.end(), y + cap)));
    }
}
