package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.SpriteTextureClientSupport;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "graph-view", group = "container", registry = "ldlib2:ui_element")
public class GraphView extends UIElement {
    public record DragOffset(float startOffsetX, float startOffsetY) {}

    @Configurable(name = "GraphViewStyle")
    public class GraphViewStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.ALLOW_ZOOM,
                PropertyRegistry.ALLOW_PAN,
                PropertyRegistry.MIN_SCALE,
                PropertyRegistry.MAX_SCALE,
                PropertyRegistry.GRID_BACKGROUND,
                PropertyRegistry.GRID_SIZE,
        };

        public GraphViewStyle() {
            super(GraphView.this);
            setDefault(PropertyRegistry.GRID_BACKGROUND, SpriteTexture.of("ldlib2:textures/gui/grid_bg.png")
                    .setWrapMode(SpriteTexture.WrapMode.REPEAT));
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public GraphViewStyle allowZoom(boolean allowZoom) {
            set(PropertyRegistry.ALLOW_ZOOM, allowZoom);
            return this;
        }

        public boolean allowZoom() {
            return getValueSave(PropertyRegistry.ALLOW_ZOOM);
        }

        public GraphViewStyle allowPan(boolean allowPan) {
            set(PropertyRegistry.ALLOW_PAN, allowPan);
            return this;
        }

        public boolean allowPan() {
            return getValueSave(PropertyRegistry.ALLOW_PAN);
        }

        public GraphViewStyle minScale(float minScale) {
            set(PropertyRegistry.MIN_SCALE, minScale);
            return this;
        }

        public float minScale() {
            return getValueSave(PropertyRegistry.MIN_SCALE);
        }

        public GraphViewStyle maxScale(float maxScale) {
            set(PropertyRegistry.MAX_SCALE, maxScale);
            return this;
        }

        public float maxScale() {
            return getValueSave(PropertyRegistry.MAX_SCALE);
        }

        public GraphViewStyle gridTexture(IGuiTexture gridTexture) {
            set(PropertyRegistry.GRID_BACKGROUND, gridTexture);
            return this;
        }

        public IGuiTexture gridTexture() {
            return getValueSave(PropertyRegistry.GRID_BACKGROUND);
        }

        public GraphViewStyle gridSize(float gridSize) {
            set(PropertyRegistry.GRID_SIZE, gridSize);
            return this;
        }

        public float gridSize() {
            return getValueSave(PropertyRegistry.GRID_SIZE);
        }
    }

    public final UIElement contentRoot = new UIElement();
    @Getter
    private final GraphViewStyle graphViewStyle = new GraphViewStyle();

    // runtime
    @Getter @Setter
    private float offsetX = 0f, offsetY = 0f;  // world offset
    @Getter
    private float scale = 1f;

    public GraphView() {
        setOverflowVisible(false);

        contentRoot.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)).addClass("__graph-view_content-root__");
        contentRoot.transform(transform -> transform.pivot(0f, 0f));

        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        addEventListener(UIEvents.MOUSE_WHEEL, this::onMouseWheel);

        addChild(contentRoot);
        refreshContentTransform();
        internalSetup();
    }

    public GraphView graphViewStyle(Consumer<GraphViewStyle> style) {
        style.accept(this.graphViewStyle);
        return this;
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
        contentRoot.transform(transform -> transform
                .translate(-(offsetX * scale), -(offsetY * scale))
                .scale(scale)
        );
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

        fit(minX, minY, maxX, maxY, minScaleBound);
    }

    public void fit(float minX, float minY, float maxX, float maxY, float minScaleBound) {
        float w = Math.max(1f, maxX - minX);
        float h = Math.max(1f, maxY - minY);

        float sW = getContentWidth() / w;
        float sH = getContentHeight() / h;
        float newScale = Mth.clamp(Math.min(sW, sH), Math.max(minScaleBound, graphViewStyle.minScale()), graphViewStyle.maxScale());

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
        if (graphViewStyle.allowPan()
                && (event.target == this && event.button == 0 || event.button == 2)
                && isSelfOrChildHover()
                && isMouseOverContent(event.x, event.y)) {
            startDrag(new DragOffset(offsetX, offsetY), null);
        }
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragOffset(float startOffsetX, float startOffsetY)) {
            float invS = 1f / Math.max(0.0001f, Mth.clamp(scale, graphViewStyle.minScale(), graphViewStyle.maxScale()));
            var localMouse = getLocalMouse(event.x, event.y);
            var localStart = getLocalMouse(event.dragStartX, event.dragStartY);
            offsetX = startOffsetX + (localStart.x - localMouse.x) * invS;
            offsetY = startOffsetY + (localStart.y - localMouse.y) * invS;
            refreshContentTransform();
        }
    }

    protected void onMouseWheel(UIEvent event) {
        if (graphViewStyle.allowZoom() && event.target == this
                && isSelfOrChildHover()
                && isMouseOverContent(event.x, event.y)) {
            var newScale = Mth.clamp(scale + event.deltaY * 0.1f, graphViewStyle.minScale(), graphViewStyle.maxScale());
            if (newScale != scale) {
                var localMouse = getLocalMouse(event.x, event.y);
                var rx = localMouse.x - this.getPositionX();
                var ry = localMouse.y - this.getPositionY();
                offsetX += rx / scale - rx / newScale;
                offsetY += ry / scale - ry / newScale;
                scale = newScale;
            }
            refreshContentTransform();
        }
    }

    /// Editor
    @Override
    public void addEditorChild(UIElement child, int index) {
        if (index == -1) {
            contentRoot.addChild(child);
        } else {
            contentRoot.addChildAt(child, index);
        }
    }

    @LDLRegisterClient(name = "graph_view", registry = "ldlib2:ui_element_renderer")
    public static final class GraphViewRenderer extends DelegatingUIElementRenderer<GraphView, GraphViewRenderer> {
        @Override
        public Class<GraphView> type() {
            return GraphView.class;
        }

        @Override
        public void drawBackgroundAdditional(GraphView graphView, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundAdditional(graphView, context);
                return;
            }
            drawBackgroundAdditional(graphView, guiContext);
        }

        static void drawBackgroundAdditional(GraphView graphView, GUIContext context) {
            var x = graphView.getContentX();
            var y = graphView.getContentY();
            var w = graphView.getContentWidth();
            var h = graphView.getContentHeight();

            var imageWidth = graphView.getGraphViewStyle().gridSize();
            var imageHeight = graphView.getGraphViewStyle().gridSize();
            var gridSize = graphView.getGraphViewStyle().gridSize();

            if (graphView.getGraphViewStyle().gridTexture() instanceof SpriteTexture spriteTexture) {
                imageWidth = SpriteTextureClientSupport.getImageSize(spriteTexture).width;
                imageHeight = SpriteTextureClientSupport.getImageSize(spriteTexture).height;
            }

            context.pose.pushPose();

            float worldLeft = graphView.getOffsetX();
            float worldTop = graphView.getOffsetY();
            float worldRight = graphView.getOffsetX() + w / graphView.getScale();
            float worldBottom = graphView.getOffsetY() + h / graphView.getScale();

            float gridStartX = (float) Math.floor(worldLeft / gridSize) * gridSize;
            float gridStartY = (float) Math.floor(worldTop / gridSize) * gridSize;

            float gridEndX = (float) Math.ceil(worldRight / gridSize) * gridSize;
            float gridEndY = (float) Math.ceil(worldBottom / gridSize) * gridSize;

            context.pose.translate(x, y);
            context.pose.scale(graphView.getScale(), graphView.getScale());
            context.pose.translate(-graphView.getOffsetX(), -graphView.getOffsetY());

            float textureScaleX = gridSize / imageWidth;
            float textureScaleY = gridSize / imageHeight;

            context.pose.scale(textureScaleX, textureScaleY);

            float drawX = gridStartX / textureScaleX;
            float drawY = gridStartY / textureScaleY;
            float drawW = (gridEndX - gridStartX) / textureScaleX;
            float drawH = (gridEndY - gridStartY) / textureScaleY;

            context.drawTexture(graphView.getGraphViewStyle().gridTexture(), drawX, drawY, drawW, drawH);

            context.pose.popPose();
        }
    }
}
