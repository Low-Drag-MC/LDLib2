package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.port.PortOrientation;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

/**
 * Demo node for verifying vertical ports. It declares:
 * <ul>
 *   <li>two <b>vertical</b> input ports — rendered in the row above the title,</li>
 *   <li>two <b>vertical</b> output ports — rendered in the row below the body,</li>
 *   <li>one horizontal input + one horizontal output — rendered in the usual side columns,
 *       so both layouts are visible on a single node for comparison.</li>
 * </ul>
 */
@NodeAttribute(name = "test_vertical", group = "test", graphTypes = {TestGraph.class})
public class TestVerticalNode extends Node {

    @Override
    public Component getDisplayName() {
        return Component.literal("Vertical Ports");
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);

        // Vertical inputs -> top row (above title)
        context.addInputPort("v_in1", Float.class).withOrientation(PortOrientation.Vertical).build();
        context.addInputPort("v_in2", Float.class).withOrientation(PortOrientation.Vertical).build();

        // Vertical outputs -> bottom row (below body)
        context.addOutputPort("v_out1", Float.class).withOrientation(PortOrientation.Vertical).build();
        context.addOutputPort("v_out2", Float.class).withOrientation(PortOrientation.Vertical).build();

        // Horizontal in/out -> usual side columns, for visual comparison
        context.addInputPort("h_in", Float.class).build();
        context.addOutputPort("h_out", Float.class).build();
    }
}
