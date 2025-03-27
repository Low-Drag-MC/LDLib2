package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.IConfiguratorContainer;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Layout;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Rect;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

@Getter
public class NodeWidget extends WidgetGroup {
    private final BaseNode node;
    private final GraphViewWidget graphView;
    protected WidgetGroup title;
    protected WidgetGroup ports;
    protected WidgetGroup content;
    protected final Map<NodePort, NodePortWidget> portMap = new HashMap<>();
    // runtime
    protected boolean isDragging = false;
    protected double lastMouseX, lastMouseY;
    @Setter
    protected Position lastPosition;
    protected long lastClickTime = 0;

    public NodeWidget(GraphViewWidget graphView, BaseNode node) {
        super(node.position, Size.of(node.getMinWidth(), 30));
        this.graphView = graphView;
        this.node = node;
        addWidget(title = new WidgetGroup(0, 0, node.getMinWidth(), 15));
        addWidget(ports = new WidgetGroup(0, 15, node.getMinWidth(), 15));
        ports.setDynamicSized(true);
        ports.setLayout(Layout.HORIZONTAL_TOP);
        addWidget(content = new WidgetGroup(0, 30, node.getMinWidth(), 0));
        reloadWidget();
    }

    public void reloadWidget() {
        var width = 0;
        var height = 15;
        portMap.clear();
        title.clearAllWidgets();
        ports.clearAllWidgets();
        content.clearAllWidgets();

        if (node.isExpanded()) {
            width = Math.max(node.getMinWidth(), Minecraft.getInstance().font.width(node.getDisplayName()) + 10);
            // load inputs
            var inputGroup = new WidgetGroup(0, 0, 0, 0);
            inputGroup.setBackground(ColorPattern.GRAY.rectTexture());
            var portHeight = Math.max(this.node.getInputPorts().size(), this.node.getOutputPorts().size()) * 15;
            var inputWidth = 0;
            for (var port : this.node.getInputPorts()) {
                var portWidget = new NodePortWidget(this, true, port);
                inputWidth = Math.max(inputWidth, portWidget.getSizeWidth());
                inputGroup.addWidget(portWidget);
                portMap.put(port, portWidget);
            }
            // load outputs
            var outputGroup = new WidgetGroup(0, 0, 0, 0);
            outputGroup.setBackground(ColorPattern.DARK_GRAY.rectTexture());
            var outputWidth = 0;
            for (var port : this.node.getOutputPorts()) {
                var portWidget = new NodePortWidget(this, false, port);
                outputWidth = Math.max(outputWidth, portWidget.getSizeWidth());
                outputGroup.addWidget(portWidget);
                portMap.put(port, portWidget);
            }
            width = Math.max(width, inputWidth + outputWidth);
            var inputPercent = inputWidth * 1f / (inputWidth + outputWidth);
            inputGroup.setSize((int) (inputPercent * width), portHeight);
            outputGroup.setSize(width - (int) (inputPercent * width), portHeight);
            inputGroup.setLayout(Layout.VERTICAL_LEFT);
            outputGroup.setLayout(Layout.VERTICAL_RIGHT);
            ports.addWidget(inputGroup);
            ports.addWidget(outputGroup);
            ports.setVisible(true);
            height += ports.getSizeHeight();

            // title
            title.setBackground(new ColorRectTexture(node.getTitleColor()).setTopRadius(5));
            title.setSize(width, 15);
            title.addWidget(new ImageWidget(5, 2, width - 10, 11,
                    new TextTexture(node.getDisplayName()).setWidth(width - 10)
                            .setType(TextTexture.TextType.LEFT)));
            title.addWidget(new ImageWidget(0, -11, width, 1,
                    new TextTexture(() -> graphView.isShowDebugInfo() ? "compute order: %d".formatted(node.getComputeOrder()) : "")
                            .setColor(ColorPattern.BRIGHT_RED.color)));

            // split line
            title.addWidget(new ImageWidget(0, 14, width, 1, ColorPattern.BLACK.rectTexture()));

            // content
            ConfiguratorGroup group = new ConfiguratorGroup("", false);
            node.buildConfigurator(group);

            if (group.getConfigurators().isEmpty()) {
                // if there is no configurator
                content.setVisible(false);
                if (!inputGroup.widgets.isEmpty()) {
                    if (outputGroup.widgets.isEmpty())
                        inputGroup.setBackground(ColorPattern.GRAY.rectTexture().setBottomRadius(5));
                    else
                        inputGroup.setBackground(ColorPattern.GRAY.rectTexture().setRadiusLB(5));
                }
                if (!outputGroup.widgets.isEmpty()) {
                    if (inputGroup.widgets.isEmpty())
                        outputGroup.setBackground(ColorPattern.DARK_GRAY.rectTexture().setBottomRadius(5));
                    else
                        outputGroup.setBackground(ColorPattern.DARK_GRAY.rectTexture().setRadiusRB(5));
                }
                setSize(width, height);
            } else {
                IConfiguratorContainer computeLayout = getConfiguratorContainer(height, width, group);
                for (Configurator configurator : group.getConfigurators()) {
                    configurator.setConfiguratorContainer(computeLayout);
                    configurator.init(width - 4);
                    content.addWidget(configurator);
                }
                computeLayout.computeLayout();
            }
        } else {
            width = Minecraft.getInstance().font.width(node.getDisplayName()) + 10;
            CollapsePortWidget inputPort = null;
            for (var port : this.node.getInputPorts()) {
                if (inputPort == null) {
                    inputPort = new CollapsePortWidget(this, port, true);
                }
                portMap.put(port, inputPort);
            }
            CollapsePortWidget outputPort = null;
            for (var port : this.node.getOutputPorts()) {
                if (outputPort == null) {
                    outputPort = new CollapsePortWidget(this, port, false);
                }
                portMap.put(port, outputPort);
            }
            var textWidth = width - 10;
            width += inputPort == null ? 0 : 10;
            width += outputPort == null ? 0 : 10;
            // title
            if (inputPort != null) {
                title.addWidget(inputPort);
                inputPort.setSelfPosition(0, 0);
            }
            if (outputPort != null) {
                title.addWidget(outputPort);
                outputPort.setSelfPosition(width - 15, 0);
            }
            title.setBackground(new ColorRectTexture(node.getTitleColor()).setRadius(5));
            title.setSize(width, 15);
            title.addWidget(new ImageWidget(inputPort == null ? 5 : 15, 2, textWidth, 11,
                    new TextTexture(node.getDisplayName()).setWidth(textWidth)
                            .setType(TextTexture.TextType.LEFT)));
            title.addWidget(new ImageWidget(0, -11, width, 1,
                    new TextTexture(() -> graphView.isShowDebugInfo() ? "compute order: %d".formatted(node.getComputeOrder()) : "")
                            .setColor(ColorPattern.BRIGHT_RED.color)));
            content.setVisible(false);
            ports.setVisible(false);
            setSize(width, height);
        }
    }

