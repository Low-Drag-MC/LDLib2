package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

/**
 * A node whose option adds and removes a port: on, it has a {@code Vector3f} input with an inline editor.
 */
@NodeAttribute(name = "toggle_port_test", group = "test", graphTypes = {TestGraph.class})
public class TogglePortTestNode extends Node {
    public static final String OPTION = "with_vector";
    public static final String PORT = "vector";

    @Override
    public Component getDisplayName() {
        return Component.literal("Toggle Port");
    }

    @Override
    public void onDefineOptions(IOptionDefinitionContext context) {
        super.onDefineOptions(context);
        context.addOption(OPTION, Boolean.class).withDefaultValue(true);
    }

    @Override
    public void onDefinePorts(IPortDefinitionContext context) {
        super.onDefinePorts(context);
        var option = getNodeOptionById(OPTION);
        if (option == null || option.<Boolean>tryGetValue(Boolean.class).result().orElse(true)) {
            context.addInputPort(PORT, Vector3f.class).withDefaultValue(new Vector3f(1, 2, 3)).build();
        }
    }
}
