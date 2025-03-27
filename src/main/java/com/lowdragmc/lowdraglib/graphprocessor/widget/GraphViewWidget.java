package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.utils.AnnotationDetector;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.PortEdge;
import com.lowdragmc.lowdraglib.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib.graphprocessor.data.parameter.ParameterNode;
import com.lowdragmc.lowdraglib.graphprocessor.processor.BaseGraphProcessor;
import com.lowdragmc.lowdraglib.graphprocessor.processor.TriggerProcessor;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.lowdragmc.lowdraglib.gui.widget.DialogWidget;
import com.lowdragmc.lowdraglib.gui.widget.FreeGraphView;
import com.lowdragmc.lowdraglib.gui.widget.MenuWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Rect;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@Getter
public class GraphViewWidget extends WidgetGroup {
    private final BaseGraph graph;
    private final FreeGraphView freeGraphView;
    private final Map<BaseNode, NodeWidget> nodeMap = new HashMap<>();
    private final DebugPanelWidget debugPanel;
    private final NodePanelWidget nodePanel;
    private final ParameterPanelWidget parameterPanel;
    @Setter
    private NodePortWidget clickedPort;
    @Setter
    @Nullable
    private BaseGraphProcessor processor;
    @Getter
    private Map<String, List<AnnotationDetector.Wrapper<LDLRegister, ? extends BaseNode>>> nodeGroups = new LinkedHashMap<>();
    // runtime
    @Setter
    private boolean showDebugInfo = Platform.isDevEnv();
    private boolean isRunStep = false;
    private Iterator<BaseNode> stepIterator;
    @Nullable
    private BaseNode stepNode;
    private final Set<BaseNode> selectedNodes = new HashSet<>();
    private final Set<BaseNode> copiedNodes = new HashSet<>();
    private boolean isDraggingArea = false;
    private double startMouseX, startMouseY;
    private int currentMouseX, currentMouseY;
    private long lastClickTick;

    public GraphViewWidget(BaseGraph graph, int x, int y, int width, int height) {
        this(graph, x, y, width, height, null);
    }