    @NotNull
    private IConfiguratorContainer getConfiguratorContainer(int height, int width, ConfiguratorGroup group) {
        return () -> {
            int configHeight = 1;
            for (Configurator configurator : group.getConfigurators()) {
                configurator.computeHeight();
                configurator.setSelfPosition(Position.of(2, configHeight));
                configHeight += configurator.getSize().height;
            }
            content.addWidget(new ImageWidget(0, 0, width, 1, ColorPattern.BLACK.rectTexture()));
            content.setVisible(true);
            content.setSelfPosition(Position.of(0, height));
            content.setSize(width, configHeight);
            content.setBackground(new ColorRectTexture(ColorPattern.DARK_GRAY.color).setBottomRadius(5));
            setSize(width, height + configHeight);
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var radius = new Vector4f(5, 5, 5, 5);
        if (isMouseOverElement(mouseX, mouseY) || graphView.getSelectedNodes().contains(node)) {
            DrawerHelper.drawFrameRoundBox(graphics, Rect.ofRelative(getPositionX(), getSizeWidth(), getPositionY(), getSizeHeight()),
                    1, radius, radius, ColorPattern.BLUE.color);
        }
        if (graphView.isRunStep() && graphView.getStepNode() == node) {
            DrawerHelper.drawFrameRoundBox(graphics, Rect.ofRelative(getPositionX() - 3, getSizeWidth() + 6,
                            getPositionY() - 3, getSizeHeight() + 6),
                    2, radius, radius, ColorPattern.generateRainbowColor(gui.getTickCount()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (button == 0 && title.isMouseOverElement(mouseX, mouseY) && gui != null) {
            var clickTime = gui.getTickCount();
            if (lastClickTime != 0 && clickTime - lastClickTime < 10) { // double click
                node.setExpanded(!node.isExpanded());
                reloadWidget();
                return true;
            }
            lastClickTime = clickTime;
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            isDragging = true;
            lastPosition = getSelfPosition();
            for (var selectedNode : graphView.getSelectedNodes()) {
                var widget = graphView.getNodeMap().get(selectedNode);
                widget.setLastPosition(widget.getSelfPosition());
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            setSelfPosition(lastPosition.add((int) (mouseX - lastMouseX), (int) (mouseY - lastMouseY)));
            node.setPosition(getSelfPosition());
            for (var selectedNode : graphView.getSelectedNodes()) {
                var widget = graphView.getNodeMap().get(selectedNode);
                widget.setSelfPosition(widget.getLastPosition().add((int) (mouseX - lastMouseX), (int) (mouseY - lastMouseY)));
                selectedNode.setPosition(widget.getSelfPosition());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        if (isMouseOverElement(mouseX, mouseY) && button == 0) {
            if (super.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
            if (!isShiftDown() && !isCtrlDown()) {
                graphView.getSelectedNodes().clear();
            }
            graphView.getSelectedNodes().add(node);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
