package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * A port rendered vertically: the connector (wire dot above type-icon + label) stacks above the
 * optional constant editor. Laid out in a row by {@link VerticalPortContainer} so a set of vertical
 * ports forms a horizontal strip (e.g. inputs above the node title, outputs below the body).
 */
public class VerticalPortElement extends PortElement {

    public VerticalPortElement(PortModel portModel) {
        super(portModel);
        addClass("__vertical-port__");
    }

    @Override
    protected PortConnectorWithIconElement createConnector() {
        return new VerticalPortConnectorElement(getModel());
    }

    @Override
    protected void buildUI() {
        super.buildUI();
        // PortElement defaults to a ROW (connector | constant); stack them in a column instead, and
        // flex-grow so a row of vertical ports splits the available width evenly.
        Style.importantPipeline(getLayout(), l -> l.flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.CENTER)
                .flexGrow(1)
                .flexBasis(0));
    }
}