    public GraphViewWidget(BaseGraph graph, int x, int y, int width, int height, @Nullable Consumer<List<String>> additionalGroup) {
        super(x, y, width, height);
        this.graph = graph;
        addWidget(this.freeGraphView = new FreeGraphView(0, 0, width, height) {
            @Override
            @OnlyIn(Dist.CLIENT)
            protected void drawWidgetsBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
                drawGraphLines(graphics, mouseX, mouseY, partialTicks);
                super.drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
            }
        });
        this.freeGraphView.setDrawGrid(true);
        this.freeGraphView.setBackground(ColorPattern.SEAL_BLACK.rectTexture());
        this.processor = new TriggerProcessor(graph);
        var supportNodeGroups = new ArrayList<String>();
        setupNodeGroups(supportNodeGroups);
        if (additionalGroup != null) {
            additionalGroup.accept(supportNodeGroups);
        }
        for (String group : supportNodeGroups) {
            for (var wrapper : AnnotationDetector.REGISTER_GP_NODES.values()) {
                if (wrapper.annotation().group().startsWith(group)) {
                    nodeGroups.computeIfAbsent(wrapper.annotation().group(), k -> new ArrayList<>()).add(wrapper);
                }
            }
        }
        loadGraph();
        // panels
        addWidget(this.debugPanel = new DebugPanelWidget(this));
        this.debugPanel.setSelfPosition(getSizeWidth() - this.debugPanel.getSizeWidth() - 5,
                getSizeHeight() - this.debugPanel.getSizeHeight() - 5);
        addWidget(this.nodePanel = new NodePanelWidget(this, 5, 5, 120, getSizeHeight() - 10));
        this.nodePanel.setDraggable(false);
        addWidget(this.parameterPanel = new ParameterPanelWidget(this, 130, 5, 200, 200));
        // node dragging
        this.setDraggingConsumer(
                o -> o instanceof BaseNode,
                o -> {},
                o -> {},
                o -> {
                    if (o instanceof BaseNode baseNode) {
                        addNode(baseNode, currentMouseX, currentMouseY);
                    }
                });
        freeGraphView.resetFitScaleByWidgets();
    }

    protected void setupNodeGroups(List<String> supportNodeGroups) {
        supportNodeGroups.add("graph_processor.node.value");
        supportNodeGroups.add("graph_processor.node.logic");
        supportNodeGroups.add("graph_processor.node.math");
        supportNodeGroups.add("graph_processor.node.utils");
        supportNodeGroups.add("graph_processor.node.minecraft");
    }

    public void loadGraph() {
        freeGraphView.clearAllWidgets();
        nodeMap.clear();
        selectedNodes.clear();
        for (var node : graph.nodes) {
            var nodeWidget = new NodeWidget(this, node);
            freeGraphView.addWidget(nodeWidget);
            nodeMap.put(node, nodeWidget);
        }
        updateComputeOrder();
    }

    public void addEdge(NodePort inputPort, NodePort outputPort) {
        graph.connect(inputPort, outputPort);
        updateComputeOrder();
    }

    public void removeEdge(NodePort port) {
        var edges = new ArrayList<>(port.getEdges());
        for (var edge : edges) {
            graph.disconnect(edge);
        }
        updateComputeOrder();
    }

    public void updateComputeOrder() {
        graph.updateComputeOrder(BaseGraph.ComputeOrderType.DepthFirst);
        stepIterator = null;
        stepNode = null;
        isRunStep = false;
        if (processor != null) {
            processor.updateComputeOrder();
        }
    }

    public void runAll() {
        if (isRunStep) {
            if (stepIterator != null) {
                while (stepIterator.hasNext()) {
                    stepNode = stepIterator.next();
                }
            }
            stepIterator = null;
            stepNode = null;
        }
        isRunStep = false;
    }

    public void runStep() {
        if (!isRunStep && processor != null) {
            stepIterator = processor.iterator();
        }
        isRunStep = true;
        if (stepIterator.hasNext()) {
            stepNode = stepIterator.next();
        } else if (processor != null) {
            stepNode = null;
            stepIterator = processor.iterator();
        }
    }

    public boolean isRunStepFinish() {
        return isRunStep && stepNode == null;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (processor != null && !isRunStep) {
            processor.run();
        }
    }


    public void setCopy(Collection<BaseNode> nodes) {
        copiedNodes.clear();
        copiedNodes.addAll(nodes);
    }

    public void pasteTo(double mouseX, double mouseY) {
        if (copiedNodes.isEmpty()) return;
        var nodes = new ArrayList<>(copiedNodes);
        Position firstPosition = null;
        Map<BaseNode, BaseNode> old2New = new HashMap<>();
        List<PortEdge> edges = new ArrayList<>();

        for (var node : nodes) {
            if (firstPosition == null) {
                firstPosition = node.position;
            }
            var newNode = node.copy();
            var newPosition = freeGraphView.getViewPosition(mouseX, mouseY);
            var offset = node.position.subtract(firstPosition);
            newNode.position = Position.of((int) newPosition.x + offset.x, (int) newPosition.y + offset.y);
            graph.addNode(newNode);
            var nodeWidget = new NodeWidget(this, newNode);
            freeGraphView.addWidget(nodeWidget);
            nodeMap.put(newNode, nodeWidget);
            old2New.put(node, newNode);
            // if the edge ports are connected to the copied nodes, then copy the edge
            for (var edge : node.getAllEdges()) {
                if (copiedNodes.contains(edge.inputNode) && copiedNodes.contains(edge.outputNode) && !edges.contains(edge)) {
                    edges.add(edge);
                }
            }
        }

        // make sure we have the same order
        edges.sort(Comparator.comparingInt(graph.edges::indexOf));

        // copy the edges
        for (var edge : edges) {
            var copiedEdge = edge.copy();
            copiedEdge.GUID = graph.newGUID().toString();
            copiedEdge.inputNodeGUID = old2New.get(edge.inputNode).getGUID();
            copiedEdge.outputNodeGUID = old2New.get(edge.outputNode).getGUID();
            copiedEdge.initialize(graph);
            graph.edges.add(copiedEdge);
            graph.edgesPerGUID.put(copiedEdge.GUID, copiedEdge);
            graph.addGUID(copiedEdge.GUID);

            // Sanity check for the edge:
            if (copiedEdge.inputPort == null || copiedEdge.outputPort == null) {
                graph.disconnect(copiedEdge.GUID);
                continue;
            }

            // Add the edge to the non-serialized port data
            copiedEdge.inputPort.owner.onEdgeConnected(copiedEdge);
            copiedEdge.outputPort.owner.onEdgeConnected(copiedEdge);
            nodeMap.get(copiedEdge.inputPort.owner).reloadWidget();
            nodeMap.get(copiedEdge.outputPort.owner).reloadWidget();
        }
        updateComputeOrder();
    }

    public void renameNode(BaseNode node) {
        DialogWidget.showStringEditorDialog(this, "ldlib.gui.editor.tips.rename", node.getDisplayName(),
                s -> true, s -> {
                    if (s != null) {
                        node.setDisplayName(s);
                        nodeMap.get(node).reloadWidget();
                    }
                });
    }

    public void addNode(BaseNode node) {
        graph.addNode(node);
        var nodeWidget = new NodeWidget(this, node);
        freeGraphView.addWidget(nodeWidget);
        nodeMap.put(node, nodeWidget);
        updateComputeOrder();
    }

    public void addNode(BaseNode node, int x, int y) {
        var position = freeGraphView.getViewPosition(x, y);
        node.position = Position.of(position.x, position.y);
        addNode(node);
    }

    public void addNodeToCenter(BaseNode node) {
        if(node instanceof ParameterNode parameterNode && parameterNode.parameter != null &&
                parameterNode.parameter.getAccessor() == ExposedParameter.ParameterAccessor.Set) {
            if (graph.nodes.stream().anyMatch(n -> n instanceof ParameterNode pn && pn.parameter == parameterNode.parameter)) {
                return;
            }
        }
        addNode(node, getSizeWidth() / 2 + LDLib.RANDOM.nextInt(-20, 20),
                getSizeHeight() / 2 + LDLib.RANDOM.nextInt(-20, 20));
    }

    public void removeNodes(Collection<BaseNode> removeNodes) {
        var nodes = new ArrayList<>(removeNodes);
        for (var node : nodes) {
            if (!node.isCanBeRemoved()) continue;
            var edges = node.getAllEdges();
            edges.forEach(graph::disconnect);
            graph.removeNode(node);
            var nodeWidget = nodeMap.remove(node);
            freeGraphView.removeWidget(nodeWidget);
            edges.forEach(edge -> {
                if (edge.inputNode == node) {
                    nodeMap.get(edge.outputNode).reloadWidget();
                } else {
                    nodeMap.get(edge.inputNode).reloadWidget();
                }
            });
        }
        copiedNodes.removeAll(removeNodes);
        updateComputeOrder();
    }

    public void openNodePanelDialog() {
        // TODO open node panel dialog ?
    }

    public <T, C> MenuWidget<T, C> openMenu(double posX, double posY, TreeNode<T, C> menuNode) {
        var menu = new MenuWidget<>((int) posX - parent.getPositionX(), (int) posY - parent.getPositionY(), 14, menuNode)
                .setNodeTexture(MenuWidget.NODE_TEXTURE)
                .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE);
        parent.waitToAdded(menu.setBackground(MenuWidget.BACKGROUND));
        return menu;
    }

    public void openMenu(double posX, double posY, TreeBuilder.Menu menuBuilder) {
        if (menuBuilder == null) return;
        openMenu(posX, posY, menuBuilder.build())
                .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                .setKeyNameSupplier(TreeBuilder.Menu::getName)
                .setOnNodeClicked(TreeBuilder.Menu::handle);
    }

    protected TreeBuilder.Menu createMenu(double mouseX, double mouseY) {
        var menu =  TreeBuilder.Menu.start()
                .branch(Icons.ADD, "add nodes", m -> nodeGroups.forEach((group, wrappers) -> m.branch(group, n -> {
                    for (var wrapper : wrappers) {
                        n.leaf(wrapper.annotation().name(), () -> addNode(wrapper.creator().get(), (int)mouseX, (int)mouseY));
                    }
                })));
        if (!selectedNodes.isEmpty()) {
            menu.crossLine();
            if (selectedNodes.size() == 1) {
                menu.leaf("ldlib.gui.editor.menu.rename", () -> renameNode(selectedNodes.iterator().next()));
            }
            menu.leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> setCopy(selectedNodes));
            if (!copiedNodes.isEmpty()) {
                menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", () -> pasteTo(mouseX, mouseY));
            }
            var canBeRemoved = selectedNodes.stream().filter(BaseNode::isCanBeRemoved).toList();
            if (!canBeRemoved.isEmpty()) {
                menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                    removeNodes(canBeRemoved);
                    selectedNodes.clear();
                });
            }
        }
        return menu;
    }

    @Override
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        currentMouseX = mouseX;
        currentMouseY = mouseY;
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        // draw selection area
        if (isDraggingArea) {
            var x = (int) Math.min(startMouseX, mouseX);
            var y = (int) Math.min(startMouseY, mouseY);
            var w = (int) Math.abs(mouseX - startMouseX);
            var h = (int) Math.abs(mouseY - startMouseY);
            DrawerHelper.drawBorder(graphics, x, y, w, h, ColorPattern.BLUE.color, 1);
            DrawerHelper.drawSolidRect(graphics, x, y, w, h, ColorPattern.T_LIGHT_BLUE.color);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawGraphLines(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // existing edges
        for (var edge : graph.edges) {
            var outputPort = nodeMap.get(edge.outputNode).portMap.get(edge.outputPort);
            var inputPort = nodeMap.get(edge.inputNode).portMap.get(edge.inputPort);
            if (inputPort == null) {
                LDLib.LOGGER.error("Edge {} input port {} are not existing: ", edge, edge.inputPort);
                continue;
            }
            if (outputPort == null) {
                LDLib.LOGGER.error("Edge {} output port {} are not existing: ", edge, edge.outputPort);
                continue;
            }
            var outputPosition = outputPort.getPortPosition();
            var inputPosition = inputPort.getPortPosition();
            drawEdge(graphics, outputPosition.vec2(), inputPosition.vec2(), outputPort.getPortColor(), inputPort.getPortColor());
        }
        // dragging edge
        if (clickedPort != null) {
            var outputPosition = clickedPort.isInput ? new Vec2(mouseX, mouseY) : clickedPort.getPortPosition().vec2();
            var inputPosition = clickedPort.isInput ? clickedPort.getPortPosition().vec2() : new Vec2(mouseX, mouseY);
            var inputColor = clickedPort.isInput ? clickedPort.getPortColor() : ColorPattern.BLUE.color;
            var outputColor = clickedPort.isInput ? ColorPattern.BLUE.color : clickedPort.getPortColor();
            drawEdge(graphics, outputPosition, inputPosition, outputColor, inputColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawEdge(GuiGraphics graphics, Vec2 outputPosition, Vec2 inputPosition, int startColor, int endColor) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        var outputController = outputPosition.add(new Vec2(20, 0));
        var inputController = inputPosition.add(new Vec2(-20, 0));
        RenderBufferUtils.drawColorLines(graphics.pose(), buffer, List.of(outputPosition, outputController), startColor, startColor, 0.75f);
        RenderBufferUtils.drawColorLines(graphics.pose(), buffer, List.of(outputController, inputController), startColor, endColor, 0.75f);
        RenderBufferUtils.drawColorLines(graphics.pose(), buffer, List.of(inputController, inputPosition), endColor, endColor, 0.75f);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!selectedNodes.isEmpty() && Screen.isCopy(keyCode)) {
            setCopy(selectedNodes);
            return true;
        }
        if (!copiedNodes.isEmpty() && Screen.isPaste(keyCode)) {
            pasteTo(currentMouseX, currentMouseY);
            return true;
        }
        if (Screen.isSelectAll(keyCode)) {
            selectedNodes.clear();
            selectedNodes.addAll(nodeMap.keySet());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 1) {
                isDraggingArea = false;
                startMouseX = mouseX;
                startMouseY = mouseY;
                return true;
            } else {
                if (!isShiftDown() && !isCtrlDown()) {
                    selectedNodes.clear();
                }
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (lastClickTick != 0 && gui.getTickCount() - lastClickTick < 10) {
                    openNodePanelDialog();
                    return true;
                }
                lastClickTick = gui.getTickCount();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var result = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        isDraggingArea = button == 1 || isDraggingArea;
        return result;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var result = super.mouseReleased(mouseX, mouseY, button);
        clickedPort = null;
        if (!result) {
            if (button == 1) {
                if (isDraggingArea) {
                    var start = freeGraphView.getViewPosition(startMouseX, startMouseY);
                    var end = freeGraphView.getViewPosition(mouseX, mouseY);
                    var area = Rect.ofAbsolute((int) Math.min(start.x, end.x), (int) Math.max(end.x, start.x),
                            (int) Math.min(start.y, end.y), (int) Math.max(end.y, start.y));
                    selectedNodes.clear();
                    for (NodeWidget widget : nodeMap.values()) {
                        if (area.isCollide(widget.getRect())) {
                            selectedNodes.add(widget.getNode());
                        }
                    }
                    if (selectedNodes.isEmpty()) {
                        openMenu(mouseX, mouseY, createMenu(mouseX, mouseY));
                    }
                } else {
                    openMenu(mouseX, mouseY, createMenu(mouseX, mouseY));
                }
            }
        }
        isDraggingArea = false;
        return result;
    }
}
