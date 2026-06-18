package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphLogger;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResourceProviderContainer;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard.Blackboard;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.CreateForeignLocalSubgraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.CreateSubgraphFromSelectionCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ElementRenameColorCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommandListener;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.ImportExternalSubgraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.NodeCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.WireCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ElementUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibrary;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.PlacematElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.StickyNoteElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.PortMigrationResult;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WirePlaceHolder;
import dev.vfyjxf.taffy.style.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GraphView extends UIElement {
    public record ElementUpdate(ModelElement element, ElementUpdateVisitor visitor) { }
    public record DragRegionSelection(UIElement selectionRect) {}
    public record DragMove(boolean targetWasSelected, Model target, List<Model> movables) {}

    public final UIElement header = new UIElement();
    public final UIElement canvas = new UIElement();
    public final com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView graphView = new com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView();
    public final ItemLibrary itemLibrary = new ItemLibrary(this);
    public final Blackboard blackboard = new Blackboard(this);
    public final GraphInspector inspector = new GraphInspector(this);
    public final GraphPreview preview = new GraphPreview(this);
    private final UIElement graphLogFooter = new UIElement();
    private final UIElement graphLogHeader = new UIElement();
    private final Label graphLogSummary = new Label();
    private final Label graphLogCount = new Label();
    private final ScrollerView graphLogList = new ScrollerView();

    /**
     * Optional instance-level veto consulted by {@link #dispatchCommand} before a command runs;
     * return {@code false} to block. Layered on top of the graph's own
     * {@link GraphModel#canExecuteCommand} policy (both must allow). For policy tied to the graph
     * definition, override {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#canExecuteCommand} instead.
     */
    @Nullable @Setter @Getter
    private Predicate<IGraphCommand> commandInterceptor;
    /** Observers notified after a command executes (see {@link #addCommandListener}). */
    private final List<GraphCommandListener> commandListeners = new ArrayList<>();

    // runtime
    @Nullable
    private GraphModel.CopyPasteData clipboardData = null;
    private boolean requireFitGraph = false;
    @Getter
    private GraphChangeset changeset = new GraphChangeset();
    @Getter
    private final UIElement panelLayer = new UIElement();
    public final DockManager dockManager = new DockManager(this);
    private final Map<String, UIElement> layers = new HashMap<>();
    private final UIElement fallbackLayer = new UIElement();
    @Nullable @Getter
    private Graph graph;
    @Getter
    private final Map<Model, ModelElement> modelElements = new HashMap<>();
    @Getter
    private final Map<UUID, ModelElement> modelElementsByID = new HashMap<>();
    @Getter
    private final Map<UUID, Set<ModelElement>> modelDependencies = new HashMap<>();
    private final List<ElementUpdate> updatePipeline = new ArrayList<>();
    @Getter
    private boolean isUpdateBatching = false;
    private final Set<Model> waitToSelected = new HashSet<>();
    private final Set<Model> waitToDeSelected = new HashSet<>();
    @Getter
    private boolean isSelectionBatching = false;
    @Getter
    private boolean isMenuOpen = false;
    @Getter
    private final Set<Model> selected = Sets.newHashSet();
    @Getter @Nullable
    private Vector4f dragRegionSelection = null; // local rect
    @Getter
    protected boolean isWireDragging = false;
    @Getter
    protected HistoryStack historyStack = new HistoryStack();
    private List<GraphLogger.Entry> graphLogEntries = List.of();
    private boolean graphLogExpanded = false;

    /** When true, drag-moved and newly-created elements snap their positions to {@link #gridSnapSize}. */
    @Getter @Setter
    private boolean snapToGrid = true;
    /** Pixel granularity for snap-to-grid alignment. Runtime-mutable; default 16. */
    @Getter @Setter
    private float gridSnapSize = 16f;


    public GraphView() {
        addClass("__node-graph-view__");

        graphView.addClass("__node-graph-view_canvas-view__");
        Style.defaultPipeline(this.graphView.getLayout(), l -> l.widthPercent(100).heightPercent(100));

        panelLayer.addClass("__node-graph-view_panel-layer__");
        Style.defaultPipeline(this.panelLayer.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE).width(0).height(0));

        // header initial
        header.addClass("__ui-editor-view_header__");
        Style.defaultPipeline(header.getLayout(), l -> l
                .widthPercent(100)
                .height(16)
                .paddingAll(1)
                .flexDirection(FlexDirection.ROW));
        Style.defaultPipeline(header.getStyle(), s -> s.backgroundTexture(Sprites.RECT_SOLID));
        initHeaders();

        // canvas
        canvas.addClass("__node-graph-view_canvas__");
        Style.defaultPipeline(canvas.getLayout(), l -> l.widthPercent(100).flex(1));

        graphView.addEventListener(UIEvents.MOUSE_DOWN, this::onGraphViewMouseDown);
        graphView.addEventListener(UIEvents.MOUSE_UP, this::onGraphViewMouseUp);
        graphView.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onGraphViewDragSourceUpdate);
        graphView.addEventListener(UIEvents.DRAG_END, this::onGraphViewDragEnd);
        graphView.addEventListener(UIEvents.DRAG_PERFORM, this::onGraphViewDragPerform);
        fallbackLayer.addClass("__node-graph-view_fallback-layer__");
        fallbackLayer.setAllowHitTest(false);
        Style.defaultPipeline(fallbackLayer.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
        graphView.addContentChild(fallbackLayer);
        setLayers(List.of(PlacematElement.PLACEMAT_LAYER, WireElement.WIRE_LAYER, NodeElement.NODE_LAYER, StickyNoteElement.STICKY_NOTE_LAYER));
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        addEventListener(UIEvents.DRAG_END, this::onDragEnd);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        addEventListener(UIEvents.VALIDATE_COMMAND, this::onValidateCommand);
        addEventListener(UIEvents.EXECUTE_COMMAND, this::onExecuteCommand);

        setEnforceFocus(Consumers.nop());

        // ItemLibrary is hidden until explicitly shown — popup visibility is state-driven.
        Style.importantPipeline(itemLibrary.getLayout(), l -> l.display(TaffyDisplay.NONE));
        inspector.setHistoryStack(historyStack);

        initPanels();
        initGraphLogFooter();

        addChildren(header, canvas.addChildren(graphView, panelLayer, graphLogFooter));
    }


    protected void initHeaders() {
        // left section
        var leftSection = new UIElement();
        leftSection.addClass("__node-graph-view_header-left__");
        Style.defaultPipeline(leftSection.getLayout(), l -> l.flexDirection(FlexDirection.ROW).heightPercent(100).flex(1));
        var undoBtn = new Button();
        undoBtn.setText("Undo").setOnClick(event -> historyStack.undo());
        undoBtn.addClass("__node-graph-view_header-undo__");
        Style.defaultPipeline(undoBtn.getLayout(), l -> l.width(30));
        Style.defaultPipeline(undoBtn.getStyle(), s -> s.tooltips("Ctrl+Z"));
        var redoBtn = new Button();
        redoBtn.setText("Redo").setOnClick(event -> historyStack.redo());
        redoBtn.addClass("__node-graph-view_header-redo__");
        Style.defaultPipeline(redoBtn.getLayout(), l -> l.width(30));
        Style.defaultPipeline(redoBtn.getStyle(), s -> s.tooltips("Ctrl+Y / Ctrl+Shift+Z"));
        leftSection.addChildren(undoBtn, redoBtn);

        // center section
        var centerSection = new UIElement();
        centerSection.addClass("__node-graph-view_header-center__");
        Style.defaultPipeline(centerSection.getLayout(), l -> l.heightPercent(100));

        // right section
        var rightSection = new UIElement();
        rightSection.addClass("__node-graph-view_header-right__");
        Style.defaultPipeline(rightSection.getLayout(), l -> l.flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.FLEX_END)
                .gapAll(2)
                .heightPercent(100)
                .flex(1));
        var snapToggle = new Toggle();
        snapToggle.addClass("__node-graph-view_header-snap-toggle__");
        snapToggle.noText()
                .setOn(snapToGrid, false)
                .setOnToggleChanged(this::setSnapToGrid)
                .bindDataSource(SupplierDataSource.of(() -> snapToGrid));
        Style.defaultPipeline(snapToggle.getToggleStyle(), style -> style.baseTexture(Sprites.BORDER1_RT1_DARK)
                .hoverTexture(Sprites.BORDER1_RT1)
                .markTexture(Icons.MAGNET)
                .unmarkTexture(Icons.MAGNET));
        Style.defaultPipeline(snapToggle.getLayout(), l -> l.width(14).heightPercent(100));
        Style.defaultPipeline(snapToggle.getStyle(), s -> s.tooltips("graph.snap_to_grid"));
        rightSection.addChild(snapToggle);

        var fitBtn = new Button();
        fitBtn.noText().setOnClick(event -> fitGraphChildren());
        fitBtn.addClass("__node-graph-view_header-fit-button__");
        Style.defaultPipeline(fitBtn.getLayout(), l -> l.width(14));
        Style.defaultPipeline(fitBtn.getStyle(), s -> s.tooltips("GraphView.fit"));
        var fitIcon = new UIElement().addClass("__white_icon__");
        fitIcon.addClass("__node-graph-view_header-fit-icon__");
        Style.defaultPipeline(fitIcon.getLayout(), l -> l.heightPercent(100).setAspectRatio(1));
        Style.defaultPipeline(fitIcon.getStyle(), s -> s.backgroundTexture(Icons.PAGE_FIT));
        fitBtn.addChild(fitIcon);
        rightSection.addChild(fitBtn);

        header.addChildren(leftSection, centerSection, rightSection);
    }

    protected void initPanels() {
        var bbPanel = new GraphPanel(this, blackboard);
        var insPanel = new GraphPanel(this, inspector);
        var prevPanel = new GraphPanel(this, preview);
        Style.defaultPipeline(bbPanel.getLayout(), l -> l.width(160));
        panelLayer.addChildren(bbPanel, insPanel, prevPanel);
        dockManager.register(bbPanel, DockSlot.TOP_LEFT);
        dockManager.register(insPanel, DockSlot.TOP_RIGHT);
        dockManager.register(prevPanel, DockSlot.BOTTOM_RIGHT);
    }

    protected void initGraphLogFooter() {
        graphLogFooter.addClass("__node-graph-view_log-footer__");
        Style.defaultPipeline(graphLogFooter.getLayout(), l -> l
                .positionType(TaffyPosition.ABSOLUTE)
                .left(8)
                .right(8)
                .bottom(8)
                .height(16)
                .paddingAll(3)
                .gapAll(2));
        Style.defaultPipeline(graphLogFooter.getStyle(), s -> s.background(
                new SDFRectTexture()
                        .setRadius(4)
                        .setStroke(0.5f)
                        .setColor(0xCC1E1F22)
                        .setBorderColor(0xAA7F8084)));
        Style.importantPipeline(graphLogFooter.getLayout(), l -> l.display(TaffyDisplay.NONE));
        graphLogFooter.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (!graphLogEntries.isEmpty()) {
                setGraphLogExpanded(!graphLogExpanded);
            }
            event.stopPropagation();
        });

        graphLogHeader.addClass("__node-graph-view_log-header__");
        Style.defaultPipeline(graphLogHeader.getLayout(), l -> l
                .height(10)
                .widthPercent(100)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(4));

        graphLogSummary.addClass("__node-graph-view_log-summary__");
        graphLogSummary.setText(Component.empty());
        Style.defaultPipeline(graphLogSummary.getLayout(), l -> l.flex(1).heightPercent(100));
        Style.defaultPipeline(graphLogSummary.getTextStyle(), s -> s
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HOVER_ROLL)
                .textShadow(false));
        Style.defaultPipeline(graphLogSummary.getStyle(), s -> s.overflowVisible(false));

        graphLogCount.addClass("__node-graph-view_log-count__");
        graphLogCount.setText(Component.empty());
        Style.defaultPipeline(graphLogCount.getLayout(), l -> l.width(42).heightPercent(100));
        Style.defaultPipeline(graphLogCount.getTextStyle(), s -> s
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .textColor(ColorPattern.LIGHT_GRAY.color)
                .textShadow(false));

        graphLogList.addClass("__node-graph-view_log-list__");
        Style.defaultPipeline(graphLogList.getLayout(), l -> l.widthPercent(100).flex(1));
        graphLogList.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .verticalScrollDisplay(ScrollDisplay.AUTO));
        Style.importantPipeline(graphLogList.getLayout(), l -> l.display(TaffyDisplay.NONE));

        graphLogHeader.addChildren(graphLogSummary, graphLogCount);
        graphLogFooter.addChildren(graphLogHeader, graphLogList);
    }

    /**
     * Sets the layer configuration for this {@code GraphEditor} instance using the specified order of layers.
     * Each layer is represented as a {@code UIElement} and will be added to the {@code graphView}.
     *
     * @param layerOrder A list of layer names defining the order in which the layers should exist.
     *                   Layer names should be unique and will be used as IDs for {@code UIElement}.
     * @return The current {@code GraphEditor} instance to allow method chaining.
     */
    public GraphView setLayers(List<String> layerOrder) {
        layers.values().forEach(UIElement::removeSelf);
        layers.clear();
        for (var layerName : layerOrder) {
            var layer = new UIElement();
            // setId is preserved here because getLayer() looks layers up by id-as-key, but we also
            // expose an internal class so stylesheets can target each layer.
            layer.setId(layerName);
            layer.addClass("__node-graph-view_layer__");
            layer.addClass("__node-graph-view_layer-" + layerName.toLowerCase() + "__");
            layer.setAllowHitTest(false);
            Style.defaultPipeline(layer.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE));
            graphView.addContentChild(layer);
            layers.put(layerName, layer);
        }
        return this;
    }

    /**
     * Retrieves a {@link UIElement} corresponding to the specified layer name.
     * If the layer name is not found or is null/empty, the fallback layer or {@code null} is returned.
     *
     * @param layerName the name of the layer to retrieve; this can be {@code null}.
     * @return the {@link UIElement} for the specified layer name, or {@code null} if the layer is not found or if the input is invalid.
     */
    public @Nullable UIElement getLayer(@Nullable String layerName) {
        if (layerName == null || layerName.isEmpty()) return null;
        return layers.getOrDefault(layerName, fallbackLayer);
    }

    /**
     * Loads a new {@link Graph} into the current {@code GraphView}. If a graph is already loaded,
     * it is cleared before the new graph is added. Updates the user interface elements to reflect
     * the newly loaded graph.
     *
     * @param graph the {@link Graph} to be loaded into the view. This parameter can be {@code null}.
     *              If {@code null}, the view will be cleared and no graph will be loaded.
     * @return the current {@code GraphView} instance to allow method chaining.
     */
    public GraphView loadGraph(@Nullable Graph graph) {
        clearGraph();
        this.graph = graph;
        if (this.graph == null) return this;
        this.itemLibrary.onLoadGraph(graph.graphModel);
        buildUITree(this.graph.graphModel);
        requireFitGraph = true;
        // Push initial snapshot so the first command can be undone
        historyStack.clearHistory();
        var provider = Platform.getFrozenRegistry();
        var initialTag = graph.graphModel.serializeNBT(provider);
        historyStack.pushHistory(Component.translatable("initial"),
                EditAction.of(
                        () -> { graph.graphModel.deserializeNBT(provider, initialTag); rebuildGraphUI(); },
                        () -> { graph.graphModel.deserializeNBT(provider, initialTag); rebuildGraphUI(); }
                ), null, false);
        refreshGraphLogger();
        return this;
    }

    public void clearGraph() {
        this.modelElements.clear();
        this.modelElementsByID.clear();
        this.modelDependencies.clear();
        this.selected.clear();
        this.layers.values().forEach(UIElement::clearAllChildren);
        this.isWireDragging = false;
        this.changeset.clear();
        this.inspector.clear();
        this.blackboard.clear();
        this.graphLogEntries = List.of();
        this.graphLogExpanded = false;
        updateGraphLogFooter();
    }

    /**
     * Rebuilds the entire graph UI from the current graph model state.
     * Used after undo/redo to synchronize the UI with the deserialized model.
     */
    public void rebuildGraphUI() {
        if (graph == null) return;
        clearGraph();
        graph.graphModel.getCurrentGraphChangeDescription().clear();
        buildUITree(graph.graphModel);
        refreshGraphLogger();
    }

    /**
     * Manually reruns the graph diagnostic hook and refreshes the floating logger footer.
     */
    public void refreshGraphLogger() {
        if (graph == null) {
            graphLogEntries = List.of();
            graphLogExpanded = false;
            updateGraphLogFooter();
            return;
        }

        var logger = new GraphLogger();
        try {
            graph.graphModel.onGraphChanged(logger);
        } catch (RuntimeException e) {
            LDLib2.LOGGER.error("Graph validation hook failed", e);
            logger.error(Component.literal("Graph validation hook failed: " + e.getMessage()));
        }
        graphLogEntries = logger.getSortedEntries();
        if (graphLogEntries.isEmpty()) {
            graphLogExpanded = false;
        }
        updateGraphLogFooter();
    }

    protected void setGraphLogExpanded(boolean expanded) {
        graphLogExpanded = expanded && !graphLogEntries.isEmpty();
        updateGraphLogFooter();
    }

    protected void updateGraphLogFooter() {
        graphLogList.clearAllScrollViewChildren();
        if (graphLogEntries.isEmpty()) {
            graphLogSummary.setText(Component.empty());
            graphLogCount.setText(Component.empty());
            Style.importantPipeline(graphLogFooter.getLayout(), l -> l.display(TaffyDisplay.NONE));
            return;
        }

        var first = graphLogEntries.getFirst();
        graphLogSummary.setText(formatGraphLogEntry(first));
        Style.importantPipeline(graphLogSummary.getTextStyle(), s -> s.textColor(graphLogLevelColor(first.level())));
        graphLogCount.setText(graphLogEntries.size() > 1
                ? Component.translatable("graph.logger.count", graphLogEntries.size())
                : Component.empty());

        if (graphLogExpanded) {
            for (var entry : graphLogEntries) {
                graphLogList.addScrollViewChild(createGraphLogRow(entry));
            }
        }

        Style.importantPipeline(graphLogFooter.getLayout(), l -> l
                .display(TaffyDisplay.FLEX)
                .height(graphLogExpanded ? 96 : 16));
        Style.importantPipeline(graphLogList.getLayout(), l -> l
                .display(graphLogExpanded ? TaffyDisplay.FLEX : TaffyDisplay.NONE));
    }

    protected UIElement createGraphLogRow(GraphLogger.Entry entry) {
        var row = new UIElement();
        row.addClass("__node-graph-view_log-row__");
        Style.defaultPipeline(row.getLayout(), l -> l
                .widthPercent(100)
                .height(12)
                .paddingAll(1)
                .flexDirection(FlexDirection.ROW));

        var label = new Label();
        label.addClass("__node-graph-view_log-row-label__");
        label.setText(formatGraphLogEntry(entry));
        Style.defaultPipeline(label.getLayout(), l -> l.flex(1).heightPercent(100));
        Style.defaultPipeline(label.getTextStyle(), s -> s
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HOVER_ROLL)
                .textColor(graphLogLevelColor(entry.level()))
                .textShadow(false));
        Style.defaultPipeline(label.getStyle(), s -> s.overflowVisible(false));
        row.addChild(label);
        return row;
    }

    protected Component formatGraphLogEntry(GraphLogger.Entry entry) {
        return Component.literal("")
                .append(graphLogLevelComponent(entry.level()))
                .append(Component.literal(": "))
                .append(entry.message());
    }

    protected Component graphLogLevelComponent(GraphLogger.Level level) {
        return switch (level) {
            case ERROR -> Component.translatable("graph.logger.level.error");
            case WARNING -> Component.translatable("graph.logger.level.warning");
            case INFO -> Component.translatable("graph.logger.level.info");
        };
    }

    protected int graphLogLevelColor(GraphLogger.Level level) {
        return switch (level) {
            case ERROR -> ColorPattern.BRIGHT_RED.color;
            case WARNING -> ColorPattern.YELLOW.color;
            case INFO -> ColorPattern.LIGHT_BLUE.color;
        };
    }

    public void fitGraphChildren() {
        fitGraphChildren(15);
    }

    /**
     * Adjusts the graph view to fit all visible and displayed children within its bounds,
     * adding the specified padding around the edges.
     *
     * The method calculates the minimal and maximal bounds of all visible child elements
     * within the graph and resizes the graph view's viewport accordingly, ensuring
     * all elements are fully contained with a margin defined by {@code padding}.
     *
     * @param padding the additional space in pixels to include around the boundaries
     *                of the visible child elements when resizing the graph view.
     *                A positive value increases the margin, while a value of zero
     *                only fits the exact boundaries.
     */
    public void fitGraphChildren(float padding) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean has = false;

        for (var child : modelElements.values()) {
            if (!child.isDisplayed() || !child.isVisible()) continue;
            float x = child.getPositionX() - graphView.getContentX();
            float y = child.getPositionY() - graphView.getContentY();
            float w = child.getSizeWidth();
            float h = child.getSizeHeight();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x + w);
            maxY = Math.max(maxY, y + h);
            has = true;
        }

        if (!has) return;

        fitGraph(minX, minY, maxX, maxY, padding);
    }

    public void fitGraph(float minX, float minY, float maxX, float maxY, float padding) {
        minX -= padding; minY -= padding;
        maxX += padding; maxY += padding;

        graphView.fit(minX, minY, maxX, maxY, 0.1f);
    }

    /**
     * Constructs and initializes the UI tree for the provided {@link GraphModel}.
     * This involves creating and adding UI elements for various components such as
     * placeholders, nodes, wires, and placemats, based on the model structure.
     *
     * @param graphModel the {@link GraphModel} containing the data structure
     *                   representing the graph, including placeholders, nodes,
     *                   wires, and placemats. This model is used to generate
     *                   corresponding UI elements.
     */
    protected void buildUITree(GraphModel graphModel) {
        // changes are resolved here
        graphModel.getCurrentGraphChangeDescription().clear();
        // placeholders
        for (var placeholder : graphModel.getPlaceholders()) {
            switch (placeholder) {
                case DeclarationModel declarationModel: continue;
                case WirePlaceHolder wirePlaceHolder:
                    createWireUI(wirePlaceHolder);
                    continue;
                case NodePlaceholder nodePlaceholder:
                    createAndAddModelElement(nodePlaceholder);
                    break;
                default: break;
            }
        }

//        ContentViewContainer.Add(m_MarkersParent);
//        m_MarkersParent.Clear();

        // nodes
        for (var nodeModel : graphModel.getNodeModels()) {
            // GraphModel.removeNode replaces the slot with null instead of compacting the list
            // (so indices stay stable across edits) — same null-slot pattern as wireModels. Skip
            // those gaps before touching the model.
            if (nodeModel == null) continue;
            // skip nodes that require container, e.g., BlockNodeModel
            if (nodeModel.needsContainer()) continue;
            // The preview panel is rendered inside the node element (see NodeElement#buildPreviewPart),
            // so we don't register a separate top-level element for the preview model here.
            createAndAddModelElement(nodeModel);
        }

        // sticky notes
        for (var stickyNoteModel : graphModel.getStickyNoteModels()) {
            createAndAddModelElement(stickyNoteModel);
        }

        // wire
        int index = 0;
        for (var wire : graphModel.getWireModels()) {
            if (!createWireUI(wire)) {
                LDLib2.LOGGER.warn("wire {} cannot be restored: {}", index, wire);
            }
            index++;
        }

        // placemats
        var placemats = new ArrayList<ModelElement>();
        for (var placematModel : graphModel.getPlacematModels()) {
            var placematUI = createAndAddModelElement(placematModel);
            if (placematUI != null) placemats.add(placematUI);
        }

        // We need to do this after all graph elements are created.
        for (var placemat : placemats) {
            placemat.updateElement(ModelUpdateVisitor.UNSPECIFIED);
        }

        // variables
        blackboard.doCompleteUpdate();
    }

    public UIElement getContentViewContainer() {
        return graphView.contentRoot;
    }

    /**
     * Dispatches a command to be executed on the current graph model.
     * The command is applied only if the graph is not {@code null}.
     *
     * @param command the {@link IGraphCommand} instance to execute. The command cannot be {@code null}.
     * @return {@code true} if the command was successfully dispatched and executed; {@code false} if the graph is {@code null}.
     */
    public boolean dispatchCommand(IGraphCommand command) {
        if (graph == null) return false;
        var graphModel = graph.graphModel;
        // before-veto: the graph's own policy AND the optional instance interceptor must both allow.
        if (!graphModel.canExecuteCommand(command)) return false;
        if (commandInterceptor != null && !commandInterceptor.test(command)) return false;
        command.execute(this, graphModel);
        // post-execute: graph hook first, then registered listeners (copy to tolerate mutation).
        graphModel.onCommandExecuted(command);
        if (!commandListeners.isEmpty()) {
            for (var listener : List.copyOf(commandListeners)) {
                listener.onCommandExecuted(command, this, graphModel);
            }
        }
        refreshGraphLogger();
        return true;
    }

    /** Registers an observer notified after each command executes. */
    public GraphView addCommandListener(GraphCommandListener listener) {
        if (listener != null && !commandListeners.contains(listener)) {
            commandListeners.add(listener);
        }
        return this;
    }

    /** Removes a previously-registered command listener. */
    public boolean removeCommandListener(GraphCommandListener listener) {
        return commandListeners.remove(listener);
    }

    public boolean batchUpdate() {
        var isBatching = isUpdateBatching;
        isUpdateBatching = true;
        return !isBatching;
    }

    public void batchUpdate(Runnable runnable) {
        var isBatching = isUpdateBatching;
        isUpdateBatching = true;
        runnable.run();
        if (!isBatching) {
            endBatchUpdate();
        }
    }

    /**
     * Dispatches an update operation to the specified {@link ModelElement} using the provided
     * {@link ElementUpdateVisitor}. If batch updating is enabled, the update is added to a pipeline
     * of pending updates; otherwise, it is executed immediately.
     *
     * @param element the {@link ModelElement} to update. This parameter cannot be {@code null}.
     * @param visitor the {@link ElementUpdateVisitor} responsible for performing the update logic. This parameter cannot be {@code null}.
     */
    public void dispatchUpdate(ModelElement element, ElementUpdateVisitor visitor) {
        if (isUpdateBatching) {
            updatePipeline.add(new ElementUpdate(element, visitor));
        } else {
            element.updateElement(visitor);
        }
    }

    /**
     * Ends the batch update process and applies all pending updates in the update pipeline.
     *
     * This method transitions {@code isUpdateBatching} to {@code false} after processing
     * all elements in the {@code updatePipeline}. During execution, the pipeline is repeatedly
     * cleared and processed until it becomes empty. Each update task invokes the
     * {@code updateElement} method for the respective {@code element} with its provided
     * {@code visitor}.
     *
     * Ensure that this method is called after invoking corresponding batch-initiating logic
     * to process accumulated updates.
     */
    public void endBatchUpdate() {
        isUpdateBatching = true;
        while (!updatePipeline.isEmpty()) {
            var copied = List.copyOf(updatePipeline);
            updatePipeline.clear();
            copied.forEach(e -> e.element.updateElement(e.visitor));
        }
        isUpdateBatching = false;
    }

    protected void registerModelElement(ModelElement element) {
        if (element.getModel() == null) return;
        modelElements.put(element.getModel(), element);
        modelElementsByID.put(element.getModel().getUid(), element);
    }

    protected void unregisterModelElement(ModelElement element) {
        if (element.getModel() == null) return;
        modelElements.remove(element.getModel());
        modelElementsByID.remove(element.getModel().getUid());
    }

    /**
     * Adds a dependency between a model and a UI.
     */
    public void addModelDependency(UUID uid, ModelElement ui) {
        modelDependencies.computeIfAbsent(uid, u -> new HashSet<>()).add(ui);
    }

    /**
     * Removes a dependency between a model and a UI.
     */
    public void removeModelDependency(UUID uid, ModelElement ui) {
        Optional.ofNullable(modelDependencies.get(uid)).ifPresent(set -> set.remove(ui));
    }

    public Set<ModelElement> getModelDependencies(UUID uid) {
        return modelDependencies.getOrDefault(uid, Collections.emptySet());
    }

    public @Nullable ModelElement getModelElement(@Nullable Model model) {
        return modelElements.get(model);
    }

    public @Nullable ModelElement getModelElement(@Nullable UUID uid) {
        return modelElementsByID.get(uid);
    }

    /**
     * Adds a graph element to the appropriate layer in the graph view.
     *
     * @param element the {@link ModelElement} to add. This can be {@code null}.
     * @return {@code true} if the element was successfully added to the graph view, {@code false} otherwise.
     */
    public boolean addElement(@Nullable ModelElement element) {
        if (element == null) return false;
        var layer = getLayer(element.getLayerName());
        if (layer == null) return false;
        var model = element.getModel();
        element.setGraphView(this);
        layer.addChild(element);
        wireSelectableElement(element);
        if (model instanceof IMovable movable) {
            // position is driven by the model — pin via IMPORTANT.
            Style.importantPipeline(element.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                    .left(movable.getPosition().x)
                    .top(movable.getPosition().y));
        }
        return true;
    }

    /**
     * Wires MOUSE_DOWN selection + drag-to-move for a {@link ModelElement}. Called automatically
     * by {@link #addElement} for top-level graph elements; nested elements (e.g. block nodes
     * inside a context) must call it explicitly because they are not added to a layer.
     */
    public void wireSelectableElement(@Nullable ModelElement element) {
        if (element == null || !element.isSelectable() || element.getModel() == null) return;
        var model = element.getModel();
        element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            if (element.allowGraphMouseDown(event)) {
                var tagetWasSelected = isSelected(model);
                batchSelection(() -> {
                    // select node
                    if (!event.isCtrlDown() && !isSelected(model)) {
                        clearAllSelected();
                    }
                    addSelected(model);
                    moveElementTop(element);
                });

                // drag movable — include fully contained nodes when dragging a placemat. Filter
                // by the MOVABLE capability so non-movable nodes (e.g. BlockNodeModel) don't
                // start a DragMove that would preempt their own drag-reorder handlers.
                var movablesList = new ArrayList<>(selected.stream()
                        .filter(m -> m instanceof IMovable
                                && (!(m instanceof GraphElementModel gem) || gem.isMovable()))
                        .toList());
                for (var sel : new ArrayList<>(movablesList)) {
                    if (sel instanceof PlacematModel pm) {
                        java.util.function.Function<com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.AbstractNodeModel, Vector2f> sizeLookup = node -> {
                            var nodeEl = getModelElement(node);
                            return nodeEl != null ? new Vector2f(nodeEl.getSizeWidth(), nodeEl.getSizeHeight()) : null;
                        };
                        for (var contained : pm.getContainedNodes(sizeLookup)) {
                            if (contained != null && !movablesList.contains(contained)) {
                                movablesList.add(contained);
                            }
                        }
                    }
                }
                var movables = List.copyOf(movablesList);
                if (movables.isEmpty()) return;
                var width = 12;
                var height = 12;
                startDrag(new DragMove(tagetWasSelected, model, movables), Icons.MOVE).setDragTexture(- width / 2f, -height / 2f, width, height);
            }
        }, element.isGraphMouseDownCaptured());
    }

    /**
     * Removes a graph element from the graph view.
     * @param element the {@link ModelElement} to remove. This can be {@code null}.
     * @return {@code true} if the element was successfully removed from the graph view, {@code false} otherwise.
     */
    public boolean removeElement(@Nullable ModelElement element) {
        if (element != null) {
            // remove from selected
            if (selected.contains(element.getModel())) {
                removeSelected(element.getModel());
            }
            var layer = getLayer(element.getLayerName());
            if (layer != null && layer.removeChild(element)) {
                element.clearDependencies();
                element.setGraphView(null);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new {@link ModelElement} instance based on the provided model and adds it
     * to the graph view. If the model is already associated with an existing {@link ModelElement},
     * the existing element is returned instead.
     *
     * <br>
     * Besides, it will do {@link ModelElement#doCompleteUpdate()} to initialize the elements.
     *
     * @param model the {@link Model} that serves as the basis for creating
     *              a {@link ModelElement}. This can be an instance of {@link IGraphElementUIModel}.
     *              If {@code null} or if the model fails to create a valid {@link ModelElement},
     *              the method will return {@code null}.
     *
     * @return the created {@link ModelElement}, or an existing instance if one is already
     *         associated with the provided model. Returns {@code null} if the model is not
     *         of a compatible type, if no element could be created, or if an error occurs
     *         during creation.
     */
    @Nullable
    public ModelElement createAndAddModelElement(@Nullable Model model) {
        if (model instanceof IGraphElementUIModel graphElement) {
            var element = modelElements.get(model);
            if (element != null) return element;
            var elementUI = graphElement.createElementUI();
            if (elementUI != null && addElement(elementUI)) {
                elementUI.doCompleteUpdate();
            }
            return elementUI;
        }
        return null;
    }

    public void moveElementTop(ModelElement element) {
        // move to the top of the layer
        var layer = getLayer(element.getLayerName());
        if (layer != null && layer.hasChild(element) && layer.getChildren().size() > (element.getSiblingIndex() + 1)) {
            layer.removeChild(element);
            layer.addChild(element);
        }
    }

    /**
     * Toggles the selection batching state for the current operation.
     * If selection batching is already active, it sets the state to active again
     * and returns {@code false}. If selection batching is inactive, it activates
     * the state and returns {@code true}.
     *
     * @return {@code true} if selection batching was previously inactive and has now been activated,
     *         {@code false} if selection batching was already active.
     */
    public boolean batchSelection() {
        var isBatching = isSelectionBatching;
        isSelectionBatching = true;
        return !isBatching;
    }

    /**
     * Executes the provided {@code Runnable} within a batch selection context.
     * Ensures that selection batching is enabled during execution
     * and disables it afterward if it was not already enabled.
     *
     * @param runnable the {@code Runnable} task to be executed within the batch selection context
     */
    public void batchSelection(Runnable runnable) {
        var isBatching = isSelectionBatching;
        isSelectionBatching = true;
        runnable.run();
        if (!isBatching) {
            endBatchSelection();
        }
    }

    /**
     * Ends the batch selection process and processes changes in selection.
     *
     * This method finalizes the current batch selection operations by handling
     * pending additions and removals of models from the selection. It ensures that:
     * <ul>
     * - Models in the {@code waitToSelected} set are added to the {@code selected} set.
     * - Models in the {@code waitToDeSelected} set are removed from the {@code selected} set.
     * - Calls the {@code onSelectionChanged} method on the corresponding elements of models
     *   whose selection state has been updated.
     * </ul>
     * Once all pending operations are processed, the batching mode is disabled.
     *
     * Note:
     * - The method ensures that no duplicate notifications are triggered for models.
     * - {@code isSelectionBatching} is temporarily set to {@code true} during processing,
     *   and reset to {@code false} once the method completes execution.
     *
     * Dependencies:
     * - {@link Model} represents the model being selected or deselected.
     * - {@code modelElements} is a mapping of models to their corresponding elements
     *   that can respond to selection changes.
     */
    public void endBatchSelection() {
        isSelectionBatching = true;
        var currentSelection = new HashSet<>(selected);
        Set<Model> toNotify = new HashSet<>();
        while (!waitToSelected.isEmpty() || !waitToDeSelected.isEmpty()) {
            if (!waitToSelected.isEmpty()) {
                var copyAdded = List.copyOf(waitToSelected);
                waitToSelected.clear();
                for (var model : copyAdded) {
                    if (!selected.contains(model)) {
                        selected.add(model);
                        toNotify.add(model);
                    }
                }
            }
            if (!waitToDeSelected.isEmpty()) {
                var copyRemoved = List.copyOf(waitToDeSelected);
                waitToDeSelected.clear();
                for (var model : copyRemoved) {
                    if (selected.contains(model)) {
                        selected.remove(model);
                        toNotify.add(model);
                    }
                }
            }
        }
        for (Model model : toNotify) {
            var element = modelElements.get(model);
            if (element != null) {
                element.onSelectionChanged();
            }
        }
        if (!currentSelection.equals(selected)) {
            inspector.clear();
            if (selected.size() == 1) {
                var model = selected.iterator().next();
                var element = modelElements.get(model);
                if (element != null) {
                    element.onSelectionInspect(inspector);
                }
            }
        }
        isSelectionBatching = false;
    }

    public void addSelected(Model model) {
        batchSelection(() -> {
            waitToSelected.add(model);
            waitToDeSelected.remove(model);
        });
    }

    public void removeSelected(Model model) {
        batchSelection(() -> {
            waitToDeSelected.add(model);
            waitToSelected.remove(model);
        });
    }

    public void clearAllSelected() {
        batchSelection(() -> {
            waitToDeSelected.addAll(selected);
            waitToSelected.clear();
        });
    }

    public boolean isSelected(Model nodeModel) {
        return selected.contains(nodeModel);
    }

    @Override
    public boolean isSelfOrChildHover() {
        return !isMenuOpen && super.isSelfOrChildHover();
    }

    /**
     * Rounds a canvas-local position to the snap grid. Returns the input unchanged when snap is
     * disabled. Returns a new {@link Vector2f}; the input is not mutated.
     */
    public Vector2f snapPosition(Vector2f pos) {
        if (!snapToGrid || gridSnapSize <= 0) return new Vector2f(pos);
        return new Vector2f(
                Math.round(pos.x / gridSnapSize) * gridSnapSize,
                Math.round(pos.y / gridSnapSize) * gridSnapSize);
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragMove dragMove) {
            var offset = new Vector2f(event.x - event.dragStartX, event.y - event.dragStartY);
            if (offset.lengthSquared() < 1f) {
                for (var model : dragMove.movables) {
                    var ele = modelElements.get(model);
                    if (ele != null && model instanceof IMovable movable) {
                        Style.importantPipeline(ele.getLayout(), l -> l.left(movable.getPosition().x).top(movable.getPosition().y));
                    }
                }
                return;
            }
            var localOffset = getContentViewContainer().getLocalMouseNormal(offset.x, offset.y);
            for (var model : dragMove.movables) {
                var ele = modelElements.get(model);
                if (ele != null && model instanceof IMovable movable) {
                    var newPos = snapPosition(localOffset.add(movable.getPosition(), new Vector2f()));
                    Style.importantPipeline(ele.getLayout(), l -> l.left(newPos.x).top(newPos.y));
                }
            }
        }
    }

    protected void onDragEnd(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragMove(var targetWasSelected, var target, var movables)) {
            var offset = new Vector2f(event.x - event.dragStartX, event.y - event.dragStartY);
            if (offset.lengthSquared() < 1f) {
                // too less drag, back to click
                batchSelection(() -> {
                    if (!event.isCtrlDown()) {
                        clearAllSelected();
                        addSelected(target);
                    } else if (targetWasSelected && event.isCtrlDown() && selected.size() > 1) {
                        removeSelected(target);
                    }
                });
                return;
            }
            var localOffset = getContentViewContainer().getLocalMouseNormal(offset.x, offset.y);
            dispatchCommand(new GraphCommands.MoveElementsCommand(new ArrayList<>(movables), localOffset));
        }
    }

    protected void onKeyDown(UIEvent event) {
        if (this.isFocused() || panelLayer.getChildren().stream().anyMatch(UIElement::isFocused)) {
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_DELETE -> deleteSelectedElements();
                default -> event.hasHandler = false;
            }
        } else {
            event.hasHandler = false;
        }
    }

    protected void onValidateCommand(UIEvent event) {
        if (
                CommandEvents.UNDO.equals(event.command) ||
                CommandEvents.REDO.equals(event.command) ||
                CommandEvents.COPY.equals(event.command) ||
                CommandEvents.CUT.equals(event.command) ||
                CommandEvents.DUPLICATE.equals(event.command) ||
                CommandEvents.PASTE.equals(event.command)
        ) {
            event.stopPropagation();
        }
    }


    protected void onExecuteCommand(UIEvent event) {
        if (CommandEvents.REDO.equals(event.command)) {
            historyStack.redo();
        } else if (CommandEvents.UNDO.equals(event.command)) {
            historyStack.undo();
        } else if (CommandEvents.COPY.equals(event.command)) {
            copySelectedElements();
        } else if (CommandEvents.CUT.equals(event.command)) {
            cutSelectedElements();
        } else if (CommandEvents.DUPLICATE.equals(event.command)) {
            duplicateSelectedElements();
        } else if (CommandEvents.PASTE.equals(event.command)) {
            pasteElements();
        }
    }

    protected void onGraphViewMouseDown(UIEvent event) {
        // re-implement it
        // drag with middle / right button
        if (graphView.getGraphViewStyle().allowPan()
                && (event.button == 1 || event.button == 2)
                && graphView.isSelfOrChildHover()
                && graphView.isMouseOverContent(event.x, event.y)) {
            graphView.startDrag(new com.lowdragmc.lowdraglib2.gui.ui.elements.GraphView.DragOffset(graphView.getOffsetX(), graphView.getOffsetY()), null);
        } else if (event.button == 0) {
            if (event.bubbleListeners.size() == 1) {
                // clear selection if click on empty space
                clearAllSelected();
                // start drag selection — transient drag-rect feedback, pinned via IMPORTANT.
                var selectionRect = new UIElement();
                selectionRect.addClass("__node-graph-view_drag-selection__");
                Style.importantPipeline(selectionRect.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                        .width(0)
                        .height(0));
                Style.importantPipeline(selectionRect.getStyle(), s -> s.background(new SDFRectTexture().setStroke(0.5f)
                        .setColor(ColorPattern.T_LIGHT_BLUE.color)
                        .setBorderColor(ColorPattern.LIGHT_BLUE.color)
                ));
                graphView.startDrag(new DragRegionSelection(selectionRect), null);
                graphView.addChild(selectionRect);
            }
        }
        event.stopLaterPropagation();
    }

    protected void onGraphViewMouseUp(UIEvent event) {
        var mui = getModularUI();
        if (event.button == 1 && mui != null) {
            // check if movement is smaller than 1 pixel
            if (new Vector2f(event.x, event.y).sub(mui.getLastMouseDownX(), mui.getLastMouseDownY()).lengthSquared() < 1f) {
                var menu = createMenu(event.x, event.y);
                if (menu.isEmpty()) return;
                isMenuOpen = true;
                var layoutOffset = mui.ui.rootElement.worldToLocalLayoutOffset(new Vector2f(event.x, event.y));
                var contextMenu = new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                        .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                        .setOnNodeClicked(TreeBuilder.Menu::handle)
                        .setOnClose(() -> isMenuOpen = false);
                contextMenu.addClass("__node-graph-view_context-menu__");
                Style.importantPipeline(contextMenu.getLayout(), l -> l.left(layoutOffset.x).top(layoutOffset.y));
                mui.ui.rootElement.addChild(contextMenu);
            }
        }
    }

    protected void onGraphViewDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.getDraggingObject() instanceof DragRegionSelection(var selectionRect)) {
            var minX = Math.min(event.dragStartX, event.x);
            var minY = Math.min(event.dragStartY, event.y);
            var localMouse = graphView.getLocalMouse(minX, minY);
            var width = Math.abs(event.dragStartX - event.x);
            var height = Math.abs(event.dragStartY - event.y);
            var localSize = graphView.getLocalMouseNormal(width, height);
            // Live drag-rect geometry — data-driven.
            float rectLeft = localMouse.x - graphView.getContentX();
            float rectTop = localMouse.y - graphView.getContentY();
            Style.importantPipeline(selectionRect.getLayout(), l -> l
                    .left(rectLeft)
                    .top(rectTop)
                    .width(localSize.x)
                    .height(localSize.y));
            var localGraphMouse = getContentViewContainer().getLocalMouse(minX, minY);
            var localGraphSize = getContentViewContainer().getLocalMouseNormal(width, height);
            dragRegionSelection = new Vector4f(localGraphMouse.x, localGraphMouse.y, localGraphSize.x, localGraphSize.y);
        }
    }

    protected void onGraphViewDragEnd(UIEvent event) {
        if (event.dragHandler.getDraggingObject() instanceof DragRegionSelection(var selectionRect)) {
            selectionRect.removeSelf();
            if (dragRegionSelection != null) {
                batchSelection(() -> {
                    // select all
                    for (var entry : modelElements.entrySet()) {
                        var model = entry.getKey();
                        var element = entry.getValue();
                        if (element.isSelectable() && element.canBeRegionSelected(dragRegionSelection)) {
                            addSelected(model);
                        }
                    }
                });
                this.dragRegionSelection = null;
            }
        }
    }

    protected void onGraphViewDragPerform(UIEvent event) {
        if (!(event.dragHandler.getDraggingObject() instanceof GraphResourceProviderContainer.DraggingGraph draggingGraph)
                || graph == null || !graph.graphModel.allowSubgraphCreation()
                || !graphView.isMouseOverContent(event.x, event.y)) {
            return;
        }

        // Reject self-import: the dragged resource is the same file this editor is currently
        // showing at its root. Anything looser (e.g. a parent file referenced from a child) would
        // require traversing the open editor topology, which is out of scope for v1.
        var editorView = getFirstAncestorOfType(GraphEditorView.class);
        if (editorView != null && editorView.getRootPath() != null
                && editorView.getRootPath().equals(draggingGraph.path())) {
            LDLib2.LOGGER.warn("Rejected subgraph import: cannot import a graph into itself ({}).",
                    draggingGraph.path());
            return;
        }

        // Cross-GraphResource imports are allowed only when the host graph opts in via
        // acceptsSubgraphGraph. GraphResource instances are singletons binding a node-class registry
        // and path scheme, so by default (acceptsSubgraphGraph == false) different resources stay
        // non-interchangeable; a graph that wants cross-type subgraphs overrides the method.
        var resolver = graph.graphModel.getReferenceResolver();
        var hostResource = resolver == null ? null : resolver.getSourceResource();
        if (hostResource != null && hostResource != draggingGraph.graphResource()) {
            var draggedGraph = draggingGraph.graphResource().createGraph();
            if (!graph.acceptsSubgraphGraph(draggedGraph)) {
                LDLib2.LOGGER.warn(
                        "Rejected subgraph import: host graph {} does not accept subgraph type {}.",
                        graph.getClass().getName(), draggedGraph.getClass().getName());
                return;
            }
        }

        // Validated — dispatch the actual import.
        var localPosition = snapPosition(
                getContentViewContainer().worldToLocalLayoutOffset(new Vector2f(event.x, event.y)));
        dispatchCommand(new ImportExternalSubgraphCommand(draggingGraph.path(), localPosition));
    }

    /**
     * Adds an "Add Subgraph" submenu listing every loaded {@link com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResource} whose graph type is
     * accepted by this host graph via {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph#acceptsSubgraphGraph}.
     * Selecting one creates an empty inline local subgraph of that foreign type plus a node bound to
     * it (via {@link CreateForeignLocalSubgraphCommand}). No-op when the host disallows subgraphs or
     * accepts no foreign types.
     */
    private void appendForeignSubgraphMenu(TreeBuilder.Menu menuBuilder, Vector2f localPosition) {
        if (graph == null || !graph.graphModel.allowSubgraphCreation()) return;
        var editor = getFirstAncestorOfType(com.lowdragmc.lowdraglib2.editor.ui.Editor.class);
        if (editor == null) return;

        // (display name, graph type), deduped by graph class.
        var seen = new java.util.HashSet<Class<?>>();
        var compatible = new java.util.ArrayList<com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResource<?>>();
        for (var entry : editor.resourceView.getResources().entrySet()) {
            if (!(entry.getKey() instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResource<?> resource)) continue;
            var sample = resource.createGraph();
            if (sample.getClass() == graph.getClass()) continue; // same-type handled elsewhere
            if (!graph.acceptsSubgraphGraph(sample)) continue;
            if (seen.add(sample.getClass())) compatible.add(resource);
        }
        if (compatible.isEmpty()) return;

        menuBuilder.branch("graph.commands.create_foreign_local_subgraph", branch -> {
            for (var resource : compatible) {
                Class<? extends com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph> type = resource.createGraph().getClass();
                var name = resource.getName();
                branch.leaf(resource.getDisplayName(), () ->
                        dispatchCommand(new CreateForeignLocalSubgraphCommand(type, name, localPosition)));
            }
        });
    }

    protected TreeBuilder.Menu createMenu(float mouseX, float mouseY) {
        var menuBuilder = TreeBuilder.Menu.start();
        // Newly-created elements align to the snap grid the same way drag-moved ones do, so the
        // canvas stays grid-consistent regardless of how the user adds content.
        var localPosition = snapPosition(
                getContentViewContainer().worldToLocalLayoutOffset(new Vector2f(mouseX, mouseY)));

        // "Add Node" is always available
        menuBuilder.leaf("graph.commands.add_node", () -> {
            itemLibrary.show(mouseX, mouseY, node -> {
                if (node instanceof NodeModelLibraryItem nodeItem) {
                    dispatchCommand(new NodeCommands.CreateNodeCommand().onGraph(nodeItem, localPosition, null));
                }
            });
        });

        // "Add Subgraph (<type>)" for each cross-type graph this graph accepts — creates an inline
        // local subgraph of that foreign type so the new node IS a subgraph of another graph type.
        appendForeignSubgraphMenu(menuBuilder, localPosition);

        // "Create Sticky Note" is always available
        menuBuilder.leaf(ContextualMenuHelpers.CREATE_STICKY_NOTE_ITEM.getName(), () ->
                dispatchCommand(new GraphCommands.CreateStickyNoteCommand(localPosition))
        );

        // "Create Placemat" is always available
        menuBuilder.leaf(ContextualMenuHelpers.CREATE_PLACEMAT_ITEM.getName(), () ->
                createPlacematFromSelection(localPosition)
        );

        if (getSelected().isEmpty()) return menuBuilder;

        // Collect selected GraphElementModels
        var selectedModels = getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .toList();
        if (selectedModels.isEmpty()) return menuBuilder;

        menuBuilder.crossLine();

        // Get common menu items (intersection) from all selected models, sorted by priority
        var commonItems = getCommonMenuItems(selectedModels);

        // Bind actions and build menu
        for (var item : commonItems) {
            var bound = bindMenuItemAction(item, selectedModels, localPosition);
            if (bound != null) {
                menuBuilder.leaf(bound.getName(), bound::execute);
            }
        }

        // Wire-specific items (only when all selected are wires)
        if (selectedModels.stream().allMatch(WireModel.class::isInstance)) {
            appendWireMenuItems(menuBuilder, selectedModels);
        }

        // Context/Block items
        appendContextBlockMenuItems(menuBuilder, selectedModels, mouseX, mouseY);

        return menuBuilder;
    }

    /**
     * Appends "Add Block" (when a single ContextNodeModel is selected) and
     * "Delete Block" / "Move Up" / "Move Down" (when only sibling BlockNodeModels are selected).
     */
    private void appendContextBlockMenuItems(com.lowdragmc.lowdraglib2.gui.util.TreeBuilder.Menu menuBuilder,
                                             List<GraphElementModel> selectedModels,
                                             float mouseX, float mouseY) {
        // Add Block: shown when the selection is a single context node with at least one
        // compatible block type. Opens the ItemLibrary in block-only mode targeting this context.
        if (selectedModels.size() == 1
                && selectedModels.get(0) instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.ContextNodeModel ctxModel) {
            var supported = ctxModel.getSupportBlockClasses();
            if (!supported.isEmpty()) {
                menuBuilder.leaf("graph.add_block", () ->
                        itemLibrary.showBlocksForContext(mouseX, mouseY, ctxModel, item -> {
                            if (item instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.BlockLibraryItem blockItem) {
                                dispatchCommand(new com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.BlockCommands.InsertBlockCommand(
                                        ctxModel, blockItem.getBlockClass(), -1));
                            }
                        }));
            }
        }

        // Block-only single-selection: offer Move Up / Move Down. (Standard "Delete" already
        // routes through removeBlock via GraphModel.removeElements, and the standard menu items
        // — Copy/Cut/Duplicate/Delete — are emitted by the common-items path above.)
        if (selectedModels.size() == 1
                && selectedModels.get(0) instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel block) {
            var parent = block.getContextNodeModel();
            if (parent != null) {
                int idx = parent.indexOf(block);
                if (idx > 0) {
                    menuBuilder.leaf("graph.move_block_up", () -> dispatchCommand(
                            new com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.BlockCommands.MoveBlockCommand(parent, idx, idx - 1)));
                }
                if (idx >= 0 && idx < parent.getBlockCount() - 1) {
                    menuBuilder.leaf("graph.move_block_down", () -> dispatchCommand(
                            new com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.BlockCommands.MoveBlockCommand(parent, idx, idx + 1)));
                }
            }
        }
    }

    /** Gets the intersection of menu items from all selected models, sorted by priority. */
    private List<ContextualMenuItem> getCommonMenuItems(List<GraphElementModel> models) {
        var first = new LinkedHashSet<>(models.get(0).getContextualMenuItems());
        for (int i = 1; i < models.size(); i++) {
            var names = models.get(i).getContextualMenuItems().stream()
                    .map(ContextualMenuItem::getName).collect(Collectors.toSet());
            first.removeIf(item -> !names.contains(item.getName()));
        }
        return first.stream().sorted(Comparator.comparingInt(ContextualMenuItem::getPriority)).toList();
    }

    /** Binds a runtime action to a menu item. Returns null if the item is not available. */
    private @Nullable ContextualMenuItem bindMenuItemAction(ContextualMenuItem item,
            List<GraphElementModel> selectedModels, Vector2f localPosition) {
        // Case strings match the translation keys stored in ContextualMenuHelpers — keep them in
        // sync if you rename either side.
        return switch (item.getName()) {
            case "graph.delete" -> {
                if (selectedModels.stream().allMatch(GraphElementModel::isDeletable))
                    yield item.withAction(this::deleteSelectedElements);
                yield null;
            }
            case "graph.frame_selection" -> item.withAction(this::fitGraphChildren);
            case "graph.cut" -> {
                if (selectedModels.stream().allMatch(m -> m.isDeletable() && m.isCopiable()))
                    yield item.withAction(this::cutSelectedElements);
                yield null;
            }
            case "graph.copy" -> {
                if (selectedModels.stream().allMatch(GraphElementModel::isCopiable))
                    yield item.withAction(this::copySelectedElements);
                yield null;
            }
            case "graph.paste" -> {
                if (clipboardData != null)
                    yield item.withAction(this::pasteElements);
                yield null;
            }
            case "graph.paste_as_new" -> null; // TODO
            case "graph.rename" -> {
                if (selectedModels.size() == 1) {
                    var only = selectedModels.get(0);
                    if (only.isRenamable() && only instanceof IHasName) {
                        yield item.withAction(() -> startInlineRenameFor(only));
                    }
                }
                yield null;
            }
            case "graph.duplicate" -> {
                if (selectedModels.stream().allMatch(GraphElementModel::isCopiable))
                    yield item.withAction(this::duplicateSelectedElements);
                yield null;
            }
            case "graph.color_picker" -> {
                if (selectedModels.size() == 1) {
                    var only = selectedModels.get(0);
                    if (only.isColorable() && only instanceof IHasElementColor colored) {
                        yield item.withAction(() -> openColorPopup(localPosition, only, colored));
                    }
                }
                yield null;
            }
            case "graph.create_placemat" -> item.withAction(() -> createPlacematFromSelection(localPosition));
            case "graph.create_subgraph_from_selection" -> {
                // Wires are tolerated (filtered inside the model); nodes need to be copiable;
                // placemats / sticky notes pass through. Final validation (e.g. placemat with
                // non-selected contained node) happens inside extractSelectionToLocalSubgraph.
                if (graph != null && graph.graphModel.allowSubgraphCreation()
                        && !selectedModels.isEmpty()
                        && selectedModels.stream().allMatch(m ->
                                m instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel
                                        || (m instanceof AbstractNodeModel an && an.isCopiable())
                                        || m instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.PlacematModel
                                        || m instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.wiget.StickyNoteModel)) {
                    var selection = new ArrayList<>(selectedModels);
                    yield item.withAction(() -> dispatchCommand(new CreateSubgraphFromSelectionCommand(selection)));
                }
                yield null;
            }
            case "graph.align_and_distribute" -> null; // TODO
            // Node-specific items
            case "graph.delete_and_reconnect" -> null; // TODO
            case "graph.edit_subtitle" -> null; // TODO
            case "graph.bypass_node" -> null; // TODO
            case "graph.disable_node" -> null; // TODO
            case "graph.disconnect_all_wires" -> item.withAction(() -> {
                var wiresToDelete = selectedModels.stream()
                        .filter(AbstractNodeModel.class::isInstance)
                        .map(AbstractNodeModel.class::cast)
                        .flatMap(node -> node.getConnectedWires().stream())
                        .distinct()
                        .map(GraphElementModel.class::cast)
                        .toList();
                if (!wiresToDelete.isEmpty()) {
                    dispatchCommand(new GraphCommands.DeleteElementsCommand(wiresToDelete));
                }
            });
            case "graph.toggle_collapse" -> {
                var nodes = selectedModels.stream()
                        .filter(AbstractNodeModel.class::isInstance)
                        .map(AbstractNodeModel.class::cast)
                        .filter(GraphElementModel::isCollapsible)
                        .toList();
                if (nodes.isEmpty()) yield null;
                // Target state mirrors the first node — collapse the whole batch when the first
                // is expanded, expand when the first is already collapsed.
                var target = !nodes.get(0).isCollapsed();
                yield item.withAction(() -> nodes.forEach(n -> n.setCollapsed(target)));
            }
            default -> {
                // If the item already has an action, use it directly
                if (item.getAction() != null) yield item;
                yield null;
            }
        };
    }

    /** Appends wire-specific menu items (e.g., Convert to Portals). */
    private void appendWireMenuItems(TreeBuilder.Menu menuBuilder, List<GraphElementModel> models) {
        menuBuilder.leaf("graph.commands.covert_wires_to_portals", () -> {
            var wires = new ArrayList<WireModel>();
            for (var model : models) {
                if (!(model instanceof WireModel wireModel)) return;
                var hasNullOrMissingPort = wireModel.getToPort() == null || wireModel.getFromPort() == null ||
                        wireModel.getToPort().getPortType() == PortType.MISSING_PORT ||
                        wireModel.getFromPort().getPortType() == PortType.MISSING_PORT;
                if (hasNullOrMissingPort) continue;
                wires.add(wireModel);
            }
            var wireData = WireElement.getPortalsWireData(wires, this);
            dispatchCommand(new WireCommands.ConvertWiresToPortalsCommand(wireData));
        });
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // lets update the graph elements here
        updateGraphModelChanges();
        if (requireFitGraph) {
            var size = getTaffyLayout().size();
            // make sure it's a valid graph view size
            if (size.width == 0 || size.height == 0) return;
            requireFitGraph = false;
            fitGraphChildren();
        }
    }

    protected void updateGraphModelChanges() {
        if (graph == null) return;
        var graphModel = graph.graphModel;
        var changes = graphModel.flushChanges();
        changeset.addNewModels(changes.getNewModels());
        changeset.addChangedModels(changes.getChangedModels());
        changeset.addDeletedModels(changes.getDeletedModels());
        var somethingChanged = changeset.hasChanges();
        if (somethingChanged) {
            var newPlacemats = new ArrayList<GraphElement<?>>();
            var changedModels = new HashMap<UUID, ChangeHintList>();

            // remove deleted elements
            deleteElementsFromChangeSet(changeset);

            // add new elements
            addElementsFromChangeSet(changeset, newPlacemats);

            //Update new and deleted node containers
            var allModels = new ArrayList<>(changeset.getNewModels());
            allModels.addAll(changeset.getDeletedModels());
            for (var uid : allModels) {
                if (graphModel.getModel(uid) instanceof GraphElementModel model && model.getContainer() instanceof GraphElementModel container) {
                    // Whatever change hint was there is superseded by Unspecified.
                    changedModels.put(container.getUid(), ChangeHintList.UNSPECIFIED);
                }
            }

            // notify changes
            for (var entry : changeset.getChangedModelsAndHints().entrySet()) {
                addChangedModel(changedModels, entry.getKey(), entry.getValue());
            }

            updateChangedModels(changedModels, newPlacemats);
            refreshGraphLogger();
        }

        changeset.clear();
    }

    protected void updateChangedModels(Map<UUID, ChangeHintList> changedModels,
//                                       SimpleChangeset selectionChangeset,
//                                       HashSet<UUID> selectionAlreadyUpdatedModels,
//                                       boolean shouldUpdatePlacematContainer,
                                       List<GraphElement<?>> placemats) {
        // update blackboard
        blackboard.updateModelChanges(changedModels);

        for (var entry : changedModels.entrySet()) {
            var uid = entry.getKey();
            var hints = entry.getValue();
            var element = getModelElement(uid);
//            bool inSelection = selectionChangeset?.ChangedModels.Contains(guid) ?? false;
//            if (inSelection)
//            {
//                selectionAlreadyUpdatedModels.Add(guid);
//            }
            ModelUpdateVisitor viewUpdater;
            if (hints == ChangeHintList.UNSPECIFIED) {
                viewUpdater = ModelUpdateVisitor.UNSPECIFIED;
            } else {
                viewUpdater = new ModelUpdateVisitor(hints);
            }

            if (element != null) {
                dispatchUpdate(element, viewUpdater);
//                if (inSelection) {
//                    UpdateSelectionVisitor.Visitor.Update(ui);
//                }
//                if (ui.parent == PlacematContainer)
//                    shouldUpdatePlacematContainer = true;
            }

            // ToList is needed to bake the dependencies.
            for (var ui : List.copyOf(getModelDependencies(uid))) {
                if (ui instanceof GraphElement<?> e) {
                    var h = changedModels.get(e.getModel().getUid());
                    if (h != null && h.isSupersetOf(hints)) continue;
                }
                dispatchUpdate(ui, viewUpdater);
            }
        }

//        if (shouldUpdatePlacematContainer)
//            PlacematContainer?.UpdateElementsOrder();

        for (var placemat : placemats) {
            dispatchUpdate(placemat, ModelUpdateVisitor.UNSPECIFIED);
        }
    }

    private void addChangedModel(HashMap<UUID, ChangeHintList> changedModels, UUID uid, ChangeHintList changeHints) {
        changedModels.merge(
                uid,
                changeHints,
                ChangeHintList::addRange
        );
    }

    protected void deleteElementsFromChangeSet(GraphChangeset modelChangeSet) {
        for (var uid : modelChangeSet.getDeletedModels()) {
            var element = modelElementsByID.get(uid);
            if (element != null) {
                removeElement(element);
            }

            // notify all tracking element to update
            for (var dependencyElement : List.copyOf(getModelDependencies(uid))) {
                dispatchUpdate(dependencyElement, ModelUpdateVisitor.UNSPECIFIED);
            }
        }
    }

    protected void addElementsFromChangeSet(GraphChangeset modelChangeSet, List<GraphElement<?>> newPlacemats) {
        var newModels = modelChangeSet.getNewModels().stream()
                .map(uid -> graph.graphModel.getModel(uid))
                .filter(Objects::nonNull).toList();

        for (var model : newModels) {
            if (!(model instanceof IGraphElementUIModel)) continue;
            if (model instanceof WireModel
                    || model instanceof PortModel
                    || model instanceof DeclarationModel
                    || model instanceof NodePreviewModel
                    || model instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.BlockNodeModel
            ) continue;

            if (model.getContainer() != graph.graphModel) continue;

            createAndAddModelElement(model);
        }

        for (var model : newModels) {
            if (model instanceof WireModel wireModel) {
                createWireUI(wireModel);
            }
        }

        for (var model : newModels) {
            if (model instanceof PlacematModel) {
                var element = getModelElement(model);
                if (element instanceof GraphElement<?> ge) {
                    newPlacemats.add(ge);
                }
            }
        }

        for (var model : newModels) {
            if (model instanceof NodePreviewModel previewModel) {
                // t.odo preview
//                addElement(previewModel);
            }
        }
    }

    /**
     * Triggers inline rename for {@code model} if its UI element supports it (NodeElement,
     * PlacematElement). Other element types currently have no inline edit affordance, so this
     * is a no-op for them — users can rename them via the inspector when single-selected.
     */
    public void startInlineRenameFor(com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel model) {
        var element = modelElements.get(model);
        if (element instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement nodeElement) {
            if (nodeElement.getNodeTittle() != null) {
                nodeElement.getNodeTittle().startInlineRename();
            }
        } else if (element instanceof com.lowdragmc.lowdraglib2.nodegraphtookit.gui.wiget.PlacematElement placematElement) {
            placematElement.startInlineRename();
        }
    }

    /**
     * Opens a small floating {@link com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector} at
     * {@code localPosition}. Color changes dispatch via {@code SetElementColorCommand} so they
     * land on the undo stack. Loses focus → closes (mirrors the menu lifecycle).
     */
    protected void openColorPopup(Vector2f localPosition,
                                  com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel target,
                                  com.lowdragmc.lowdraglib2.nodegraphtookit.model.IHasElementColor colored) {
        var mui = getModularUI();
        if (mui == null) return;

        var colorSelector = new com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector();
        colorSelector.addClass("__node-graph-view_color-popup__");
        colorSelector.addClass("panel_bg");
        Style.defaultPipeline(colorSelector.getStyle(), s -> s.backgroundTexture(Sprites.RECT_SOLID));
        Style.defaultPipeline(colorSelector.getLayout(), l -> l.positionType(TaffyPosition.ABSOLUTE)
                .width(150)
                .paddingAll(4));
        colorSelector.setFocusable(true);
        // Close on focus loss — same dismissal model the Menu uses.
        colorSelector.setEnforceFocus(e -> colorSelector.removeSelf());
        colorSelector.setColor(colored.getElementColor(), false);
        colorSelector.setOnColorChangeListener(newColor ->
                dispatchCommand(new ElementRenameColorCommands.SetElementColorCommand(target, newColor)));

        var worldPos = getContentViewContainer().localToWorld(localPosition);
        var rootOffset = mui.ui.rootElement.worldToLocalLayoutOffset(worldPos);
        // Popup position is data-driven by mouse — pin via IMPORTANT.
        Style.importantPipeline(colorSelector.getLayout(), l -> l.left(rootOffset.x).top(rootOffset.y));
        mui.ui.rootElement.addChild(colorSelector);
        colorSelector.focus();
    }

    protected boolean createWireUI(@Nullable WireModel wire) {
        if (wire == null || graph == null)
            return false;

        if (wire.getToPort() != null && wire.getFromPort() != null) {
            createAndAddModelElement(wire);
            return true;
        }

        var missingPorts = wire.addMissingPorts();

        var inputResult = missingPorts.left().result();
        var outputResult = missingPorts.right().result();
        var inputNode = missingPorts.left().nodeModel();
        var outputNode = missingPorts.right().nodeModel();

        if (inputResult == PortMigrationResult.MISSING_PORT_ADDED && inputNode != null) {
            var inputNodeUi = getModelElement(inputNode);
            if (inputNodeUi != null) {
                dispatchUpdate(inputNodeUi, ModelUpdateVisitor.UNSPECIFIED);
            }
        }

        if (outputResult == PortMigrationResult.MISSING_PORT_ADDED && outputNode != null) {
            var outputNodeUi = getModelElement(outputNode);
            if (outputNodeUi != null) {
                dispatchUpdate(outputNodeUi, ModelUpdateVisitor.UNSPECIFIED);
            }
        }

        if (inputResult != PortMigrationResult.MISSING_PORT_FAILURE &&
                outputResult != PortMigrationResult.MISSING_PORT_FAILURE) {
            createAndAddModelElement(wire);
            return true;
        }

        return false;
    }

    private void createPlacematFromSelection(Vector2f localPosition) {
        var movables = getSelected().stream()
                .filter(IMovable.class::isInstance)
                .map(IMovable.class::cast)
                .toList();
        if (movables.isEmpty()) {
            dispatchCommand(new GraphCommands.CreatePlacematCommand("Placemat", localPosition, new Vector2f(200, 150)));
        } else {

            float padding = 20f;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (var movable : movables) {
                var pos = movable.getPosition();
                var el = getModelElement((GraphElementModel) movable);
                float w = el != null ? el.getSizeWidth() : 0;
                float h = el != null ? el.getSizeHeight() : 0;
                minX = Math.min(minX, pos.x);
                minY = Math.min(minY, pos.y);
                maxX = Math.max(maxX, pos.x + w);
                maxY = Math.max(maxY, pos.y + h);
            }
            var placematPos = new Vector2f(minX - padding, minY - padding);
            var placematSize = new Vector2f(maxX - minX + padding * 2, maxY - minY + padding * 2);
            dispatchCommand(new GraphCommands.CreatePlacematCommand("Placemat", placematPos, placematSize));
        }
    }

    public void copySelectedElements() {
        if (graph == null) return;
        var selectedModels = getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .filter(GraphElementModel::isCopiable)
                .toList();
        if (selectedModels.isEmpty()) return;
        clipboardData = graph.graphModel.copyElements(selectedModels, Platform.getFrozenRegistry());
    }

    public void cutSelectedElements() {
        copySelectedElements();
        deleteSelectedElements();
    }

    public void pasteElements() {
        if (graph == null || clipboardData == null) return;
        dispatchCommand(new GraphCommands.PasteElementsCommand(clipboardData, new Vector2f(50, 50)));
    }

    public void duplicateSelectedElements() {
        if (graph == null) return;
        var selectedModels = getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .filter(GraphElementModel::isCopiable)
                .toList();
        if (selectedModels.isEmpty()) return;
        var data = graph.graphModel.copyElements(selectedModels, Platform.getFrozenRegistry());
        dispatchCommand(new GraphCommands.PasteElementsCommand(data, new Vector2f(50, 50)));
    }

    public void deleteSelectedElements() {
        deleteSelectedElements(Predicates.alwaysTrue());
    }

    /**
     * Deletes the selected elements in the graph that match the provided {@code filter}.
     * This method filters the selected elements based on their type, deletable status,
     * and the given condition before dispatching a delete command.
     *
     * @param filter a {@link Predicate} that defines the condition to determine which
     *               {@link GraphElementModel} elements should be deleted.
     */
    public void deleteSelectedElements(Predicate<GraphElementModel> filter) {
        dispatchCommand(new GraphCommands.DeleteElementsCommand(getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .filter(GraphElementModel::isDeletable)
                .filter(filter)
                .toList()));
    }
}
