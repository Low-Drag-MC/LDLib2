package com.lowdragmc.lowdraglib2.test.noddegraphtoolkit;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IPortDefinitionContext;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A graph whose nodes sit one in each tier of a wire drag's recommendation off {@link Source}'s float
 * output: exact type in its group, exact type elsewhere, compatible in its group, compatible elsewhere.
 * Listed in the reverse of that, so a recommendation that kept the library's order would show it.
 */
public class RecommendationOrderGraph extends Graph {

    @Override
    public List<Class<? extends Node>> getSupportNodes() {
        return List.of(OtherGroupObjectSink.class, SameGroupObjectSink.class, OtherGroupFloatSink.class,
                SameGroupFloatSink.class, Source.class);
    }

    public abstract static class OrderNode extends Node {
        @Override
        public Component getDisplayName() {
            return Component.literal(getClass().getSimpleName());
        }
    }

    @NodeAttribute(name = "rec_order_source", group = "rec/a", graphTypes = {RecommendationOrderGraph.class})
    public static class Source extends OrderNode {
        @Override
        public void onDefinePorts(IPortDefinitionContext context) {
            super.onDefinePorts(context);
            context.addOutputPort("out", Float.class);
        }
    }

    @NodeAttribute(name = "rec_order_same_group_float", group = "rec/a", graphTypes = {RecommendationOrderGraph.class})
    public static class SameGroupFloatSink extends OrderNode {
        @Override
        public void onDefinePorts(IPortDefinitionContext context) {
            super.onDefinePorts(context);
            context.addInputPort("in", Float.class);
        }
    }

    @NodeAttribute(name = "rec_order_other_group_float", group = "rec/b", graphTypes = {RecommendationOrderGraph.class})
    public static class OtherGroupFloatSink extends OrderNode {
        @Override
        public void onDefinePorts(IPortDefinitionContext context) {
            super.onDefinePorts(context);
            context.addInputPort("in", Float.class);
        }
    }

    @NodeAttribute(name = "rec_order_same_group_object", group = "rec/a", graphTypes = {RecommendationOrderGraph.class})
    public static class SameGroupObjectSink extends OrderNode {
        @Override
        public void onDefinePorts(IPortDefinitionContext context) {
            super.onDefinePorts(context);
            context.addInputPort("in", Object.class);
        }
    }

    @NodeAttribute(name = "rec_order_other_group_object", group = "rec/b", graphTypes = {RecommendationOrderGraph.class})
    public static class OtherGroupObjectSink extends OrderNode {
        @Override
        public void onDefinePorts(IPortDefinitionContext context) {
            super.onDefinePorts(context);
            context.addInputPort("in", Object.class);
        }
    }
}
