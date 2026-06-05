package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.DynamicTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortConnectorUI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortModelOptions;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.ModelElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.dependency.ModelUpdateVisitor;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.ChangeHint;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModelImpl;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import lombok.Getter;
import net.minecraft.network.chat.Component;

import java.util.stream.Stream;

public class PortConnectorElement extends ModelElement {
    public final PortModel portModel;
    // runtime
    @Getter
    protected UIElement connectorIcon;
    @Getter
    protected Label name;
    @Getter
    protected boolean willConnect;
    protected IGuiTexture lastIcon = IGuiTexture.EMPTY;

    public PortConnectorElement(PortModel portModel) {
        this.portModel = portModel;
        addClass("__port-connector__");
    }

    public Stream<? extends UIElement> getWireDragParts() {
        return Stream.of(connectorIcon);
    }

    @Override
    protected void buildUI() {
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));

        connectorIcon = new UIElement().addClass("__port-connector_icon__");
        Style.defaultPipeline(connectorIcon.getLayout(), l -> l.aspectRatio(1).width(9));

        name = new Label();
        name.addClass("__port-connector_label__");
        Style.defaultPipeline(name.getTextStyle(), s -> s.adaptiveWidth(true));

        addChildren(connectorIcon, name);
    }

    /**
     * Whether the port will be connected during an edge drag if the mouse is released where it is.
     */
    public void setWillConnect(boolean willConnect) {
        if (willConnect == this.willConnect) return;
        this.willConnect = willConnect;
        updateConnector();
    }

    @Override
    public void updateUIFromModel(ModelUpdateVisitor visitor) {
        // update connector icon
        if (visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY) || visitor.hasHint(ChangeHint.DATA)) {
            updateConnector();
        }
        if (visitor.hasHint(ChangeHint.STYLE) || visitor.hasHint(ChangeHint.DATA) || visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY)) {
            // update title and tooltips
            name.setText(portModel.getDisplayName());
            Style.importantPipeline(getStyle(), s -> s.tooltips(portModel.getTooltips()));
            // Hide label when its text is empty — data-driven.
            var empty = Component.empty().equals(name.getValue());
            Style.importantPipeline(name.getLayout(), l -> l.display(empty ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
            // update color — icon color is data (typed-port color) so pin via IMPORTANT.
            var icon = lastIcon.copy().setColor(portModel.getDataTypeHandle().getTypeColor());
            Style.importantPipeline(connectorIcon.getStyle(), s -> s.background(DynamicTexture.of(() -> {
                if (isActive()) return icon;
                else return icon.copy().setColor(ColorPattern.GRAY.color);
            })));
        }
    }

    protected void updateConnector() {
        if (portModel.getOptions().hasFlag(PortModelOptions.NODE_OPTION)) {
            // Hide the connector icon for node-option-style ports — data-driven.
            Style.importantPipeline(connectorIcon.getLayout(), l -> l.display(TaffyDisplay.NONE));
        } else {
            Style.importantPipeline(connectorIcon.getLayout(), l -> l.display(TaffyDisplay.FLEX));
            var connectorUI = portModel instanceof PortModelImpl impl ? impl.getConnectorUI() : PortConnectorUI.DEFAULT;
            lastIcon = connectorUI.getIcon(portModel.isConnected() || isWillConnect());
        }
    }
}
