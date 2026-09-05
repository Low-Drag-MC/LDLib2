package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.HeaderConfigurator;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.FieldValueInspector;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.NodeElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.OptionTestNode;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code IOptionBuilder#showInInspectorOnly()} — an option that is edited from the inspector instead
 * of from the node body.
 *
 * <p>The flag shipped with the toolkit and did nothing: the model carried it, but
 * {@code NodeOptionsInspector} read it only to decide whether to rebuild and drew the row anyway,
 * and {@code NodeElement#onSelectionInspect} put no options in the inspector at all. So the two
 * halves are asserted separately — the option is absent from the node, and present in the inspector
 * — because either one passing alone still leaves the feature broken.</p>
 *
 * <p>The node's own options are checked in the inspector too: an option drawn in the node body stays
 * editable from both places, which is what makes the inspector the node's full configuration rather
 * than an overflow bin for whatever the body refused.</p>
 */
@LDLRegisterClient(name = "ngt_inspector_only_option", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtInspectorOnlyOptionScenario implements UIScenario {

    /** The option {@link OptionTestNode} marks {@code showInInspectorOnly()}. */
    private static final String INSPECTOR_ONLY = "inspector_only";
    /** Options the same node leaves on the body, which the inspector must also list. */
    private static final List<String> BODY_OPTIONS = List.of("enum", "string[]", "color", "block", "stack");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "option").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("graph editor with one option node", NgtInspectorOnlyOptionScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the node is laid out", ctx -> ctx.count(".__node-element__") == 1)
                .step("frame the graph", ctx -> graphView(ctx).fitGraphChildren(60f))
                .settleMs(150)

                // Read off the option container rather than counting .__field-value-inspector__
                // globally: port constant editors are built from the same element, so a global count
                // would pass for the wrong reason.
                .group("the node body omits the inspector-only option", g -> g
                        .step("collect the node's option rows", ctx -> {
                            var names = bodyOptionNames(ctx);
                            ctx.log("node body options: " + names);
                            ctx.check("the inspector-only option is not drawn in the node",
                                    !names.contains(INSPECTOR_ONLY), "absent",
                                    names.contains(INSPECTOR_ONLY) ? "present" : "absent");
                            for (var option : BODY_OPTIONS) {
                                ctx.check("the node still draws '" + option + "'", names.contains(option));
                            }
                        })
                        .screenshotElement("01_node_body", ".__node-element__"))

                .group("selecting the node surfaces every option in the inspector", g -> g
                        .step("select the node", ctx -> {
                            var element = ctx.el(".__node-element__").as(NodeElement.class);
                            graphView(ctx).addSelected(element.getModel());
                        })
                        .settleMs(120)
                        .step("collect the inspector's rows", ctx -> {
                            var labels = inspectorLabels(ctx);
                            ctx.log("inspector rows: " + labels);
                            ctx.check("the inspector-only option is editable from the inspector",
                                    labels.contains(INSPECTOR_ONLY), "present",
                                    labels.contains(INSPECTOR_ONLY) ? "present" : "absent");
                            for (var option : BODY_OPTIONS) {
                                ctx.check("the inspector also lists '" + option + "'", labels.contains(option));
                            }
                        })
                        // The heading is what separates the node's identity rows from its options, so
                        // its position carries the meaning - merely existing somewhere would not.
                        .step("a heading separates the identity rows from the options", ctx -> {
                            var rows = inspectorRows(ctx);
                            int header = indexOfHeader(rows);
                            int firstOption = indexOfLabel(rows, BODY_OPTIONS.get(0));
                            ctx.check("exactly one heading is added",
                                    rows.stream().filter(HeaderConfigurator.class::isInstance).count() == 1,
                                    1, rows.stream().filter(HeaderConfigurator.class::isInstance).count());
                            ctx.check("the heading sits above the first option",
                                    header >= 0 && firstOption >= 0 && header < firstOption,
                                    "header < option", header + " < " + firstOption);
                            ctx.check("the identity rows stay above the heading", header > 0, "> 0", header);
                        })
                        .screenshot("02_inspector"))

                .closeScreen();
    }

    /** Display names of the option rows the node drew in its body, in order. */
    private static List<String> bodyOptionNames(TestContext ctx) {
        var nodeElement = ctx.el(".__node-element__").as(NodeElement.class);
        var container = nodeElement.getNodeOptionContainer();
        if (container == null) throw new IllegalStateException("the node has no option container");
        var names = new ArrayList<String>();
        for (var child : container.getChildren()) {
            if (child instanceof FieldValueInspector row) {
                names.add(row.fieldName.getText().getString());
            }
        }
        return names;
    }

    /**
     * Labels of every configurator the inspector built. Walked off {@code GraphView#inspector}
     * directly rather than through a selector: the inspector is docked inside the graph view's own
     * panel layer, so a selector would have to know that layout, and the rows are what matters here
     * rather than where they are drawn.
     */
    private static List<String> inspectorLabels(TestContext ctx) {
        return inspectorRows(ctx).stream().map(c -> c.getLabel().getString()).toList();
    }

    /** Every configurator the inspector built, in the order it built them. */
    private static List<Configurator> inspectorRows(TestContext ctx) {
        var rows = new ArrayList<Configurator>();
        collectRows(graphView(ctx).inspector, rows);
        return rows;
    }

    private static void collectRows(UIElement element, List<Configurator> rows) {
        if (element instanceof Configurator configurator) {
            rows.add(configurator);
        }
        for (var child : element.getChildren()) {
            collectRows(child, rows);
        }
    }

    private static int indexOfHeader(List<Configurator> rows) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) instanceof HeaderConfigurator) return i;
        }
        return -1;
    }

    private static int indexOfLabel(List<Configurator> rows, String label) {
        for (int i = 0; i < rows.size(); i++) {
            if (label.equals(rows.get(i).getLabel().getString())) return i;
        }
        return -1;
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static ModularUI buildGraphUI(TestContext ctx) {
        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        var editor = new GraphView();
        editor.setId("graph");
        editor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        root.addChildren(editor);

        var graph = new TestGraph();
        graph.graphModel.createNodeModel(new OptionTestNode(), new Vector2f(0, 0));
        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
