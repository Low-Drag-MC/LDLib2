package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.List;

public class VirtualWireElement extends UIElement {
    @Getter @Setter
    private Vector2f from = new Vector2f();
    @Getter @Setter
    private Vector2f to = new Vector2f();
    private List<Vector2f> rawPoints = Collections.emptyList();
    private List<Vector2f> drawPoints = Collections.emptyList();

    protected void updateWire(Vector2f from, PortDirection fromDirection, Vector2f to, PortDirection toDirection) {
        var fromPoint2 = from.add(fromDirection == PortDirection.INPUT ? -15 : 15, 0, new Vector2f());
        var toPoint2 = to.add(toDirection == PortDirection.INPUT ? -15 : 15, 0, new Vector2f());
        var minX = Math.min(from.x, to.x);
        var minY = Math.min(from.y, to.y);
        var maxX = Math.max(from.x, to.x);
        var maxY = Math.max(from.y, to.y);
        minX = Math.min(minX, fromPoint2.x);
        minY = Math.min(minY, fromPoint2.y);
        maxX = Math.max(maxX, toPoint2.x);
        maxY = Math.max(maxY, toPoint2.y);
        var border = 2;
        getLayout()
                .left(minX - border)
                .top(minY - border)
                .width(maxX - minX + 2 * border)
                .height(maxY - minY + 2 * border);
        rawPoints = List.of(from, fromPoint2, toPoint2, to);
        drawPoints = WireElement.roundCorners(rawPoints, 6, 5);
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (drawPoints.isEmpty()) return;
        // couldn't be clicking state
        guiContext.pose.pushPose();
        guiContext.pose.translate(getPositionX() - getLayoutX(), getPositionY() - getLayoutY(), 0);
        DrawerHelper.drawLines(guiContext.graphics, drawPoints,
                -1,
                -1,
               0.7f);
        guiContext.pose.popPose();
    }

}
