package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify which {@link Graph} types are compatible with the annotated {@link Node} class.
 *
 * <p>This annotation links a specific {@link Node} class to one or more {@link Graph} types, enabling fine-grained
 * control over which graph types support the node. This allows framework authors to explicitly declare node
 * compatibility across different kinds of graphs and ensures that only valid nodes are available for use in each
 * graph context.</p>
 *
 * <p>By default, nodes defined in the same package as the graph are considered compatible and available.
 * In this default setup, the {@link UseWithGraphAttribute} is not required.
 * However, when a graph uses {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.GraphOptions#DISABLE_AUTO_INCLUSION_OF_NODES_FROM_GRAPH_PACKAGE},
 * this annotation must be used to declare which {@link Graph} types support the node.</p>
 *
 * <p>This annotation affects editor behaviors such as graph item library population and helps prevent the accidental
 * use of unsupported nodes.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @UseWithGraphAttribute(graphTypes = {MyGraph.class, AnotherGraph.class})
 * public class MyNode extends Node { }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseWithGraphAttribute {
    /**
     * The graph types that support the annotated node type.
     *
     * @return an array of graph types that can use this node
     */
    Class<? extends Graph>[] graphTypes();
}
