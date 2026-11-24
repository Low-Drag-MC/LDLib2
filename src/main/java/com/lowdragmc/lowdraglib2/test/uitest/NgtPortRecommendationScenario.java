package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.graph.Graph;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.Node;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibrary;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.NodeModelLibraryItem;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.RecommendationOrderGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestAddNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.joml.Vector2f;

import java.util.List;
import java.util.function.Supplier;

/**
 * The library a wire dragged into empty space opens: it starts at the top, where the recommendation
 * is, and recommends the exact type first and the dragged node's own group next.
 */
@LDLRegisterClient(name = "ngt_port_recommendation", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtPortRecommendationScenario implements UIScenario {

    private static final String NODE = "node";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("ngt", "item-library").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a test graph with one node",
                        ctx -> graphUI(ctx, TestGraph::new, new TestAddNode()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")

                .group("a library scrolled down before", g -> g
                        .step("open the library small, every node listed", ctx -> {
                            var library = library(ctx);
                            library.setPopupSize(150, 80);
                            library.show(100, 60, ignored -> {});
                            library.nodeTree.expandNodeAlongPath(firstNodeOf(library.nodeTree.getRoot()));
                        })
                        .frames(5)
                        .step("scroll it to the bottom", ctx -> library(ctx).resultContainer.verticalScroller.setNormalizedValue(1))
                        .frames(3)
                        .check("it is scrolled", ctx -> library(ctx).resultContainer.verticalScroller.getNormalizedValue() > 0.5f)
                        .step("close it", ctx -> library(ctx).hide())
                        .frames(2))

                .group("a wire dropped into empty space opens it at the top", g -> g
                        .step("drop a wire from the node's float input", ctx -> library(ctx).showWithNodesFitPort(
                                100, 60, List.of(node(ctx).getInputsById().get("in1")), ignored -> {}))
                        .frames(5)
                        .check("it is at the top", ctx -> library(ctx).resultContainer.verticalScroller.getNormalizedValue() == 0)
                        // a float constant matches the type exactly and its tree is walked first
                        .check("first is a node of its group, not a float constant", ctx ->
                                "test".equals(recommended(ctx).getFirst().getGroup()))
                        .step("close the library", ctx -> library(ctx).hide()))

                .openModularUI("a graph with a node in each tier",
                        ctx -> graphUI(ctx, RecommendationOrderGraph::new, new RecommendationOrderGraph.Source()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .group("the exact type first, the dragged node's own group next", g -> g
                        .step("drop a wire from the node's float output", ctx -> library(ctx).showWithNodesFitPort(
                                100, 60, List.of(node(ctx).getOutputsById().get("out")), ignored -> {}))
                        .frames(5)
                        .check("float in its group, float elsewhere, object in its group, object elsewhere", ctx -> {
                            var names = recommended(ctx).stream().map(ItemLibraryItem::getSearchableName).toList();
                            ctx.log("recommended " + names);
                            return names.equals(List.of("rec_order_same_group_float", "rec_order_other_group_float",
                                    "rec_order_same_group_object", "rec_order_other_group_object"));
                        })
                        .step("close the library", ctx -> library(ctx).hide()))

                .closeScreen();
    }

    private static ModularUI graphUI(TestContext ctx, Supplier<Graph> graphFactory, Node source) {
        ScenarioPreferences.isolate(ctx, "ngt_port_recommendation");
        var root = new UIElement().setId("root");
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        var view = new GraphView();
        view.setId("graph");
        view.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addChildren(view);

        var graph = graphFactory.get();
        ctx.put(NODE, graph.graphModel.createNodeModel(source, new Vector2f(0, 0)));
        view.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static List<NodeModelLibraryItem> recommended(TestContext ctx) {
        var root = library(ctx).recommendationTree.getRoot();
        ctx.require("a recommendation is shown", root != null);
        return root.getChildren().stream()
                .map(TreeNode::getKey)
                .filter(NodeModelLibraryItem.class::isInstance)
                .map(NodeModelLibraryItem.class::cast)
                .toList();
    }

    private static TreeNode<ItemLibraryItem, Void> firstNodeOf(TreeNode<ItemLibraryItem, Void> node) {
        if (node.getKey() instanceof NodeModelLibraryItem) return node;
        for (var child : node.getChildren()) {
            var found = firstNodeOf(child);
            if (found != null) return found;
        }
        return null;
    }

    private static ItemLibrary library(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class).itemLibrary;
    }

    private static NodeModel node(TestContext ctx) {
        return ctx.get(NODE);
    }
}
