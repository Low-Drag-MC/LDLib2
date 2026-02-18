package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to define a link between a subgraph type and a main graph type.
 *
 * <p>Apply this annotation to a custom {@link Graph} class to declare it as a valid subgraph type for a specific
 * parent (main) graph type. This annotation is required when you want to designate a specific graph type to function
 * as a subgraph in tools that support subgraphs.</p>
 *
 * <p>Use it on custom graph classes that are designed to act as subgraphs. This is useful when you want to provide
 * specialized subgraph behaviors, customize the user experience, or restrict subgraph usage to certain graph types.</p>
 *
 * <p>When a graph type declares that it supports subgraphs using {@link GraphOptions#SUPPORTS_SUBGRAPHS} but no
 * corresponding {@link SubgraphAttribute} is found, the main graph type itself is used as the subgraph type by default.</p>
 *
 * <p>You can associate multiple subgraph types with the same main graph type. In this case, the editor's context menu
 * includes multiple "Create Subgraph" actions—one for each valid subgraph type.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * // This declares a subgraph type used by MyMainGraph
 * @SubgraphAttribute(mainGraphType = MyMainGraph.class)
 * public class MySubgraph extends Graph { }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubgraphAttribute {

    /**
     * The type of the main {@link Graph} that supports this subgraph type.
     *
     * <p>This must be a type that inherits from {@link Graph}.</p>
     *
     * @return the main graph type class
     */
    Class<? extends Graph> mainGraphType();
}
