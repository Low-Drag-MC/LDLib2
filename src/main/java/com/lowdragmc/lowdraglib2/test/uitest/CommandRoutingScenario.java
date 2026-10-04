package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.HistoryStack;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.nodegraphtookit.gui.GraphView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraph;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Commands start at the focused element and bubble: whoever handles one has to stop it, or an ancestor
 * answering the same command runs it again — a graph inside a graph editor used to undo twice per Ctrl+Z.
 */
@LDLRegisterClient(name = "command_routing", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class CommandRoutingScenario implements UIScenario {

    private static final String ROOT_SAW = "root_saw";
    private static final String PANEL_SAW = "panel_saw";
    private static final String FIELD = "field";
    private static final String PLAIN = "plain";
    private static final String GRAPH_EDITOR = "graph_editor";
    private static final String UNDOS = "undos";
    private static final String REDOS = "redos";
    private static final String SAVES = "saves";
    private static final String USER_CLIPBOARD = "user_clipboard";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("commands", "editor", "ngt").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("a text field and a graph editor under listening ancestors", CommandRoutingScenario::buildUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the graph editor has laid out", ctx -> graphEditor(ctx).getSizeHeight() > 0)
                // the run drives the real system clipboard; hand the user theirs back at the end
                .step("remember the clipboard", ctx -> ctx.put(USER_CLIPBOARD, clipboard()))

                .group("a focused text field handles the clipboard commands itself", g -> g
                        .step("type into a focused field", ctx -> {
                            forgetWhatAncestorsSaw(ctx);
                            field(ctx).focus();
                            field(ctx).setText("hello");
                        })
                        .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                        .check("ctrl+a selects the whole text", ctx -> {
                            var field = field(ctx);
                            return Math.min(field.getSelectionStart(), field.getSelectionEnd()) == 0
                                    && Math.max(field.getSelectionStart(), field.getSelectionEnd()) == 5;
                        })
                        .key(GLFW.GLFW_KEY_C, Keys.MOD_CONTROL)
                        .step("ctrl+c puts it on the system clipboard",
                                ctx -> ctx.check("clipboard", "hello".equals(clipboard()), "hello", clipboard()))
                        .key(GLFW.GLFW_KEY_END)
                        .key(GLFW.GLFW_KEY_V, Keys.MOD_CONTROL)
                        .step("ctrl+v pastes it back", ctx -> ctx.check("field text",
                                "hellohello".equals(field(ctx).getRawText()), "hellohello", field(ctx).getRawText()))
                        .key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL)
                        .key(GLFW.GLFW_KEY_X, Keys.MOD_CONTROL)
                        .check("ctrl+x empties the field", ctx -> field(ctx).getRawText().isEmpty())
                        .check("and copied what it took", ctx -> "hellohello".equals(clipboard()))
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .step("none of it reached the field's container",
                                ctx -> ctx.check("panel saw", saw(ctx, PANEL_SAW).isEmpty(), "[]", saw(ctx, PANEL_SAW))))

                .group("a command nobody handles bubbles to the container", g -> g
                        .step("focus an element with no command handling", ctx -> {
                            forgetWhatAncestorsSaw(ctx);
                            plain(ctx).focus();
                        })
                        .check("it has the focus", ctx -> plain(ctx).isFocused())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .check("the container got the undo", ctx -> saw(ctx, PANEL_SAW).equals(List.of(CommandEvents.UNDO)))
                        .check("and so did the root", ctx -> saw(ctx, ROOT_SAW).equals(List.of(CommandEvents.UNDO))))

                .group("the graph view's commands run once and stop there", g -> g
                        .step("record two undoable edits on the graph", ctx -> {
                            forgetWhatAncestorsSaw(ctx);
                            var undos = new AtomicInteger();
                            var redos = new AtomicInteger();
                            ctx.put(UNDOS, undos);
                            ctx.put(REDOS, redos);
                            var history = history(ctx);
                            for (var name : List.of("first", "second")) {
                                history.pushHistory(Component.literal(name),
                                        EditAction.of(redos::incrementAndGet, undos::incrementAndGet), null, false);
                            }
                            graphView(ctx).focus();
                        })
                        .check("the graph view has the focus", ctx -> graphView(ctx).isFocused())
                        .key(GLFW.GLFW_KEY_Z, Keys.MOD_CONTROL)
                        .step("ctrl+z undoes one edit, not two", ctx -> ctx.check("undos",
                                count(ctx, UNDOS) == 1, 1, count(ctx, UNDOS)))
                        .key(GLFW.GLFW_KEY_Y, Keys.MOD_CONTROL)
                        .step("ctrl+y redoes one edit", ctx -> ctx.check("redos",
                                count(ctx, REDOS) == 1, 1, count(ctx, REDOS)))
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .step("ctrl+s saves the graph once", ctx -> ctx.check("saves",
                                count(ctx, SAVES) == 1, 1, count(ctx, SAVES)))
                        .step("none of them reached the root",
                                ctx -> ctx.check("root saw", saw(ctx, ROOT_SAW).isEmpty(), "[]", saw(ctx, ROOT_SAW))))

                .step("give the clipboard back", ctx -> Minecraft.getInstance().keyboardHandler
                        .setClipboard(ctx.<String>get(USER_CLIPBOARD)))
                .closeScreen();
    }

    private static ModularUI buildUI(TestContext ctx) {
        var rootSaw = new ArrayList<String>();
        var panelSaw = new ArrayList<String>();
        var saves = new AtomicInteger();
        ctx.put(ROOT_SAW, rootSaw);
        ctx.put(PANEL_SAW, panelSaw);
        ctx.put(SAVES, saves);

        var root = new UIElement().setId("root");
        root.layout(layout -> layout.widthPercent(100).heightPercent(100));
        root.addEventListener(UIEvents.EXECUTE_COMMAND, event -> rootSaw.add(event.command));

        var panel = new UIElement().setId("panel");
        panel.layout(layout -> layout.widthPercent(100).height(20).gapAll(4).paddingAll(4)
                .flexDirection(FlexDirection.ROW));
        panel.addEventListener(UIEvents.EXECUTE_COMMAND, event -> panelSaw.add(event.command));

        var field = new TextField();
        field.setAnyString();
        field.layout(layout -> layout.width(120).height(12));
        var plain = new UIElement().setFocusable(true);
        plain.layout(layout -> layout.width(20).height(12));
        panel.addChildren(field, plain);
        ctx.put(FIELD, field);
        ctx.put(PLAIN, plain);

        var graphEditor = new GraphEditorView();
        graphEditor.layout(layout -> layout.widthPercent(100).flex(1));
        graphEditor.loadGraph(new TestGraph(), tag -> saves.incrementAndGet());
        ctx.put(GRAPH_EDITOR, graphEditor);

        root.addChildren(panel, graphEditor);
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static void forgetWhatAncestorsSaw(TestContext ctx) {
        saw(ctx, ROOT_SAW).clear();
        saw(ctx, PANEL_SAW).clear();
    }

    private static List<String> saw(TestContext ctx, String key) {
        return ctx.get(key);
    }

    private static int count(TestContext ctx, String key) {
        return ctx.<AtomicInteger>get(key).get();
    }

    private static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    private static TextField field(TestContext ctx) {
        return ctx.get(FIELD);
    }

    private static UIElement plain(TestContext ctx) {
        return ctx.get(PLAIN);
    }

    private static GraphEditorView graphEditor(TestContext ctx) {
        return ctx.get(GRAPH_EDITOR);
    }

    private static GraphView graphView(TestContext ctx) {
        return graphEditor(ctx).getCurrentView();
    }

    private static HistoryStack history(TestContext ctx) {
        return graphView(ctx).getHistoryStack();
    }
}
