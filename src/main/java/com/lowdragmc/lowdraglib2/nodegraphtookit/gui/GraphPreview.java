package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class GraphPreview extends UIElement implements IGraphTool {
    private static final float PADDING = 20f;
    private static final int HIGHLIGHT_COLOR = 0xFF_4488FF;
    private static final int DEFAULT_NODE_COLOR = 0xFF_555555;
    private static final int VIEWPORT_BORDER_COLOR = 0x88_FFFFFF;

    public final GraphView graphView;

    public GraphPreview(GraphView graphView) {
        this.graphView = graphView;
        addClass("__graph-preview__");
        Style.defaultPipeline(getLayout(), l -> l.widthPercent(100).heightPercent(100));
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragUpdate);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("graph.preview");
    }

    private void onMouseDown(UIEvent event) {
        if (event.button == 0) {
            navigateTo(event.x, event.y);
            startDrag(Boolean.TRUE, null);
            event.stopPropagation();
        }
    }

    private void onDragUpdate(UIEvent event) {
        navigateTo(event.x, event.y);
    }

    private void navigateTo(float screenX, float screenY) {
        var bounds = computeBounds();
        if (bounds == null) return;

        float cx = getContentX();
        float cy = getContentY();
        float cw = getContentWidth();
        float ch = getContentHeight();
        if (cw <= 0 || ch <= 0) return;

        float boundsW = bounds.maxX - bounds.minX;
        float boundsH = bounds.maxY - bounds.minY;
        float minimapScale = Math.min(cw / boundsW, ch / boundsH);
        float ox = cx + (cw - boundsW * minimapScale) / 2f;
        float oy = cy + (ch - boundsH * minimapScale) / 2f;

        // Convert screen position to world coordinates
        float worldX = (screenX - ox) / minimapScale + bounds.minX;
        float worldY = (screenY - oy) / minimapScale + bounds.minY;

        // Center the view on this world position
        var gv = graphView.graphView;
        float vpW = gv.getContentWidth() / gv.getScale();
        float vpH = gv.getContentHeight() / gv.getScale();
        float newOffsetX = worldX - vpW / 2f;
        float newOffsetY = worldY - vpH / 2f;

        // Clamp so the viewport rect always intersects the nodes' bounding rect.
        var nodes = computeNodesBounds();
        if (nodes != null) {
            newOffsetX = Math.max(nodes.minX - vpW, Math.min(nodes.maxX, newOffsetX));
            newOffsetY = Math.max(nodes.minY - vpH, Math.min(nodes.maxY, newOffsetY));
        }

        gv.setOffsetX(newOffsetX);
        gv.setOffsetY(newOffsetY);
        // Update the content transform to reflect the new offset
        float s = gv.getScale();
        float tx = newOffsetX * s;
        float ty = newOffsetY * s;
        gv.contentRoot.transform(transform -> transform
                .translate(-tx, -ty)
                .scale(s)
        );
    }

    private Bounds computeNodesBounds() {
        var nodeLayer = graphView.getLayer(NodeElement.NODE_LAYER);
        if (nodeLayer == null || nodeLayer.getChildren().isEmpty()) return null;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean hasNodes = false;

        for (var child : nodeLayer.getChildren()) {
            if (child instanceof NodeElement nodeElement) {
                var model = nodeElement.getModel();
                float x = model.getPosition().x;
                float y = model.getPosition().y;
                float w = nodeElement.getSizeWidth();
                float h = nodeElement.getSizeHeight();

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
                hasNodes = true;
            }
        }
        return hasNodes ? new Bounds(minX, minY, maxX, maxY) : null;
    }

    @Override
    public void drawBackgroundAdditional(@NotNull GUIContext guiContext) {
        float cx = getContentX();
        float cy = getContentY();
        float cw = getContentWidth();
        float ch = getContentHeight();
        if (cw <= 0 || ch <= 0) return;

        var bounds = computeBounds();
        if (bounds == null) return;

        float boundsW = bounds.maxX - bounds.minX;
        float boundsH = bounds.maxY - bounds.minY;
        float minimapScale = Math.min(cw / boundsW, ch / boundsH);
        float ox = cx + (cw - boundsW * minimapScale) / 2f;
        float oy = cy + (ch - boundsH * minimapScale) / 2f;

        guiContext.enableScissor(cx, cy, cw, ch);

        // Draw node blocks
        var nodeLayer = graphView.getLayer(NodeElement.NODE_LAYER);
        if (nodeLayer != null) {
            for (var child : nodeLayer.getChildren()) {
                if (child instanceof NodeElement nodeElement) {
                    var model = nodeElement.getModel();
                    float nodeX = model.getPosition().x;
                    float nodeY = model.getPosition().y;
                    float nodeW = nodeElement.getSizeWidth();
                    float nodeH = nodeElement.getSizeHeight();

                    float rx = ox + (nodeX - bounds.minX) * minimapScale;
                    float ry = oy + (nodeY - bounds.minY) * minimapScale;
                    float rw = nodeW * minimapScale;
                    float rh = nodeH * minimapScale;

                    int color = getNodeColor(model);
                    DrawerHelper.drawSolidRect(guiContext.graphics, rx, ry, rw, rh, color);

                    if (graphView.isSelected(model)) {
                        DrawerHelper.drawBorder(guiContext.graphics, rx, ry, rw, rh, HIGHLIGHT_COLOR, 1);
                    }
                }
            }
        }

        // Draw viewport rectangle
        var gv = graphView.graphView;
        float vpX = gv.getOffsetX();
        float vpY = gv.getOffsetY();
        float vpW = gv.getContentWidth() / gv.getScale();
        float vpH = gv.getContentHeight() / gv.getScale();

        float vrx = ox + (vpX - bounds.minX) * minimapScale;
        float vry = oy + (vpY - bounds.minY) * minimapScale;
        float vrw = vpW * minimapScale;
        float vrh = vpH * minimapScale;

        DrawerHelper.drawBorder(guiContext.graphics, vrx, vry, vrw, vrh, VIEWPORT_BORDER_COLOR, 1);

        guiContext.disableScissor();
    }

    private int getNodeColor(AbstractNodeModel model) {
        int color;
        if (model.hasUserColor()) {
            color = model.getElementColor();
        } else {
            color = model.getDefaultColor();
        }
        // Ensure fully opaque and not transparent/zero
        if ((color & 0xFF000000) == 0) {
            color = DEFAULT_NODE_COLOR;
        } else if ((color & 0xFF000000) != 0xFF000000) {
            color = (color & 0x00FFFFFF) | 0xFF000000;
        }
        return color;
    }

    private record Bounds(float minX, float minY, float maxX, float maxY) {}

    private Bounds computeBounds() {
        var nodeLayer = graphView.getLayer(NodeElement.NODE_LAYER);
        if (nodeLayer == null || nodeLayer.getChildren().isEmpty()) return null;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean hasNodes = false;

        for (var child : nodeLayer.getChildren()) {
            if (child instanceof NodeElement nodeElement) {
                var model = nodeElement.getModel();
                float x = model.getPosition().x;
                float y = model.getPosition().y;
                float w = nodeElement.getSizeWidth();
                float h = nodeElement.getSizeHeight();

                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
                hasNodes = true;
            }
        }

        if (!hasNodes) return null;

        // Also include viewport bounds so the minimap always shows the current view
        var gv = graphView.graphView;
        float vpX = gv.getOffsetX();
        float vpY = gv.getOffsetY();
        float vpW = gv.getContentWidth() / gv.getScale();
        float vpH = gv.getContentHeight() / gv.getScale();
        minX = Math.min(minX, vpX);
        minY = Math.min(minY, vpY);
        maxX = Math.max(maxX, vpX + vpW);
        maxY = Math.max(maxY, vpY + vpH);

        return new Bounds(minX - PADDING, minY - PADDING, maxX + PADDING, maxY + PADDING);
    }
}
