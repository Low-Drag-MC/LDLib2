package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary.ItemLibraryItem;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.TestGraphToolkit;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

import java.util.Set;

/** Captures a node's createDescriptionUI() output inside the real NGT item library. */
@LDLRegisterClient(name = "ngt_node_description", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtNodeDescriptionScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("wiki", "ngt", "visual").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("NGT node description capture",
                        ctx -> new TestGraphToolkit().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#root")
                .step("open the item library", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    graphView.itemLibrary.setDescriptionWidth(180).show(120, 70, ignored -> {});
                })
                .frames(3)
                .step("select Description Test", ctx -> {
                    var graphView = ctx.query().type(GraphView.class).one().as(GraphView.class);
                    var tree = graphView.itemLibrary.nodeTree;
                    var node = find(tree.getRoot(), "test_description");
                    if (node == null) {
                        throw new IllegalStateException("test_description is missing from the item library");
                    }
                    tree.expandNodeAlongPath(node);
                    tree.setSelected(Set.of(node), true);
                })
                .frames(3)
                .screenshotElement("node-description", "#root")
                .closeScreen();
    }

    private static TreeNode<ItemLibraryItem, Void> find(TreeNode<ItemLibraryItem, Void> node, String registryName) {
        if (node == null) return null;
        if (registryName.equals(node.getKey().getSearchableName())) return node;
        for (var child : node.getChildren()) {
            var found = find(child, registryName);
            if (found != null) return found;
        }
        return null;
    }
}
