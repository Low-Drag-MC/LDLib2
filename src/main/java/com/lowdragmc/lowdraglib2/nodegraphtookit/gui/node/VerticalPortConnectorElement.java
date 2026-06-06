package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortDirection;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * Vertical variant of {@link PortConnectorWithIconElement}: instead of a single row
 * {@code [dot, type-icon, label]}, the wire dot sits on top and the type-icon + label are grouped
 * into a row underneath it ({@code [dot] / [type-icon, label]}). Used by {@link VerticalPortElement}
 * for ports rendered above/below the node body.
 */
public class VerticalPortConnectorElement extends PortConnectorWithIconElement {

    public VerticalPortConnectorElement(PortModel portModel) {
        super(portModel);
        addClass("__vertical-port-connector__");
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        // Re-stack into a column. For an INPUT (top row) the wire dot sits on top with the
        // type-icon + label below it; for an OUTPUT (bottom row) the dot must sit at the BOTTOM
        // (closest to where the wire leaves), so reverse the column. The children are
        // [connectorIcon, labelRow] — COLUMN puts the dot first (top), COLUMN_REVERSE last (bottom).
        var isOutput = portModel.getDirection() == PortDirection.OUTPUT;
        Style.importantPipeline(getLayout(), l -> l.flexDirection(isOutput ? FlexDirection.COLUMN_REVERSE : FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER)
                .gapAll(1));

        var labelRow = new UIElement().addClass("__vertical-port-connector_label__");
        Style.defaultPipeline(labelRow.getLayout(), l -> l.flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .gapAll(2));
        // portIcon + name were added as direct children by the super build; move them into the row.
        removeChild(portIcon);
        removeChild(name);
        labelRow.addChildren(portIcon, name);
        addChild(labelRow);
    }
}
