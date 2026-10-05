package com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition;

import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;

public class NodeDefinitionScope<T extends NodeModel> {
    public final T nodeModel;

    public static ThreadLocal<OptionDefinitionContext> optionContext = ThreadLocal.withInitial(OptionDefinitionContext::new);
    public static ThreadLocal<PortDefinitionContext> portContext = ThreadLocal.withInitial(PortDefinitionContext::new);

    public NodeDefinitionScope(T nodeModel) {
        this.nodeModel = nodeModel;
    }

    /**
     * The context to define {@code scope}'s options in. A node defined while another one's definition is under way on
     * this thread (a graph read from inside a node's definition) gets one of its own, so the outer node's later
     * options still land on the outer node. Hand it back with {@link OptionDefinitionContext#release()}.
     */
    public static OptionDefinitionContext optionContextFor(NodeDefinitionScope<?> scope) {
        var context = optionContext.get();
        if (context.getScope() != null) {
            context = new OptionDefinitionContext();
        }
        context.setScope(scope);
        return context;
    }

    /** {@link #optionContextFor} for ports. Hand it back with {@link PortDefinitionContext#release()}. */
    public static PortDefinitionContext portContextFor(NodeDefinitionScope<?> scope) {
        var context = portContext.get();
        if (context.getScope() != null) {
            context = new PortDefinitionContext();
        }
        context.setScope(scope);
        return context;
    }
}
