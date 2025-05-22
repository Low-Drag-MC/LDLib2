package com.lowdragmc.lowdraglib.graphprocessor.widget;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseGraph;
import com.lowdragmc.lowdraglib.graphprocessor.data.NodePort;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.TriggerLink;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@Getter
public class NodePortWidget extends Widget {
    public final NodeWidget nodeWidget;
    public final boolean isInput;
    public final NodePort port;

    public NodePortWidget(NodeWidget nodeWidget, boolean isInput, NodePort port) {
        super(Position.ORIGIN, Size.ZERO);
        this.nodeWidget = nodeWidget;
        this.isInput = isInput;
        this.port = port;
        initPortInformation();
    }

    public void initPortInformation() {
        // size is defined by the display name;
        var width = 18;
        if (LDLib.isClient()) {
            width = 18 + Minecraft.getInstance().font.width(port.portData.displayName);
        }
        setSize(width, 15);
        // setup hover tips
        var tooltips = new ArrayList<String>();
        tooltips.add("Type: %s".formatted(getPortTypeName()));
        if (port.portData.tooltip != null && !port.portData.tooltip.isEmpty()) {
            tooltips.addAll(port.portData.tooltip);
        }
        setHoverTooltips(tooltips.toArray(new String[0]));
    }

    public String getDisplayName() {
        return port.portData.displayName;
    }

    public String getPortTypeName() {
        return TypeAdapter.getTypeDisplayName(port.portData.displayType);
    }

    public int getPortColor() {
        var type = port.portData.displayType;
        if (type != null) {
            return TypeAdapter.getTypeColor(type);
        }
        return ColorPattern.BLUE.color;
    }

    public Position getPortPosition() {
        return isInput ? getPosition().add(7, 7) : getPosition().add(getSizeWidth() - 7, 7);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var isConnecting = !port.getEdges().isEmpty();
        var isTriggerLink = port.portData.displayType == TriggerLink.class;
        var icon = isConnecting ? Icons.RADIOBOX_MARKED : Icons.RADIOBOX_BLANK;
        if (isTriggerLink) {
            icon = isConnecting ? Icons.CHECKBOX_MARKED : Icons.CHECKBOX_BLANK;
        }
        var color = getPortColor();
        var isHover = isMouseOverElement(mouseX, mouseY);
        var clickedPort = nodeWidget.getGraphView().getClickedPort();
        if (clickedPort == null) {
            color = isHover ? ColorPattern.GREEN.color : color;
        } else if (isHover && clickedPort != this && clickedPort.isInput != isInput){
            var outputPort = clickedPort.isInput ? port : clickedPort.port;
            var inputPort = clickedPort.isInput ? clickedPort.port :port;
            if (BaseGraph.areTypesConnectable(outputPort.portData.displayType, inputPort.portData.displayType)) {
                color = ColorPattern.GREEN.color;
            }
        }
        if (isInput) {
            // draw icon left
            icon.copy().setColor(color).draw(graphics, mouseX, mouseY, getPositionX() + 4, getPositionY() + 3, 8, 8);
            // draw text right
            graphics.drawString(Minecraft.getInstance().font, getDisplayName(), getPositionX() + 15, getPositionY() + 3, -1);
        } else {
            // draw text left
            graphics.drawString(Minecraft.getInstance().font, getDisplayName(), getPositionX() + 3, getPositionY() + 3, -1);
            // draw icon right
            icon.copy().setColor(color).draw(graphics, mouseX, mouseY, getPositionX() + getSizeWidth() - 11, getPositionY() + 3, 8, 8);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 0 && nodeWidget.getGraphView().getClickedPort() == null) {
                nodeWidget.getGraphView().setClickedPort(this);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var clickedPort = nodeWidget.getGraphView().getClickedPort();
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 0 && clickedPort != null && clickedPort != this && clickedPort.isInput != isInput) {
                var outputPort = clickedPort.isInput ? port : clickedPort.port;
                var inputPort = clickedPort.isInput ? clickedPort.port :port;
                if (BaseGraph.areTypesConnectable(outputPort.portData.displayType, inputPort.portData.displayType)) {
                    nodeWidget.getGraphView().addEdge(inputPort, outputPort);
                    // reload the widgets
                    nodeWidget.reloadWidget();
                    clickedPort.getNodeWidget().reloadWidget();
                }
            } else  if (button == 1) {
                var updatedNodes = port.getEdges().stream()
                        .map(edge -> edge.inputNode == nodeWidget.getNode() ? edge.outputNode : edge.inputNode)
                        .map(node -> nodeWidget.getGraphView().getNodeMap().get(node)).toList();

                nodeWidget.getGraphView().removeEdge(port);
                nodeWidget.reloadWidget();
                updatedNodes.forEach(NodeWidget::reloadWidget);
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

}
