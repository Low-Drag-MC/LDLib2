package com.lowdragmc.lowdraglib2.nodegraphtookit.gui;

import com.google.common.collect.Sets;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortType;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.blackboard.Blackboard;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.GraphCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.IGraphCommand;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.NodeCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.command.WireCommands;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ElementUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibrary;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.*;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.graph.GraphModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodePlaceholder;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodePreviewModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.PortMigrationResult;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WireModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.wire.WirePlaceHolder;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

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

    // runtime
    @Getter
    private GraphChangeset changeset = new GraphChangeset();
    @Getter
    private final UIElement panelLayer = new UIElement();
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


    public GraphView() {
        this.graphView.getLayout().widthPercent(100).heightPercent(100);
        this.panelLayer.getLayout().positionType(TaffyPosition.ABSOLUTE).width(0).height(0);

        // header initial
        header.layout(layout -> {
            layout.widthPercent(100);
            layout.height(16);
            layout.paddingAll(1);
            layout.flexDirection(FlexDirection.ROW);
        });
        header.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        initHeaders();

        // canvas
        canvas.getLayout().widthPercent(100).flex(1);

        graphView.addEventListener(UIEvents.MOUSE_DOWN, this::onGraphViewMouseDown);
        graphView.addEventListener(UIEvents.MOUSE_UP, this::onGraphViewMouseUp);
        graphView.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onGraphViewDragSourceUpdate);
        graphView.addEventListener(UIEvents.DRAG_END, this::onGraphViewDragEnd);
        fallbackLayer.setId("fallback-layer");
        fallbackLayer.setAllowHitTest(false);
        fallbackLayer.getLayout().positionType(TaffyPosition.ABSOLUTE);
        graphView.addContentChild(fallbackLayer);
        setLayers(List.of(WireElement.WIRE_LAYER, NodeElement.NODE_LAYER));
        addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onDragSourceUpdate);
        addEventListener(UIEvents.DRAG_END, this::onDragEnd);
        addEventListener(UIEvents.KEY_DOWN, this::onKeyDown);
        setEnforceFocus(Consumers.nop());

        itemLibrary.setDisplay(false);
        initPanels();

        addChildren(header, canvas.addChildren(graphView, panelLayer, itemLibrary));
    }

    protected void initHeaders() {
        header.addChildren(
                // left
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.heightPercent(100);
                    layout.flex(1);
                }).addChildren(
                ),
                // center
                new UIElement().layout(layout -> layout.heightPercent(100)),
                // right
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                    layout.heightPercent(100);
                    layout.flex(1);
                }).addChildren(
                        // page fit button
                        new Button().noText().setOnClick(event -> fitGraphChildren(15))
                                .layout(layout -> layout.width(14))
                                .style(style -> style.tooltips("GraphView.fit")).addChild(
                                        new UIElement().layout(layout -> {
                                            layout.heightPercent(100);
                                            layout.setAspectRatio(1);
                                        }).style(style -> style.backgroundTexture(Icons.PAGE_FIT)))
                )
        );
    }

    protected void initPanels() {
        panelLayer.addChildren(
                new GraphPanel(this, blackboard)
        );
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
            layer.setId(layerName);
            layer.setAllowHitTest(false);
            layer.getLayout().positionType(TaffyPosition.ABSOLUTE);
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
        return this;
    }

    public void clearGraph() {
        this.graph = null;
        this.modelElements.clear();
        this.modelElementsByID.clear();
        this.selected.clear();
        this.layers.values().forEach(UIElement::clearAllChildren);
        this.isWireDragging = false;
    }

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

        for (var nodeModel : graphModel.getNodeModels()) {
            var nodeUI = createAndAddModelElement(nodeModel);
            if (nodeUI != null) {
                var previewModel = nodeModel.getNodePreviewModel();
                if (previewModel != null) {
                    createAndAddModelElement(previewModel);
                }
            }
        }

        // todo sticky note

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
        command.execute(this, graph.graphModel);
        return true;
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
        if (element.isSelectable()) {
            element.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if ((event.button == 0 || event.button == 1) && event.bubbleListeners.size() == 1) {
                    var tagetWasSelected = isSelected(model);
                    // select node
                    if (!event.isCtrlDown() && !isSelected(model)) {
                        clearAllSelected();
                    }
                    addSelected(model);
                    moveElementTop(element);
                    // drag movable
                    var movables = selected.stream().filter(m -> m instanceof IMovable).toList();
                    if (movables.isEmpty()) return;
                    var width = 12;
                    var height = 12;
                    startDrag(new DragMove(tagetWasSelected, model, movables), Icons.MOVE).setDragTexture(- width / 2f, -height / 2f, width, height);
                }
            });
        }
        if (model instanceof IMovable movable) {
            // position
            element.getLayout().positionType(TaffyPosition.ABSOLUTE).left(movable.getPosition().x).top(movable.getPosition().y);
        }
        return true;
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

    public void addSelected(Model model) {
        if (!modelElements.containsKey(model)) return;
        selected.add(model);
        var element = modelElements.get(model);
        if (element != null) element.onSelectionChanged();
    }

    public void removeSelected(Model model) {
        selected.remove(model);
        var element = modelElements.get(model);
        if (element != null) element.onSelectionChanged();
    }

    public void clearAllSelected() {
        var selectedNodes = Sets.newHashSet(selected);
        selected.clear();
        selectedNodes.forEach(node -> {
            var element = modelElements.get(node);
            if (element != null) element.onSelectionChanged();
        });
    }

    public boolean isSelected(Model nodeModel) {
        return selected.contains(nodeModel);
    }

    @Override
    public boolean isSelfOrChildHover() {
        return !isMenuOpen && super.isSelfOrChildHover();
    }

    protected void onDragSourceUpdate(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragMove dragMove) {
            var offset = new Vector2f(event.x - event.dragStartX, event.y - event.dragStartY);
            if (offset.lengthSquared() < 1f) {
                for (var model : dragMove.movables) {
                    var ele = modelElements.get(model);
                    if (ele != null && model instanceof IMovable movable) {
                        ele.getLayout().left(movable.getPosition().x).top(movable.getPosition().y);
                    }
                }
                return;
            }
            var localOffset = getContentViewContainer().getLocalMouseNormal(offset.x, offset.y);
            for (var model : dragMove.movables) {
                var ele = modelElements.get(model);
                if (ele != null && model instanceof IMovable movable) {
                    var newPos = localOffset.add(movable.getPosition(), new Vector2f());
                    ele.getLayout().left(newPos.x).top(newPos.y);
                }
            }
        }
    }

    protected void onDragEnd(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof DragMove(var targetWasSelected, var target, var movables)) {
            var offset = new Vector2f(event.x - event.dragStartX, event.y - event.dragStartY);
            if (offset.lengthSquared() < 1f) {
                // too less drag, back to click
                if (!event.isCtrlDown()) {
                    clearAllSelected();
                    addSelected(target);
                } else if (targetWasSelected && event.isCtrlDown() && selected.size() > 1) {
                    removeSelected(target);
                }
                return;
            }
            var localOffset = getContentViewContainer().getLocalMouseNormal(offset.x, offset.y);
            for (var model : movables) {
                var ele = modelElements.get(model);
                if (ele != null && model instanceof IMovable movable) {
                    var newPos = localOffset.add(movable.getPosition(), new Vector2f());
                    ele.getLayout().left(newPos.x).top(newPos.y);
                    movable.setPosition(newPos);
                }
            }
        }
    }

    protected void onKeyDown(UIEvent event) {
        if (!this.isFocused()) return;
        switch (event.keyCode) {
            case GLFW.GLFW_KEY_DELETE -> {
                deleteSelectedElements();
                break;
            }
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
                // start drag selection
                var selectionRect = new UIElement();
                selectionRect.getLayout().positionType(TaffyPosition.ABSOLUTE)
                        .width(0)
                        .height(0);
                selectionRect.getStyle().background(new SDFRectTexture().setStroke(0.5f)
                        .setColor(ColorPattern.T_LIGHT_BLUE.color)
                        .setBorderColor(ColorPattern.LIGHT_BLUE.color)
                );
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
                mui.ui.rootElement.addChildren(new Menu<>(menu.build(), TreeBuilder.Menu::uiProvider)
                        .setHoverTextureProvider(TreeBuilder.Menu::hoverTextureProvider)
                        .setOnNodeClicked(TreeBuilder.Menu::handle)
                        .setOnClose(() -> isMenuOpen = false)
                        .layout(layout -> {
                            layout.left(event.x - mui.ui.rootElement.getPositionX());
                            layout.top(event.y - mui.ui.rootElement.getContentY());
                        }));
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
            selectionRect.getLayout()
                    .left(localMouse.x - graphView.getContentX())
                    .top(localMouse.y - graphView.getContentY())
                    .width(localSize.x)
                    .height(localSize.y);
            var localGraphMouse = getContentViewContainer().getLocalMouse(minX, minY);
            var localGraphSize = getContentViewContainer().getLocalMouseNormal(width, height);
            dragRegionSelection = new Vector4f(localGraphMouse.x, localGraphMouse.y, localGraphSize.x, localGraphSize.y);
        }
    }

    protected void onGraphViewDragEnd(UIEvent event) {
        if (event.dragHandler.getDraggingObject() instanceof DragRegionSelection(var selectionRect)) {
            selectionRect.removeSelf();
            if (dragRegionSelection != null) {
                // select all
                for (var entry : modelElements.entrySet()) {
                    var model = entry.getKey();
                    var element = entry.getValue();
                    if (element.isSelectable() && element.canBeRegionSelected(dragRegionSelection)) {
                        addSelected(model);
                    }
                }
                this.dragRegionSelection = null;
            }
        }
    }

    protected TreeBuilder.Menu createMenu(float mouseX, float mouseY) {
        var menuBuilder= TreeBuilder.Menu.start();
        var localPosition = getContentViewContainer().getLocalMouse(mouseX, mouseY);
        menuBuilder.leaf("graph.commands.add_node", () -> {
            itemLibrary.show(mouseX, mouseY, node -> {
                if (node instanceof NodeModelLibraryItem nodeItem) {
                    dispatchCommand(new NodeCommands.CreateNodeCommand().onGraph(nodeItem, localPosition, null));
                }
            });
        });
        if (!getSelected().isEmpty() && getSelected().stream().allMatch(m -> m instanceof GraphElementModel gem && gem.isDeletable())) {
            menuBuilder.leaf("graph.commands.delete", this::deleteSelectedElements);
        }
        if (!getSelected().isEmpty() && getSelected().stream().allMatch(e -> e instanceof WireModel)) {
            menuBuilder.leaf("graph.commands.covert_wires_to_portals", () -> {
                var wires = new ArrayList<WireModel>();
                var hasNullOrMissingPort = false;
                for (var model : getSelected().stream().toList()) {
                    // If the graph element is not a wire, don't append this menu item.
                    if (!(model instanceof WireModel wireModel)) return;

                    // If the wire has a missing port, do not allow creation of portals.
                    hasNullOrMissingPort = wireModel.getToPort() == null || wireModel.getFromPort() == null ||
                            wireModel.getToPort().getPortType() == PortType.MISSING_PORT ||
                            wireModel.getFromPort().getPortType() == PortType.MISSING_PORT;
                    if (hasNullOrMissingPort) continue;
                    wires.add(wireModel);
                }

                var wireData = WireElement.getPortalsWireData(wires, this);

                dispatchCommand(new WireCommands.ConvertWiresToPortalsCommand(wireData));
            });
        }
        return menuBuilder;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // lets update the graph elements here
        updateGraphModelChanges();
    }

    protected void updateGraphModelChanges() {
        if (graph == null) return;
        var graphModel = graph.graphModel;
        var changes = graphModel.getCurrentGraphChangeDescription();
        var somethingChanged = changeset.addNewModels(changes.getNewModels());
        somethingChanged |= changeset.addChangedModels(changes.getChangedModels());
        somethingChanged |= changeset.addDeletedModels(changes.getDeletedModels());
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
        }

        changes.clear();
        changeset.clear();
    }

    protected void updateChangedModels(Map<UUID, ChangeHintList> changedModels,
//                                       SimpleChangeset selectionChangeset,
//                                       HashSet<UUID> selectionAlreadyUpdatedModels,
//                                       boolean shouldUpdatePlacematContainer,
                                       List<GraphElement<?>> placemats) {
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
//                    || model instanceof PlacematModel
                    || model instanceof DeclarationModel
                    || model instanceof NodePreviewModel
//                    || model instanceof GroupModelBase
            ) continue;

            if (model.getContainer() != graph.graphModel) continue;

            createAndAddModelElement(model);
        }

        for (var model : newModels) {
            if (model instanceof WireModel wireModel) {
                createWireUI(wireModel);
            }
        }

        // todo Placemat

        for (var model : newModels) {
            if (model instanceof NodePreviewModel previewModel) {
                // t.odo preview
//                addElement(previewModel);
            }
        }
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

    protected void deleteSelectedElements() {
        dispatchCommand(new GraphCommands.DeleteElementsCommand(getSelected().stream()
                .filter(GraphElementModel.class::isInstance)
                .map(GraphElementModel.class::cast)
                .filter(GraphElementModel::isDeletable).toList()));
    }
}
