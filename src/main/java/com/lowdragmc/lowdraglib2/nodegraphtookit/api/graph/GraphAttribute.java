package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare a graph type by associating it with an identifier and optional configuration options.
 *
 * <p>Use this annotation to associate a custom {@link Graph} class with a unique identifier and {@link GraphOptions}.
 * The {@code id} parameter defines the unique identifier for the graph type. You can also configure additional
 * options using {@link GraphOptions}.</p>
 *
 * <p>This annotation is required for any class that inherits from {@link Graph} and serves as the entry point
 * for enabling editor support for the graph tool.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @GraphAttribute(id = "my_graph", options = GraphOptions.SUPPORTS_SUBGRAPHS)
 * public class MyGraph extends Graph { }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphAttribute {

    /**
     * Gets the unique identifier associated with the {@link Graph}.
     *
     * <p>The identifier must be unique across all graph types in the project.</p>
     *
     * @return the unique identifier for this graph type
     */
    String id();

    /**
     * Gets the graph configuration options.
     *
     * <p>These options define specific behaviors of the graph, such as
     * {@link GraphOptions#SUPPORTS_SUBGRAPHS} or {@link GraphOptions#DISABLE_AUTO_INCLUSION_OF_NODES_FROM_GRAPH_PACKAGE}.</p>
     *
     * @return the configuration options (defaults to {@link GraphOptions#DEFAULT})
     */
    int options() default GraphOptions.DEFAULT;
}
