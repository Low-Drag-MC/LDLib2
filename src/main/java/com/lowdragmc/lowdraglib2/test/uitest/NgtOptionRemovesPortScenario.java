package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.node.PortConstantEditorElement;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeModel;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.NodeOption;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TogglePortTestNode;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An option that removes a port while the graph is open. The node drops the port and its constant at once, and the
 * removed port's editor must not tick against the missing constant before the graph view catches up — a vector
 * editor logged a NullPointerException per component when it did.
 */
@LDLRegisterClient(name = "ngt_option_removes_port", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NgtOptionRemovesPortScenario implements UIScenario {

    private static final List<String> ERRORS = new CopyOnWriteArrayList<>();
    private static final AbstractAppender APPENDER = new AbstractAppender("ngt_option_removes_port", null, null,
            true, Property.EMPTY_ARRAY) {
        @Override
        public void append(LogEvent event) {
            // the graph view ticks on the render thread; an error from any other is not this scenario's
            if (event.getLevel().isMoreSpecificThan(Level.ERROR) && "Render thread".equals(event.getThreadName())) {
                ERRORS.add(event.getMessage().getFormattedMessage());
            }
        }
    };

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("graph", "ngt", "option").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("graph editor with one toggle-port node", NgtOptionRemovesPortScenario::buildGraphUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#graph")
                .waitUntil("the node is laid out", ctx -> ctx.count(".__node-element__") == 1)
                .check("the vector port has an editor on the node", ctx -> editor(ctx) != null)
                .step("listen for errors", ctx -> {
                    ERRORS.clear();
                    APPENDER.start();
                    rootLogger().addAppender(APPENDER);
                })
                .step("turn the option off", ctx -> {
                    var port = ((NodeOption) node(ctx).getNodeOptionById(TogglePortTestNode.OPTION)).getPortModel();
                    port.setValue(false);
                    port.notifyValueChanged();
                })
                .settleMs(250)
                .check("the port and its editor are gone", ctx ->
                        !node(ctx).getInputsById().containsKey(TogglePortTestNode.PORT) && editor(ctx) == null)
                .check("and nothing logged an error on the way", ctx -> {
                    ctx.log("errors: " + ERRORS);
                    return ERRORS.isEmpty();
                })
                .closeScreen();
        s.teardown("stop listening", ctx -> rootLogger().removeAppender(APPENDER));
    }

    private static Logger rootLogger() {
        return (Logger) LogManager.getRootLogger();
    }

    private static GraphView graphView(TestContext ctx) {
        return ctx.el("#graph").as(GraphView.class);
    }

    private static NodeModel node(TestContext ctx) {
        return (NodeModel) graphView(ctx).getGraph().graphModel.getNodeModels().get(0);
    }

    @Nullable
    private static PortConstantEditorElement editor(TestContext ctx) {
        return find(graphView(ctx));
    }

    @Nullable
    private static PortConstantEditorElement find(UIElement element) {
        if (element instanceof PortConstantEditorElement port && port.getEditor() != null
                && TogglePortTestNode.PORT.equals(port.portModel.getPortId())) {
            return port;
        }
        for (var child : element.getChildren()) {
            var found = find(child);
            if (found != null) return found;
        }
        return null;
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
        graph.graphModel.createNodeModel(new TogglePortTestNode(), new Vector2f(0, 0));
        editor.loadGraph(graph);
        return new ModularUI(UI.of(root), ctx.player());
    }
}
