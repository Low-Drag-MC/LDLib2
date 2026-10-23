package com.lowdragmc.lowdraglib2.gui.ui.compose.scenario;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.compose.demo.TestComposeTodo;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.target.Texts;

import java.util.ArrayList;
import java.util.List;

/** Drives the todo demo the way a player would, and checks the tree agrees at every step. */
@LDLRegisterClient(name = "compose_todo", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposeTodoScenario implements UIScenario {

    private final List<UIElement> rowsBeforeFilter = new ArrayList<>();

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(120).tags("compose").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("compose todo demo", ctx -> new TestComposeTodo().createUI(ctx.requirePlayer()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement(".todo-add")

                .checkExists(".todo-empty")
                .checkText(".todo-count", "剩余 0 / 0")

                // A controlled input: the field shows composition state, so this only works if the round trip closes.
                .group("add three items", g -> {
                    addTodo(g, "alpha");
                    addTodo(g, "bravo");
                    addTodo(g, "charlie");
                })
                .checkNotExists(".todo-empty")
                .checkCount(".todo-row", 3)
                .checkText(".todo-count", "剩余 3 / 3")
                .check("rows are in insertion order", ctx ->
                        rowTexts(ctx).equals(List.of("alpha", "bravo", "charlie")))
                .check("the input cleared itself after each add", ctx ->
                        ctx.query().select(".todo-input").one().value().equals(""))
                .screenshotElement("todo-populated", ".__root__")

                // Toggling rewrites the whole list, but only the toggled row should change.
                .step("remember the row elements", ctx -> {
                    rowsBeforeFilter.clear();
                    rowsBeforeFilter.addAll(rows(ctx));
                })
                // Ids are handed out in insertion order, so row 1 is "alpha".
                .click(".todo-row-1 .todo-toggle")
                .frames(3)
                .checkText(".todo-count", "剩余 2 / 3")
                .check("rebuilding the list did not rebuild the rows", ctx -> {
                    var now = rows(ctx);
                    if (now.size() != rowsBeforeFilter.size()) return false;
                    for (int i = 0; i < now.size(); i++) {
                        if (now.get(i) != rowsBeforeFilter.get(i)) return false;
                    }
                    return true;
                })

                // Filtering unmounts rows...
                .click(".todo-filter-active")
                .frames(3)
                .checkCount(".todo-row", 2)
                .check("only the unfinished ones are left", ctx ->
                        rowTexts(ctx).equals(List.of("bravo", "charlie")))

                .click(".todo-filter-done")
                .frames(3)
                .checkCount(".todo-row", 1)
                .check("only the finished one is left", ctx ->
                        rowTexts(ctx).equals(List.of("alpha")))

                // ...and coming back restores them in the original order.
                .click(".todo-filter-all")
                .frames(3)
                .checkCount(".todo-row", 3)
                .check("original order is restored", ctx ->
                        rowTexts(ctx).equals(List.of("alpha", "bravo", "charlie")))

                // Deleting from the middle - the classic index-arithmetic trap. Row 2 is "bravo".
                .step("delete bravo", ctx -> clickOnce(ctx, ".todo-row-2 .todo-delete"))
                .frames(3)
                .checkCount(".todo-row", 2)
                .check("the right row went", ctx -> rowTexts(ctx).equals(List.of("alpha", "charlie")))
                .checkText(".todo-count", "剩余 1 / 2")
                .screenshotElement("todo-after-delete", ".__root__")

                .closeScreen();
    }

    private static void addTodo(ScenarioBuilder s, String text) {
        s.typeInto(".todo-input", text).frames(2).click(".todo-add").frames(3);
    }

    /**
     * Presses and releases on a control that removes itself on mouse-down, where
     * {@link ScenarioBuilder#click} would re-resolve the selector for the release and find nothing.
     */
    private static void clickOnce(TestContext ctx, String selector) {
        var bounds = ctx.query().select(selector).one().bounds();
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    private static List<UIElement> rows(TestContext ctx) {
        var scroller = ctx.query().type(ScrollerView.class).one().as(ScrollerView.class);
        return List.copyOf(scroller.getContentHost().getChildren());
    }

    /** Row captions in document order, read from the tree because a selector query is in registration order. */
    private static List<String> rowTexts(TestContext ctx) {
        var texts = new ArrayList<String>();
        for (var row : rows(ctx)) {
            for (var child : row.getChildren()) {
                if (child.hasClass("todo-text")) texts.add(Texts.of(child));
            }
        }
        return texts;
    }
}
