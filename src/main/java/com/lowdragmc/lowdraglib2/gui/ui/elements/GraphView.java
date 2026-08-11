package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import lombok.Setter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
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
                PropertyRegistry.GRID_SIZE,
                PropertyRegistry.GRID_MIN_PIXELS,
                PropertyRegistry.GRID_SUBDIVISIONS,
                PropertyRegistry.GRID_LINE_WIDTH,
                PropertyRegistry.GRID_LINE_COLOR,
                PropertyRegistry.GRID_ACCENT_COLOR,
                PropertyRegistry.LOD_ENABLED,
                PropertyRegistry.LOD_SIMPLIFIED_PIXEL_SCALE,
                PropertyRegistry.LOD_BLOCK_PIXEL_SCALE,
        };

        public GraphViewStyle() {
            super(GraphView.this);
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

        /** Width of a grid line, in UI units. */
        public GraphViewStyle gridLineWidth(float gridLineWidth) {
            set(PropertyRegistry.GRID_LINE_WIDTH, gridLineWidth);
            return this;
        }

        public float gridLineWidth() {
            return getValueSave(PropertyRegistry.GRID_LINE_WIDTH);
        }

        /** Colour of the subdivision lines — the ones that fade in and out as you zoom. */
        public GraphViewStyle gridLineColor(int gridLineColor) {
            set(PropertyRegistry.GRID_LINE_COLOR, gridLineColor);
            return this;
        }

        public int gridLineColor() {
            return getValueSave(PropertyRegistry.GRID_LINE_COLOR);
        }

        /** Colour of the major lines — always fully drawn, one every {@link #gridSubdivisions()}. */
        public GraphViewStyle gridAccentColor(int gridAccentColor) {
            set(PropertyRegistry.GRID_ACCENT_COLOR, gridAccentColor);
            return this;
        }

        public int gridAccentColor() {
            return getValueSave(PropertyRegistry.GRID_ACCENT_COLOR);
        }

        public GraphViewStyle gridSize(float gridSize) {
            set(PropertyRegistry.GRID_SIZE, gridSize);
            return this;
        }

        public float gridSize() {
            return getValueSave(PropertyRegistry.GRID_SIZE);
        }

        /** Whether {@link #getLod()} may return anything other than {@link GraphViewLod#FULL}. */
        public GraphViewStyle lodEnabled(boolean lodEnabled) {
            set(PropertyRegistry.LOD_ENABLED, lodEnabled);
            return this;
        }

        public boolean lodEnabled() {
            return getValueSave(PropertyRegistry.LOD_ENABLED);
        }

        /**
         * Below this many <b>physical screen pixels per UI unit</b> the view reports
         * {@link GraphViewLod#SIMPLIFIED}. See {@link #getPixelScale()}.
         */
        public GraphViewStyle lodSimplifiedPixelScale(float pixelScale) {
            set(PropertyRegistry.LOD_SIMPLIFIED_PIXEL_SCALE, pixelScale);
            return this;
        }

        public float lodSimplifiedPixelScale() {
            return getValueSave(PropertyRegistry.LOD_SIMPLIFIED_PIXEL_SCALE);
        }

        /** Below this many physical screen pixels per UI unit the view reports {@link GraphViewLod#BLOCK}. */
        public GraphViewStyle lodBlockPixelScale(float pixelScale) {
            set(PropertyRegistry.LOD_BLOCK_PIXEL_SCALE, pixelScale);
            return this;
        }

        public float lodBlockPixelScale() {
            return getValueSave(PropertyRegistry.LOD_BLOCK_PIXEL_SCALE);
        }

        /**
         * Smallest on-screen spacing, in UI units, the grid is allowed to reach before it drops to a
         * coarser level. Keeps the grid readable at every zoom instead of collapsing into noise.
         */
        public GraphViewStyle gridMinPixels(float gridMinPixels) {
            set(PropertyRegistry.GRID_MIN_PIXELS, gridMinPixels);
            return this;
        }

        public float gridMinPixels() {
            return getValueSave(PropertyRegistry.GRID_MIN_PIXELS);
        }

        /**
         * Ratio between adjacent grid levels. 4 means each coarse cell holds 4x4 fine cells.
         *
         * <p>Integral by type, not by convention: the levels only nest — and the whole
         * skip-every-n-th scheme only works — if each lattice is a subset of the one below it.
         */
        public GraphViewStyle gridSubdivisions(int gridSubdivisions) {
            set(PropertyRegistry.GRID_SUBDIVISIONS, Math.max(2, gridSubdivisions));
            return this;
        }

        public int gridSubdivisions() {
            return Math.max(2, getValueSave(PropertyRegistry.GRID_SUBDIVISIONS));
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
    // Intra-frame LOD memo; see getLod(). Expires every frame and is additionally keyed on the pixel
    // scale, so a zoom mid-frame is still picked up.
    private GraphViewLod cachedLod = GraphViewLod.FULL;
    private float cachedLodPixelScale = Float.NaN;
    private boolean lodDirty = true;

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

    /**
     * Sets the zoom, clamped to the style's min/max, and refreshes the content transform.
     *
     * <p>Unlike {@link #setOffsetX(float)}/{@link #setOffsetY(float)} this cannot be a plain field
     * setter: zoom outside {@code [minScale, maxScale]} breaks the pan maths in
     * {@code onDragSourceUpdate}, and a scale change that does not refresh the transform leaves the
     * view rendering at the old zoom until the next pan or relayout.
     */
    public void setScale(float scale) {
        var newScale = Mth.clamp(scale, graphViewStyle.minScale(), graphViewStyle.maxScale());
        if (newScale == this.scale) return;
        this.scale = newScale;
        refreshContentTransform();
    }

    /**
     * The level of detail content should render itself at, derived from the current {@link #scale}.
     *
     * <p>Computed on demand rather than cached on zoom: it is two float comparisons, and computing
     * it lazily means a stylesheet that changes the thresholds at runtime takes effect immediately.
     *
     * @see GraphViewLod
     */
    /**
     * The level of detail content inside this canvas should draw itself at.
     *
     * <p>Memoised, because every node and every wire asks on every frame and the answer cannot change
     * between them. Recomputing meant three style-map lookups per element per frame for an answer
     * that is identical across the whole tree — several hundred thousand lookups a second on a large
     * graph, spent by the very machinery that exists to make large graphs cheap.
     */
    public GraphViewLod getLod() {
        float pixelScale = getPixelScale();
        if (!lodDirty && pixelScale == cachedLodPixelScale) {
            return cachedLod;
        }
        cachedLodPixelScale = pixelScale;
        cachedLod = resolveLod(pixelScale, graphViewStyle.lodEnabled(),
                graphViewStyle.lodSimplifiedPixelScale(), graphViewStyle.lodBlockPixelScale());
        lodDirty = false;
        return cachedLod;
    }

    /**
     * How many <b>physical screen pixels</b> one content unit currently occupies — the canvas zoom
     * multiplied by the window's GUI scale.
     *
     * <p>This, not the raw zoom, is what LOD keys off. Zoom alone says nothing about whether anything
     * is still legible: at GUI scale 4 a node at 0.3 zoom is physically twice the size of the same
     * node at GUI scale 2, so a fixed zoom threshold either strips detail that was perfectly readable
     * or keeps drawing text that has become a smear, depending purely on a setting the canvas never
     * looked at.
     *
     * <p>Ancestor transforms are not folded in: a graph view nested inside a scaled container is
     * rare, and reading the pose here would make the result depend on whether a frame has been
     * rendered yet.
     */
    public float getPixelScale() {
        return scale * (float) currentGuiScale();
    }

    /**
     * The LOD policy itself, split out from {@link #getLod()} so it can be exercised without a live
     * style engine, and so content that renders a graph outside a {@link GraphView} (a minimap, a
     * thumbnail) can apply the same rule.
     *
     * <p>Both thresholds are exclusive lower bounds: a view sitting exactly on
     * {@code simplifiedPixelScale} still draws in full.
     *
     * @param pixelScale physical screen pixels per UI unit, see {@link #getPixelScale()}
     */
    public static GraphViewLod resolveLod(float pixelScale, boolean enabled,
                                          float simplifiedPixelScale, float blockPixelScale) {
        if (!enabled) return GraphViewLod.FULL;
        if (pixelScale < blockPixelScale) return GraphViewLod.BLOCK;
        if (pixelScale < simplifiedPixelScale) return GraphViewLod.SIMPLIFIED;
        return GraphViewLod.FULL;
    }

    /** 1.0 off-client or before the window exists, so this is safe to call from a game test. */
    private static double currentGuiScale() {
        if (!LDLib2.isClient()) return 1d;
        var minecraft = Minecraft.getInstance();
        if (minecraft == null) return 1d;
        var window = minecraft.getWindow();
        return window == null ? 1d : window.getGuiScale();
    }

    protected void refreshContentTransform() {
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

    /**
     * One drawn level of the grid.
     *
     * @param cellSize  spacing in content units
     * @param color     ARGB the level's lines are drawn in
     * @param skipEvery omit every n-th line because a coarser level owns it; 0 draws all of them
     */
    public record GridLevel(float cellSize, int color, int skipEvery) {
    }

    /**
     * Picks the levels to draw and the colour of each.
     *
     * <p><b>Three levels, because two cannot be continuous once the grid has a visual hierarchy.</b>
     * Every level change promotes a lattice one step coarser. Its <em>position</em> never moves — the
     * coarse lattices are subsets of the fine one — so the only thing that can jump is how it is
     * drawn. With two levels the promoted lattice goes straight from subdivision colour to accent
     * colour, and that colour step is a visible pop even though nothing moved. A middle level lets
     * the promotion be interpolated instead: by the time a lattice is promoted it is already drawn
     * exactly the way the level above draws it.
     *
     * <pre>
     *   level k     subdivision colour, fading up from nothing   (densest, spacing &gt;= minPixels)
     *   level k+1   interpolating from subdivision toward accent
     *   level k+2   accent colour, fully drawn
     * </pre>
     *
     * <p>Each level skips every n-th line, which the level above owns, so no line is composited
     * twice. Overlapping layers would make the shared lines drift brighter as the fade rises and then
     * snap back at the change — the same pop by another route.
     *
     * <p>Levels finer than {@code k} are not drawn, and levels coarser than {@code k+2} do not need
     * to be: their positions are a subset of {@code k+2}'s, which draws all of its lines.
     *
     * @param fineSize spacing of the densest level, in content units
     * @param fade     0 when that level is at its densest, approaching 1 as it is about to be promoted
     */
    public static List<GridLevel> resolveGridLevels(float fineSize, float fade, int subdivisions,
                                                    int lineColor, int accentColor) {
        float coarseSize = fineSize * subdivisions;
        var levels = new ArrayList<GridLevel>(3);
        if (fade > 0.004f) {
            levels.add(new GridLevel(fineSize, withAlphaFactor(lineColor, fade), subdivisions));
        }
        levels.add(new GridLevel(coarseSize, ColorUtils.blendRGBColor(lineColor, accentColor, fade), subdivisions));
        levels.add(new GridLevel(coarseSize * subdivisions, accentColor, 0));
        return levels;
    }

    /** The densest grid level at a given zoom: its spacing in content units, and how far it has faded in. */
    public record GridFineLevel(float size, float fade) {
    }

    public static GridFineLevel resolveGridFineLevel(float scale, float base, float minPixels, int subdivisions) {
        double level = Math.ceil(Math.log(minPixels / (base * scale)) / Math.log(subdivisions));
        float size = base * (float) Math.pow(subdivisions, level);
        float fade = Mth.clamp((size * scale - minPixels) / (minPixels * (subdivisions - 1f)), 0f, 1f);
        return new GridFineLevel(size, fade);
    }

    /**
     * Hard ceiling on lines per level.
     *
     * <p>{@code grid-min-pixels} bounds the density in normal use, but it is a style property a
     * stylesheet may legally set to 1 — at which point a 4K viewport would ask for thousands of quads
     * per level. This keeps a misconfiguration from turning into a frame-rate cliff.
     */
    static final int MAX_GRID_LINES_PER_LEVEL = 512;

    /** Scales a colour's alpha, leaving RGB alone. */
    static int withAlphaFactor(int color, float factor) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Mth.clamp(factor, 0f, 1f));
        return (alpha << 24) | (color & 0x00FFFFFF);
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

        /**
         * Draws the background grid at an adaptive density.
         *
         * <p>A grid pinned to a single world-space size is only right at one zoom: zoom out and it
         * packs into an unreadable moire, zoom in and it becomes a handful of lines miles apart.
         * Instead the spacing snaps to whichever power of {@code grid-subdivisions} keeps on-screen
         * spacing at or above {@code grid-min-pixels}, so the grid reads the same at every zoom.
         *
         * <p>The levels are crossfaded rather than swapped, so zooming never pops. See
         * {@link GraphView#resolveGridLevels} for why that takes three levels and not two.
         *
         * <p>Real lines rather than a repeated tile, which is what this used to be: a bitmap grid
         * cell has to be minified to whatever the current spacing is, and a hairline inside it
         * disintegrates into a dotted shimmer long before the spacing gets tight.
         */
        static void drawBackgroundAdditional(GraphView graphView, GUIContext context) {
            // Runs once per frame, before any content is drawn, so this is where the per-frame LOD
            // memo is dropped. Keying only on the pixel scale would miss a style change that leaves
            // the zoom alone; expiring every frame keeps the memo purely an intra-frame optimisation.
            graphView.lodDirty = true;

            float x = graphView.getContentX();
            float y = graphView.getContentY();
            float w = graphView.getContentWidth();
            float h = graphView.getContentHeight();
            float scale = graphView.getScale();
            if (w <= 0 || h <= 0 || scale <= 0) return;

            var style = graphView.getGraphViewStyle();
            int subdivisions = style.gridSubdivisions();
            float base = Math.max(0.001f, style.gridSize());
            float minSpacing = Math.max(1f, style.gridMinPixels());
            float lineWidth = Math.max(0.5f, style.gridLineWidth());

            var fine = resolveGridFineLevel(scale, base, minSpacing, subdivisions);

            for (var level : resolveGridLevels(fine.size(), fine.fade(), subdivisions,
                    style.gridLineColor(), style.gridAccentColor())) {
                drawGridLines(graphView, context, level, x, y, w, h, lineWidth);
            }
        }

        /**
         * Draws one level of the grid.
         *
         * <p>One {@code drawSolidRect} per line rather than a hand-built vertex buffer: in 26.1 each
         * of those is a {@code GuiElementRenderState} appended to the frame's render state, and the
         * gui renderer batches every one of them into a single draw. There is nothing left to hoist.
         */
        private static void drawGridLines(GraphView graphView, GUIContext context, GridLevel level,
                                          float x, float y, float w, float h, float lineWidth) {
            int color = level.color();
            if ((color >>> 24) == 0) return;
            float cellSize = level.cellSize();
            float scale = graphView.getScale();
            if (cellSize * scale < 1f) return;

            float offsetX = graphView.getOffsetX();
            float offsetY = graphView.getOffsetY();
            int skipEvery = level.skipEvery();
            long firstX = (long) Math.floor(offsetX / cellSize);
            long lastX = (long) Math.ceil((offsetX + w / scale) / cellSize);
            long firstY = (long) Math.floor(offsetY / cellSize);
            long lastY = (long) Math.ceil((offsetY + h / scale) / cellSize);
            if (lastX - firstX > MAX_GRID_LINES_PER_LEVEL || lastY - firstY > MAX_GRID_LINES_PER_LEVEL) {
                return;
            }

            for (long i = firstX; i <= lastX; i++) {
                if (skipEvery > 0 && Math.floorMod(i, skipEvery) == 0) continue;
                float screenX = x + (i * cellSize - offsetX) * scale;
                DrawerHelperClient.drawSolidRect(context, screenX, y, lineWidth, h, color);
            }
            for (long i = firstY; i <= lastY; i++) {
                if (skipEvery > 0 && Math.floorMod(i, skipEvery) == 0) continue;
                float screenY = y + (i * cellSize - offsetY) * scale;
                DrawerHelperClient.drawSolidRect(context, x, screenY, w, lineWidth, color);
            }
        }
    }
}
