package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.PortModel;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;

/**
 * {@link PortContainer} that lays its {@link VerticalPortElement}s out in a horizontal strip
 * (a row of vertical ports), rather than the default vertical stack of horizontal ports.
 */
public class VerticalPortContainer extends PortContainer {

    public VerticalPortContainer() {
        addClass("__vertical-port-container_inner__");
        // Default PortContainer stacks children vertically; vertical ports stack horizontally.
        Style.defaultPipeline(getLayout(), l -> l.flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.FLEX_START));
    }

    @Override
    protected PortElement createPortElement(PortModel port) {
        return new VerticalPortElement(port);
    }
}
