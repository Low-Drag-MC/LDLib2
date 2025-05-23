package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.registry.AutoRegistry;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.math.Position;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Getter
public class NodePanelWidget extends DraggablePanelWidget {
    private final GraphViewWidget graphView;
    // runtime
    private DraggableScrollableWidgetGroup nodeListView;
    private String filter = "";
    @Nullable
    private String selectedNode;
    private long lastClickTime;
    private Map<String, NodeGroupWidget> nodeGroupWidgets = new HashMap<>();

    public NodePanelWidget(GraphViewWidget graphView, int x, int y, int width, int height) {
        super("graph_processor.node_panel", x, y, width, height);
        this.graphView = graphView;
    }

    @Override
    protected void loadWidgets() {
        super.loadWidgets();
        // node list
        nodeListView = new DraggableScrollableWidgetGroup(3, 12, content.getSizeWidth() - 6, content.getSizeHeight() - 15);
        nodeListView.setYScrollBarWidth(2).setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1));

        var groupContainer = new WidgetGroup(Position.ORIGIN);
        groupContainer.setLayout(Layout.VERTICAL_LEFT);
        groupContainer.setLayoutPadding(2);
        for (var entry : graphView.getNodeGroups().entrySet()) {
            var nodeGroupWidget = new NodeGroupWidget(entry.getKey(), entry.getValue(), nodeListView.getSizeWidth() - 3);
            nodeGroupWidgets.put(entry.getKey(), nodeGroupWidget);
            groupContainer.addWidget(nodeGroupWidget);
        }
        nodeListView.addWidget(groupContainer);
        content.addWidget(nodeListView);

        // search bar
        content.addWidget(new LabelWidget(2, 2, "Search:"));
        var length = Minecraft.getInstance().font.width("Search:");
        content.addWidget(new ImageWidget(5 + length, 2, content.getSizeWidth() - 8- length, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        content.addWidget(new TextFieldWidget(8 + length, 2, content.getSizeWidth() - 8 - length, 10, () -> filter, s -> {
            filter = s;
            nodeGroupWidgets.values().forEach(NodeGroupWidget::reloadNodes);
            for (NodeGroupWidget groupWidget : nodeGroupWidgets.values()) {
                if (!groupWidget.nodesGroup.widgets.isEmpty()) {
                    groupWidget.collapse(false);
                }
            }
        }).setCurrentString(filter).setBordered(false).setClientSideWidget());
    }

    private class NodeGroupWidget extends WidgetGroup {
        private final WidgetGroup nodesGroup;
        private final ImageWidget splitLine;
        private final List<AutoRegistry.Holder<LDLRegister, BaseNode, Supplier<BaseNode>>> nodes;

        public NodeGroupWidget(String groupName, List<AutoRegistry.Holder<LDLRegister, BaseNode, Supplier<BaseNode>>> nodes, int width) {
            super(0, 0, width, 0);
            this.nodes = nodes;
            nodesGroup = new WidgetGroup(Position.of(2, 16));
            nodesGroup.setLayout(Layout.VERTICAL_LEFT);
            setBackground(ColorPattern.WHITE.borderTexture(-1));
            addWidget(splitLine = new ImageWidget(2, 2, width - 4, 11, ColorPattern.LIGHT_GRAY.rectTexture()));
            addWidget(new ButtonWidget(2, 2, width - 4, 11, new TextTexture(groupName)
                    .setType(TextTexture.TextType.ROLL).setWidth(width - 4), b -> collapse(!isCollapsed())));
            addWidget(nodesGroup);
            reloadNodes();
            collapse(true);
        }

        public boolean isCollapsed() {
            return !splitLine.isVisible();
        }

        public void collapse(boolean collapse) {
            if (isCollapsed() == collapse) return;
            if (collapse) {
                splitLine.setVisible(false);
                splitLine.setActive(false);
                nodesGroup.setVisible(false);
                nodesGroup.setActive(false);
                setSize(getSizeWidth(), 15);
            } else {
                splitLine.setVisible(true);
                splitLine.setActive(true);
                nodesGroup.setVisible(true);
                nodesGroup.setActive(true);
                setSize(getSizeWidth(), 15 + nodesGroup.getSizeHeight() + 2);
            }
        }

        public void reloadNodes() {
            var width = getSizeWidth();
            nodesGroup.clearAllWidgets();
            for (var node : nodes) {
                if (node.annotation().name().contains(filter) || node.annotation().group().contains(filter)) {
                    var buttonGroup = new WidgetGroup(0, 0, width - 4, 10);
                    buttonGroup.addWidget(new ButtonWidget(0, 0, width - 4, 10,
                            new TextTexture(node.annotation().name())
                                    .setType(TextTexture.TextType.LEFT_HIDE).setWidth(width - 4),
                            b -> {
                                if (gui == null) return;
                                if (!node.annotation().name().equals(selectedNode)) {
                                    selectedNode = node.annotation().name();
                                    lastClickTime = gui.getTickCount();
                                } else {
                                    var currentClickTime = gui.getTickCount();
                                    if (currentClickTime - lastClickTime < 10) {
                                        graphView.addNodeToCenter(node.value().get());
                                        lastClickTime = currentClickTime - 10;
                                    } else {
                                        lastClickTime = currentClickTime;
                                    }
                                }
                            }));
                    buttonGroup.addWidget(new ImageWidget(0, 0, width - 4, 10, () ->
                            node.annotation().name().equals(selectedNode) ? ColorPattern.T_GRAY.rectTexture() : IGuiTexture.EMPTY)
                            .setDraggingProvider(() -> node.value().get(), (n, pos) -> new TextTexture(n.getDisplayName())));
                    nodesGroup.addWidget(buttonGroup);
                }
            }
            if (nodesGroup.widgets.isEmpty()) {
                setSize(getSizeWidth(), 0);
                this.setVisible(false);
                this.setActive(false);
            } else {
                if (isCollapsed()) {
                    setSize(getSizeWidth(), 15);
                } else {
                    setSize(getSizeWidth(), 15 + nodesGroup.getSizeHeight() + 2);
                }
                this.setVisible(true);
                this.setActive(true);
            }
        }

    }

}
