package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.YogaOverflow;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@LDLRegister(name = "graph_view", registry = "ldlib2:ui_element")
public class GraphView extends UIElement {
    private record DragOffset(float startOffsetX, float startOffsetY) {}

    @Accessors(chain = true, fluent = true)
    public static class GraphViewStyle extends Style {
        @Getter @Setter
        private boolean allowZoom = true;
        @Getter @Setter
        private boolean allowPan = true;
        @Getter @Setter
        private float minScale = 0.1f;
        @Getter @Setter
        private float maxScale = 10f;
        @Getter @Setter @Nullable
        private SpriteTexture gridTexture = SpriteTexture.of("ldlib2:textures/gui/grid_bg.png")
                .setWrapMode(SpriteTexture.WrapMode.REPEAT);
        @Getter @Setter
        private float gridSize = 64;

        public GraphViewStyle(UIElement holder) {
            super(holder);
        }
    }

    public final UIElement contentRoot = new UIElement();
    @Getter
    private final GraphViewStyle graphViewStyle = new GraphViewStyle(this);

    // runtime
    @Getter @Setter
    private float offsetX = 0f, offsetY = 0f;  // 世界偏移
    @Getter
    private float scale = 1f;

    public GraphView() {
        setOverflow(YogaOverflow.HIDDEN);

        contentRoot.layout(l -> {
            l.setPositionType(YogaPositionType.ABSOLUTE);
            l.setWidth(0);
            l.setHeight(0);
        });
        contentRoot.getStyle().transform2D().privot(0f, 0f);

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);

        addChild(contentRoot);
        refreshContentTransform();
        markAllChildrenAsInternal();
    }

    public GraphView graphViewStyle(Consumer<GraphViewStyle> style) {
        style.accept(this.graphViewStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        graphViewStyle.applyStyles(values);
    }

    public GraphView addContentChild(UIElement child) {
        contentRoot.addChild(child);
        return this;
    }

    public GraphView removeContentChild(UIElement child) {
        contentRoot.removeChild(child);
        return this;
    }

    public GraphView clearAllContentChildren() {
        contentRoot.clearAllChildren();
        return this;
    }

    public UIElement contentRoot(Consumer<UIElement> contentRoot) {
        contentRoot.accept(this.contentRoot);
        return this;
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        refreshContentTransform();
    }

    private void refreshContentTransform() {
        contentRoot.getStyle().transform2D()
                .translate(-(offsetX * scale), -(offsetY * scale))
                .scale(scale);
    }

    public void fitToChildren(float padding, float minScaleBound) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean has = false;
        for (UIElement child : contentRoot.getChildren()) {
            if (!child.isDisplayed() || !child.isVisible()) continue;
            float x = child.getPositionX() - getContentX();
            float y = child.getPositionY() - getContentY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + child.getSizeWidth());
            maxY = Math.max(maxY, y + child.getSizeHeight());
            has = true;
        }
        if (!has) {
            offsetX = 0f; offsetY = 0f; scale = Math.max(minScaleBound, 1f);
            refreshContentTransform();
            return;
        }
        minX -= padding; minY -= padding;
        maxX += padding; maxY += padding;
        float w = Math.max(1f, maxX - minX);
        float h = Math.max(1f, maxY - minY);

        float sW = getContentWidth() / w;
        float sH = getContentHeight() / h;
        float newScale = Mth.clamp(Math.min(sW, sH), Math.max(minScaleBound, graphViewStyle.minScale), graphViewStyle.maxScale);

        offsetX = minX;
        offsetY = minY;
        scale = newScale;

        float viewWWorld = getContentWidth() / scale;
        float viewHWorld = getContentHeight() / scale;
        offsetX -= (viewWWorld - w) / 2f;
        offsetY -= (viewHWorld - h) / 2f;

        refreshContentTransform();
    }

    protected void onMouseDown(UIEvent event) {
        if (graphViewStyle.allowPan &&
                (event.target == this && event.button == 0 || event.button == 2) &&
                isChildHover() && isMouseOverContent(event.x, event.y)) {
            startDrag(new DragOffset(offsetX, offsetY), null);
        }
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragOffset(float startOffsetX, float startOffsetY)) {
            float invS = 1f / Math.max(0.0001f, Mth.clamp(scale, graphViewStyle.minScale, graphViewStyle.maxScale));
            offsetX = startOffsetX + (event.dragStartX - event.x) * invS;
            offsetY = startOffsetY + (event.dragStartY - event.y) * invS;
            refreshContentTransform();
        }
    }

    protected void onMouseWheel(UIEvent event) {
        if (graphViewStyle.allowZoom && event.target == this && isChildHover() && isMouseOverContent(event.x, event.y)) {
            var newScale = Mth.clamp(scale + event.deltaY * 0.1f, graphViewStyle.minScale, graphViewStyle.maxScale);
            if (newScale != scale) {
                var rx = event.x - this.getPositionX();
                var ry = event.y - this.getPositionY();
                offsetX += rx / scale - rx / newScale;
                offsetY += ry / scale - ry / newScale;
                scale = newScale;
            }
            refreshContentTransform();
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        super.drawBackgroundAdditional(guiContext);
        if (graphViewStyle.gridTexture == null) return;
        var x = getContentX();
        var y = getContentY();
        var w = getContentWidth();
        var h = getContentHeight();

        var imageSize = graphViewStyle.gridTexture.getImageSize();
        var gridSize = graphViewStyle.gridSize;

        guiContext.pose.pushPose();

        float worldLeft = offsetX;
        float worldTop = offsetY;
        float worldRight = offsetX + w / scale;
        float worldBottom = offsetY + h / scale;

        float gridStartX = (float) Math.floor(worldLeft / gridSize) * gridSize;
        float gridStartY = (float) Math.floor(worldTop / gridSize) * gridSize;

        float gridEndX = (float) Math.ceil(worldRight / gridSize) * gridSize;
        float gridEndY = (float) Math.ceil(worldBottom / gridSize) * gridSize;

        guiContext.pose.translate(x, y, 0);
        guiContext.pose.scale(scale, scale, 1f);
        guiContext.pose.translate(-offsetX, -offsetY, 0);

        float textureScaleX = gridSize / imageSize.width;
        float textureScaleY = gridSize / imageSize.height;

        guiContext.pose.scale(textureScaleX, textureScaleY, 1f);

        float drawX = gridStartX / textureScaleX;
        float drawY = gridStartY / textureScaleY;
        float drawW = (gridEndX - gridStartX) / textureScaleX;
        float drawH = (gridEndY - gridStartY) / textureScaleY;

        guiContext.drawTexture(graphViewStyle.gridTexture, drawX, drawY, drawW, drawH);

        guiContext.pose.popPose();

    }
}