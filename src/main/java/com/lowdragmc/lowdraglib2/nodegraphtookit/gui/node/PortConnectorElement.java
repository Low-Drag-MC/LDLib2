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
import org.jetbrains.annotations.Nullable;

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
    /** What {@link #updateConnector} last wrote to the style, so it can skip writing the same thing. */
    @Nullable
    private PortConnectorUI appliedConnectorUI;
    private boolean appliedConnected;
    private int appliedColor;
    private boolean applied;
    /** Whether the icon is currently laid out as hidden; null until it has been written once. */
    @Nullable
    private Boolean appliedHidden;

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
        // Unconditional, not gated on GRAPH_TOPOLOGY/DATA. A port's connection state is derived from
        // the graph's wire index rather than owned by this element, and it can move without this
        // element being handed a hint that says so — a wire bound during load, a node whose ports
        // were rebuilt, an update that arrives with only STYLE because that is what the model that
        // triggered it changed. When that happened the icon used to stick on whatever it was last
        // computed as, which is how a fully wired port ended up drawing the unconnected pin. Asking
        // the model every time is one index lookup, and updateConnector() writes no style unless
        // what it would draw actually changed.
        updateConnector();
        if (visitor.hasHint(ChangeHint.STYLE) || visitor.hasHint(ChangeHint.DATA) || visitor.hasHint(ChangeHint.GRAPH_TOPOLOGY)) {
            // update title and tooltips
            name.setText(portModel.getDisplayName());
            Style.importantPipeline(getStyle(), s -> s.tooltips(portModel.getTooltips()));
            // Hide label when its text is empty — data-driven.
            var empty = Component.empty().equals(name.getValue());
            Style.importantPipeline(name.getLayout(), l -> l.display(empty ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        }
    }

    /**
     * Bring the connector pin in line with the model: which icon (connected vs not) and what colour.
     *
     * <p>Applies the background itself rather than leaving it to {@link #updateUIFromModel}, so
     * {@link #setWillConnect} shows up on screen too. Idempotent — a call that would draw what is
     * already drawn returns without touching the style pipeline.</p>
     */
    protected void updateConnector() {
        // Hide the connector icon for node-option-style ports — data-driven. Written only when it
        // changes: display goes through the style pipeline and dirties the layout tree, and this
        // runs on every update now, including the layout-only ones a drag produces.
        var hidden = portModel.getOptions().hasFlag(PortModelOptions.NODE_OPTION);
        if (appliedHidden == null || appliedHidden != hidden) {
            appliedHidden = hidden;
            Style.importantPipeline(connectorIcon.getLayout(),
                    l -> l.display(hidden ? TaffyDisplay.NONE : TaffyDisplay.FLEX));
        }
        if (hidden) return;

        var connectorUI = portModel.getConnectorUI();
        var connected = portModel.isConnected() || isWillConnect();
        var color = portModel.getDataTypeHandle().getTypeColor();
        lastIcon = connectorUI.getIcon(connected);
        if (applied && connected == appliedConnected && color == appliedColor
                && connectorUI.equals(appliedConnectorUI)) {
            return;
        }
        applied = true;
        appliedConnectorUI = connectorUI;
        appliedConnected = connected;
        appliedColor = color;
        // update color — icon color is data (typed-port color) so pin via IMPORTANT.
        var icon = lastIcon.copy().setColor(color);
        Style.importantPipeline(connectorIcon.getStyle(), s -> s.background(DynamicTexture.of(() -> {
            if (isActive()) return icon;
            else return icon.copy().setColor(ColorPattern.GRAY.color);
        })));
    }
}
