package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebuggerWindow;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * One debugger, three windows.
 *
 * <p>This is the case the old debug screen could not do at all. It lived in the game window and drove
 * everything through that window's mouse, so a UI hosted in a second operating-system window could be
 * listed in its picker but not usefully inspected: the outlines would have been drawn in the wrong
 * window and the element picker would have hit-tested against the wrong coordinates.
 *
 * <p>What is exercised here is that none of that is true any more. The debugger runs in window two,
 * is pointed at a UI in window three, and picks an element there — by a click posted into window
 * three's own event queue, so the hit test, the coordinate space and the swallowed press are all that
 * window's rather than the game's.
 *
 * <p>Retargeting is the other half: pointing the debugger elsewhere has to move the inspection with
 * it. The UI left behind must stop reporting itself as debugged and go back to handling its own
 * clicks, or every UI ever inspected would keep paying for it.
 */
@LDLRegisterClient(name = "ui_debugger_multi_window", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class UIDebuggerMultiWindowScenario implements UIScenario {

    private static final String TOOL_WINDOW = "tool_window";
    private static final String TOOL_BUTTON = "tool_button";
    private static final String TOOL_CLICKS = "tool_clicks";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("debugger", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        UIDebuggerScenario.openTarget(s)
                .check("no native windows are open to begin with", ctx -> !OsWindowManager.hasWindows())

                .group("a second UI in a window of its own", g -> g
                        .step("open a tool window", ctx -> {
                            var window = new ModularUIWindow(new ModularUI(UI.of(toolRoot(ctx))), "Tool");
                            ctx.check("the tool window opened",
                                    window.open(Integer.MIN_VALUE, Integer.MIN_VALUE, 480, 320, false));
                            ctx.put(TOOL_WINDOW, window);
                        })
                        .frames(20)
                        .check("the tool window is open", ctx -> toolWindow(ctx).isOpen())
                        .check("its UI was laid out against the window",
                                ctx -> toolButton(ctx).getSizeWidth() > 0))

                .group("the debugger opens on the game window's UI", g -> {
                    UIDebuggerWindowScenario.popOut(g);
                    g.check("it is inspecting the game window's UI",
                                ctx -> window().getTarget() == UIDebuggerScenario.targetUI(ctx))
                     // Three windows now: the game's, the tool's, and the debugger's.
                     .check("the tool window is untouched", ctx -> toolWindow(ctx).isOpen())
                     .check("the tool UI is not being debugged",
                             ctx -> !toolWindow(ctx).getModularUI().isDebugMode());
                })

                .group("the picker offers every UI on screen", g -> g
                        .check("the picker is showing more than one candidate",
                                ctx -> ctx.in(debuggerUI()).type(Button.class)
                                        .withTextContaining("Game Window").count() == 1)
                        .check("including the tool window, by its title",
                                ctx -> ctx.in(debuggerUI()).type(Button.class)
                                        .withText("Tool").count() == 1)
                        // Not itself. A debugger inspecting its own tree redraws what you are reading
                        // as you walk it, which is a hall of mirrors rather than a feature.
                        .check("but not the debugger's own window",
                                ctx -> ctx.in(debuggerUI()).type(Button.class)
                                        .withTextContaining("UI Debugger").count() == 0))

                .group("clicking the picker moves the inspection", g -> g
                        .step("aim at the tool window's entry in the picker", ctx ->
                                ctx.input(window()).moveTo(pickerEntry(ctx, "Tool")))
                        .frames(2)
                        .step("press it", ctx -> ctx.input(window()).mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT))
                        .step("release it", ctx -> ctx.input(window()).mouseUp(GLFW.GLFW_MOUSE_BUTTON_LEFT))
                        .frames(20)
                        .check("the debugger is now inspecting the tool window's UI",
                                ctx -> window().getTarget() == toolWindow(ctx).getModularUI())
                        .check("the tool UI reports itself as debugged",
                                ctx -> toolWindow(ctx).getModularUI().isDebugMode())
                        .check("the UI it left behind does not",
                                ctx -> !UIDebuggerScenario.targetUI(ctx).isDebugMode())
                        .check("the hierarchy reloaded onto the tool's tree",
                                ctx -> debugger().hierarchy.getUi() == toolWindow(ctx).getModularUI().ui)
                        .check("there is still exactly one debugger window",
                                ctx -> UIDebuggerWindow.debuggerWindows().size() == 1))

                .group("picking an element inside the tool window", g -> g
                        .step("arm the picker from the debugger window",
                                ctx -> ctx.input(window()).key(GLFW.GLFW_KEY_F1, 0))
                        .frames(5)
                        .check("focus mode is on", ctx -> debugger().isFocusMode())
                        .step("move the tool window's pointer onto its button",
                                ctx -> ctx.input(toolWindow(ctx)).moveTo(toolButton(ctx)))
                        .frames(2)
                        .check("the tool UI resolved the hover in its own coordinate space",
                                ctx -> UIDebuggerScenario.isWithin(
                                        toolWindow(ctx).getModularUI().getLastHoveredElement(), toolButton(ctx)))
                        .check("the debugger is outlining it",
                                ctx -> UIDebuggerScenario.isWithin(
                                        debugger().getShapingElement(), toolButton(ctx)))
                        .step("press inside the tool window",
                                ctx -> ctx.input(toolWindow(ctx)).mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT))
                        .step("release inside the tool window",
                                ctx -> ctx.input(toolWindow(ctx)).mouseUp(GLFW.GLFW_MOUSE_BUTTON_LEFT))
                        .frames(10)
                        .check("the debugger selected what the tool window's pointer was over",
                                ctx -> debugger().hierarchy.getSelectedOne().orElse(null)
                                        == toolWindow(ctx).getModularUI().getLastHoveredElement())
                        .check("and that was inside the tool window's button",
                                ctx -> UIDebuggerScenario.isWithin(
                                        debugger().hierarchy.getSelectedOne().orElse(null), toolButton(ctx)))
                        .check("and the button was not pressed by being inspected",
                                ctx -> toolClicks(ctx).get() == 0)
                        .screenshotSurface("01_debugger_inspecting_the_tool_window",
                                ctx -> requireSurface(window()))
                        .screenshotSurface("02_tool_window_while_picked",
                                ctx -> requireSurface(toolWindow(ctx))))

                .group("the UI the debugger left behind is its own again", g -> g
                        // Focus mode is still on, but it belongs to the tool's debugger now. The game
                        // window's UI must be back to plain behaviour, press and all.
                        .click("#pick_me")
                        .frames(5)
                        .check("the game window's button pressed normally",
                                ctx -> UIDebuggerScenario.clicks(ctx).get() == 1))

                .group("closing the tool window closes the debugger with it", g -> g
                        .step("close the tool window", ctx -> ctx.input(toolWindow(ctx)).closeRequest())
                        .frames(20)
                        .check("the tool window closed", ctx -> !toolWindow(ctx).isOpen())
                        .check("the debugger window closed with it",
                                ctx -> UIDebuggerWindow.debuggerWindows().isEmpty())
                        .check("no native window was left behind", ctx -> !OsWindowManager.hasWindows()))

                .check("no window host ever threw while being driven",
                        ctx -> OsWindowManager.totalFailures() == 0)

                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                .teardown("close the screen", ctx -> ctx.mc().gui.setScreen(null));
    }

    // ------------------------------------------------------------------------------------ fixtures

    private static UIElement toolRoot(TestContext ctx) {
        var clicks = new AtomicInteger();
        ctx.put(TOOL_CLICKS, clicks);

        var button = new Button().setText("Tool Button").setOnClick(e -> clicks.incrementAndGet());
        button.setId("tool_button").layout(layout -> layout.width(100).height(20));
        ctx.put(TOOL_BUTTON, button);

        var root = new UIElement();
        root.setId("tool_panel")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.paddingAll(20);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.FLEX_START);
                })
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF22303B)))
                .addChild(button);
        return root;
    }

    // ------------------------------------------------------------------------------------- helpers

    private static ModularUIWindow toolWindow(TestContext ctx) {
        return ctx.get(TOOL_WINDOW);
    }

    private static UIElement toolButton(TestContext ctx) {
        return ctx.get(TOOL_BUTTON);
    }

    private static AtomicInteger toolClicks(TestContext ctx) {
        return ctx.get(TOOL_CLICKS);
    }

    /**
     * The debugger window, whichever UI it currently inspects — {@code windowFor} keys on the target,
     * which is the very thing this scenario changes.
     */
    private static UIDebuggerWindow window() {
        var windows = UIDebuggerWindow.debuggerWindows();
        if (windows.size() != 1) {
            throw new IllegalStateException("Expected exactly one debugger window, found " + windows.size());
        }
        return windows.getFirst();
    }

    private static ModularUI debuggerUI() {
        return window().getModularUI();
    }

    private static UIDebugger debugger() {
        return window().getDebugger();
    }

    private static UIElement pickerEntry(TestContext ctx, String label) {
        return ctx.in(debuggerUI()).type(Button.class).withText(label).one().element();
    }

    private static com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface requireSurface(ModularUIWindow window) {
        var surface = window.surface();
        if (surface == null) {
            throw new IllegalStateException("The window has no surface yet");
        }
        return surface;
    }
}
