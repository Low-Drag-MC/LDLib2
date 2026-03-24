package com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.NodeAttribute;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * A per-graph node registry that automatically discovers {@link NodeAttribute}-annotated nodes
 * bound to a specific graph type.
 * <p>
 * Usage:
 * <pre>{@code
 * public class MyGraph extends Graph {
 *     public static final GraphNodeRegistry NODE_REGISTRY =
 *             GraphNodeRegistry.create(MyMod.id("my_graph"), MyGraph.class);
 *
 *     @Override
 *     public List<Class<? extends Node>> getSupportNodes() {
 *         return NODE_REGISTRY.getNodeClasses();
 *     }
 * }
 * }</pre>
 */
public final class GraphNodeRegistry {
    private final AutoRegistry<NodeAttribute, Node, Class<? extends Node>> registry;

    private GraphNodeRegistry(AutoRegistry<NodeAttribute, Node, Class<? extends Node>> registry) {
        this.registry = registry;
    }

    /**
     * Creates a node registry for a specific graph type.
     * Automatically scans and filters nodes by modID, environment, and graphTypes.
     *
     * @param registryName the registry identifier
     * @param graphClass   the graph class to filter nodes for
     * @return a new GraphNodeRegistry
     */
    public static GraphNodeRegistry create(ResourceLocation registryName, Class<? extends Graph> graphClass) {
        var autoRegistry = AutoRegistry.<NodeAttribute, Node, Class<? extends Node>>create(
                registryName,
                NodeAttribute.class,
                Node.class,
                annotationData -> {
                    if (annotationData.containsKey("modID") && annotationData.get("modID") instanceof String modID) {
                        if (!modID.isEmpty() && !Platform.isModLoaded(modID)) return false;
                    }
                    return RegistrationEnvironment.shouldRegister(annotationData);
                },
                clazz -> {
                    if (Modifier.isAbstract(clazz.getModifiers())) return false;
                    var annotation = clazz.getAnnotation(NodeAttribute.class);
                    if (annotation == null) return false;
                    for (var gt : annotation.graphTypes()) {
                        if (gt == graphClass) return true;
                    }
                    return false;
                },
                (annotation, clazz) -> annotation.name(),
                (annotation, clazz) -> clazz,
                (a, b) -> b.annotation().priority() - a.annotation().priority()
        );
        return new GraphNodeRegistry(autoRegistry);
    }

    /**
     * Returns the node classes registered for this graph, sorted by priority.
     */
    public List<Class<? extends Node>> getNodeClasses() {
        var result = new ArrayList<Class<? extends Node>>();
        for (var holder : registry) {
            result.add(holder.value());
        }
        return List.copyOf(result);
    }

    /**
     * Looks up a node entry by name.
     *
     * @return the holder, or null if not found
     */
    public AutoRegistry.Holder<NodeAttribute, Node, Class<? extends Node>> get(String name) {
        return registry.get(name);
    }

    /**
     * Returns the underlying AutoRegistry.
     */
    public AutoRegistry<NodeAttribute, Node, Class<? extends Node>> getRegistry() {
        return registry;
    }
}
