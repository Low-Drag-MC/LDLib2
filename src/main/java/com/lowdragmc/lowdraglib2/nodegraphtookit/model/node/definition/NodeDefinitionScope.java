package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;

public class NodeDefinitionScope<T extends NodeModel> {
    public final T nodeModel;

    public static ThreadLocal<OptionDefinitionContext> optionContext = ThreadLocal.withInitial(OptionDefinitionContext::new);
    public static ThreadLocal<PortDefinitionContext> portContext = ThreadLocal.withInitial(PortDefinitionContext::new);

    public NodeDefinitionScope(T nodeModel) {
        this.nodeModel = nodeModel;
    }
}
