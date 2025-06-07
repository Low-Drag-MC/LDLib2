package com.lowdragmc.lowdraglib2.graphprocessor.data;

import com.lowdragmc.lowdraglib2.graphprocessor.data.parameter.ExposedParameter;
import com.lowdragmc.lowdraglib2.graphprocessor.data.parameter.ParameterNode;

import java.util.*;
import java.util.function.Consumer;

public class GraphUtils {
    enum State {
        White,
        Grey,
        Black,
    }

    static class TarversalNode {
        public BaseNode node;
        public List<TarversalNode> inputs = new ArrayList<>();
        public List<TarversalNode> outputs = new ArrayList<>();
        public State    state = State.White;

        public TarversalNode(BaseNode node) { this.node = node; }
    }

    // A structure made for easy graph traversal
    static class TraversalGraph
    {
        public List<TarversalNode> nodes = new ArrayList<>();
        public List<TarversalNode> outputs = new ArrayList<>();
    }

    static TraversalGraph ConvertGraphToTraversalGraph(BaseGraph graph) {
        TraversalGraph g = new TraversalGraph();
        Map<BaseNode, TarversalNode> nodeMap = new HashMap<>();

        for (var node : graph.nodes) {
            var tn = new TarversalNode(node);
            g.nodes.add(tn);
            nodeMap.put(node, tn);

            if (graph.graphOutputs.contains(node))
                g.outputs.add(tn);
        }

        for (var tn : g.nodes) {
            tn.inputs = tn.node.getInputNodes().stream().filter(nodeMap::containsKey).map(nodeMap::get).toList();
            tn.outputs = tn.node.GetOutputNodes().stream().filter(nodeMap::containsKey).map(nodeMap::get).toList();
        }

        return g;
    }

    public static List<BaseNode> DepthFirstSort(BaseGraph g) {
        var graph = ConvertGraphToTraversalGraph(g);
        List<BaseNode> depthFirstNodes = new ArrayList<>();
        for (var n : graph.nodes)
            DFS1(graph, depthFirstNodes, n);

        return depthFirstNodes;
    }

    private static void DFS1(TraversalGraph graph, List<BaseNode> depthFirstNodes, TarversalNode n) {
        if (n.state == State.Black)
            return;

        n.state = State.Grey;

        if (n.node instanceof ParameterNode parameterNode && parameterNode.parameter.getAccessor() == ExposedParameter.ParameterAccessor.Get){
            for (var setter : graph.nodes.stream().filter(x-> x.node instanceof ParameterNode p &&
                            Objects.equals(p.parameterIdentifier, parameterNode.parameterIdentifier) &&
                            p.parameter.getAccessor() == ExposedParameter.ParameterAccessor.Set).toList()) {
                if (setter.state == State.White)
                    DFS1(graph, depthFirstNodes, setter);
            }
        } else {
            for (var input : n.inputs) {
                if (input.state == State.White)
                    DFS1(graph, depthFirstNodes, input);
            }
        }

        n.state = State.Black;

        // Only add the node when his children are completely visited
        depthFirstNodes.add(n.node);
    }

    public static void FindCyclesInGraph(BaseGraph g, Consumer<BaseNode> cyclicNode) {
        var graph = ConvertGraphToTraversalGraph(g);
        List<TarversalNode> cyclicNodes = new ArrayList<>();

        for (var n : graph.nodes)
            DFS2(cyclicNodes, n);

        if (cyclicNode != null) {
            cyclicNodes.forEach(tn -> cyclicNode.accept(tn.node));
        }

    }

    private static void DFS2(List<TarversalNode> cyclicNodes, TarversalNode n) {
        if (n.state == State.Black)
            return;

        n.state = State.Grey;

        for (var input : n.inputs) {
            if (input.state == State.White)
                DFS2(cyclicNodes, input);
            else if (input.state == State.Grey)
                cyclicNodes.add(n);
        }
        n.state = State.Black;
    }
}
