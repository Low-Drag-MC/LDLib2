package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

/**
 * Flags that define configuration options that affect the behavior and capabilities of a {@link Graph} class.
 *
 * <p>Use the {@link GraphOptions} constants in conjunction with the {@link GraphAttribute} to customize how a graph
 * behaves, including support for subgraphs and automatic node discovery. The default value is {@link #DEFAULT},
 * which enables standard behavior such as allowing nodes defined in the same package as the graph to be automatically
 * included in the graph item library.</p>
 *
 * <p>Combine flags using bitwise OR to enable multiple options.</p>
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * @GraphAttribute(id = "my_graph", options = GraphOptions.SUPPORTS_SUBGRAPHS)
 * public class MyGraph extends Graph { }
 * }</pre>
 */
public final class GraphOptions {

    private GraphOptions() {
        // Utility class - no instantiation
    }

    /**
     * Indicates that this graph supports subgraphs.
     *
     * <p>When enabled, the "Convert Selection to Subgraph" item will be available in the right-click menu
     * of a selection of elements in the graph.</p>
     */
    public static final int SUPPORTS_SUBGRAPHS = 1 << 0;

    /**
     * Indicates that nodes (i.e., subclasses of {@link com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node})
     * defined in the same package as the graph are not automatically added to the graph item library.
     *
     * <p>By default, this flag is disabled. This allows you to discover nodes without manually annotating each one
     * with {@link UseWithGraphAttribute}. Developers who want full control over what appears in the graph item
     * library might choose to enable this option.</p>
     */
    public static final int DISABLE_AUTO_INCLUSION_OF_NODES_FROM_GRAPH_PACKAGE = 1 << 1;

    /**
     * The default graph configuration.
     *
     * <p>This default is helpful for onboarding: if users forget to mark nodes with {@link UseWithGraphAttribute},
     * they will still appear in the graph item library as long as they are defined in the same package as the graph.</p>
     */
    public static final int DEFAULT = 0;

    /**
     * No graph options enabled.
     *
     * <p>This disables all optional features, including subgraph support and automatic node inclusion.</p>
     */
    public static final int NONE = 0;

    /**
     * Checks if a specific option is enabled in the given options flags.
     *
     * @param options the options flags to check
     * @param option the option to check for
     * @return {@code true} if the option is enabled, {@code false} otherwise
     */
    public static boolean hasOption(int options, int option) {
        return (options & option) == option;
    }
}
