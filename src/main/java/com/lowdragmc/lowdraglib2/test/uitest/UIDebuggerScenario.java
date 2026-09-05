package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.gui.holder.DebugScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebugger;
import com.lowdragmc.lowdraglib2.gui.ui.debugger.UIDebuggerWindow;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The debugger's default host, and the trip to the other one and back.
 *
 * <p>F12 opens it as a {@link DebugScreen} layered over the game — one keypress, nothing to know — and
 * a toggle in its title bar moves it into an OS window of its own. Both hosts have to be able to do
 * the whole job, and the switch has to be lossless in both directions: the debugger is a single
 * element tree that is handed from one host to the other, so a mistake in the order of adoption and
 * teardown either drops it on the floor or leaves it parented to a screen that has gone.
 *
 * <p>The picker is the behaviour under test in each host. In focus mode a press must select the
 * element under the pointer and <em>not</em> activate it — a button with a counter behind it is the
 * only way to tell those two apart from outside — and that must stop the moment focus mode does.
 *
 * @see UIDebuggerWindowScenario for what the window host adds
 * @see UIDebuggerMultiWindowScenario for a third window in the mix
 */
@LDLRegisterClient(name = "ui_debugger", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class UIDebuggerScenario implements UIScenario {

    static final String TARGET_UI = "debugger_target_ui";
    static final String BUTTON = "debugger_target_button";
    static final String CLICKS = "debugger_target_clicks";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("debugger", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        openTarget(s)
                .check("no native windows are open to begin with", ctx -> !OsWindowManager.hasWindows())
                .check("the UI does not think it is being debugged", ctx -> !targetUI(ctx).isDebugMode())
                .screenshot("01_before_debugger")

                .group("F12 opens it over the game", g -> g
                        .key(GLFW.GLFW_KEY_F12)
                        .frames(10)
                        .check("the UI is in debug mode", ctx -> targetUI(ctx).isDebugMode())
                        .checkScreen(DebugScreen.class)
                        .check("no native window was opened", ctx -> !OsWindowManager.hasWindows())
                        .check("the debugger floats over the UI", ctx -> debugger(ctx).isFloating())
                        // Its own UI, layered over the inspected one - which is why every step below
                        // names the UI it means rather than relying on "the open screen's".
                        .check("it lives in the debug screen's UI",
                                ctx -> ctx.query().type(UIDebugger.class).count() == 1
                                        && ctx.requireUI() != targetUI(ctx))
                        .screenshot("02_debug_screen_over_the_ui"))

                .group("picking works through the layer's forwarding", g -> g
                        .key(GLFW.GLFW_KEY_F1)
                        .frames(5)
                        .check("focus mode is on", ctx -> debugger(ctx).isFocusMode())
                        .step("move onto the button", ctx -> moveTo(ctx, button(ctx)))
                        // The inspected UI's hover is only recomputed while it renders, one layer down
                        // and with the pointer substituted back in - so it needs a frame to settle.
                        .frames(3)
                        .check("the inspected UI resolved the hover under the layer",
                                ctx -> isWithin(targetUI(ctx).getLastHoveredElement(), button(ctx)))
                        .check("the debugger is outlining it",
                                ctx -> isWithin(debugger(ctx).getShapingElement(), button(ctx)))
                        .step("press", UIDebuggerScenario::press)
                        .step("release", UIDebuggerScenario::release)
                        .frames(5)
                        .check("the debugger selected what the pointer was over",
                                ctx -> debugger(ctx).hierarchy.getSelectedOne().orElse(null)
                                        == targetUI(ctx).getLastHoveredElement())
                        .check("and the button was not pressed by being inspected",
                                ctx -> clicks(ctx).get() == 0)
                        .screenshot("03_picking_through_the_layer")
                        .key(GLFW.GLFW_KEY_F1)
                        .frames(3)
                        .check("focus mode is off", ctx -> !debugger(ctx).isFocusMode()))

                .group("the title bar toggle moves it into a window", g -> g
                        .step("aim at the window toggle", ctx -> moveTo(ctx, debugger(ctx).windowModeToggle))
                        .frames(2)
                        .step("click it", UIDebuggerScenario::click)
                        // The switch is deferred by design - it reparents the toggle's own ancestor,
                        // so it cannot happen inside the toggle's click dispatch.
                        .frames(20)
                        .check("a debugger window is open", ctx -> debuggerWindow(ctx) != null)
                        .check("the debug screen layer is gone",
                                ctx -> !(ctx.screen() instanceof DebugScreen))
                        // The whole reason for the window: the UI being inspected is back in front,
                        // uncovered, and still the one the screen shows.
                        .checkScreen(ModularUIScreen.class)
                        .check("the inspected UI is the screen's again",
                                ctx -> ctx.requireUI() == targetUI(ctx))
                        .check("it is still the same debugger, still debugging",
                                ctx -> targetUI(ctx).isDebugMode()
                                        && window(ctx).getDebugger() == debugger(ctx))
                        .check("the debugger stopped floating", ctx -> !debugger(ctx).isFloating())
                        .check("and fills the window edge to edge", ctx -> {
                            var surface = requireSurface(ctx);
                            return debugger(ctx).getSizeWidth() >= surface.guiScaledWidth() - 1
                                    && debugger(ctx).getSizeHeight() >= surface.guiScaledHeight() - 1;
                        })
                        .check("the hierarchy is still on the inspected UI",
                                ctx -> debugger(ctx).hierarchy.getUi() == targetUI(ctx).ui)
                        .screenshot("04_game_window_uncovered")
                        .screenshotSurface("05_debugger_window", UIDebuggerScenario::requireSurface))

                .group("and back again", g -> g
                        .step("aim at the window toggle in the window",
                                ctx -> ctx.input(window(ctx)).moveTo(debugger(ctx).windowModeToggle))
                        .frames(2)
                        // One step, both events: the toggle acts on the press and the window is gone
                        // by the next frame, so a release of its own would have nothing to land in.
                        .step("click it", ctx -> ctx.input(window(ctx))
                                .mouseDown(Keys.MOUSE_LEFT).mouseUp(Keys.MOUSE_LEFT))
                        .frames(20)
                        .check("the window closed", ctx -> !OsWindowManager.hasWindows())
                        .checkScreen(DebugScreen.class)
                        .check("the debugger is floating over the game again",
                                ctx -> debugger(ctx).isFloating())
                        .check("and is still debugging the same UI",
                                ctx -> targetUI(ctx).isDebugMode()
                                        && debugger(ctx).hierarchy.getUi() == targetUI(ctx).ui))

                .group("F12 closes it", g -> g
                        .key(GLFW.GLFW_KEY_F12)
                        .frames(5)
                        .check("the debug screen is gone", ctx -> !(ctx.screen() instanceof DebugScreen))
                        // Popping the layer, not the stack: the inspected UI must survive its inspector.
                        .checkScreen(ModularUIScreen.class)
                        .check("the inspected UI is still open", ctx -> ctx.requireUI() == targetUI(ctx))
                        .check("the UI is out of debug mode", ctx -> !targetUI(ctx).isDebugMode()))

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

    /** Opens the inspected UI and stashes what the other debugger scenarios need from it. */
    static ScenarioBuilder openTarget(ScenarioBuilder s) {
        return s.openModularUI("target", ctx -> new ModularUI(UI.of(targetRoot(ctx)), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .step("remember the inspected UI", ctx -> ctx.put(TARGET_UI, ctx.requireUI()))
                .waitUntil("the button has laid out", ctx -> button(ctx).getSizeWidth() > 0);
    }

    /**
     * A full-screen opaque panel with a button pinned to the bottom right.
     *
     * <p>Opaque and full-screen because the pixel checks in {@link UIDebuggerWindowScenario} sample
     * the game's frame, and a translucent UI would have the world — clouds, the sun, a passing mob —
     * answering for them. Bottom right because the floating debugger panel lays out at the top left,
     * and a target underneath it would be answering for the panel rather than the UI.
     */
    static UIElement targetRoot(TestContext ctx) {
        var clicks = new AtomicInteger();
        ctx.put(CLICKS, clicks);

        var button = new Button().setText("Pick Me").setOnClick(e -> clicks.incrementAndGet());
        button.setId("pick_me").layout(layout -> layout.width(80).height(20));
        ctx.put(BUTTON, button);

        var root = new UIElement();
        root.setId("panel")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.paddingAll(20);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                    layout.alignItems(AlignItems.FLEX_END);
                })
                .style(style -> style.backgroundTexture(new ColorRectTexture(0xFF1B1B22)))
                .addChild(button);
        return root;
    }

    // ------------------------------------------------------------------------------------- helpers

    static ModularUI targetUI(TestContext ctx) {
        return ctx.get(TARGET_UI);
    }

    static UIElement button(TestContext ctx) {
        return ctx.get(BUTTON);
    }

    static AtomicInteger clicks(TestContext ctx) {
        return ctx.get(CLICKS);
    }

    @Nullable
    static UIDebuggerWindow debuggerWindow(TestContext ctx) {
        return UIDebuggerWindow.windowFor(targetUI(ctx));
    }

    /** The debugger window, or a hard failure — for steps that have no meaning without one. */
    static UIDebuggerWindow window(TestContext ctx) {
        var window = debuggerWindow(ctx);
        if (window == null) {
            throw new IllegalStateException("No debugger window is inspecting the target UI");
        }
        return window;
    }

    /**
     * The debugger, wherever it currently lives. Read off the target rather than found by selector,
     * because which UI it is registered in is exactly what these tests are moving around — and
     * through the getter that creates nothing, so asking cannot be what turns debug mode on.
     */
    static UIDebugger debugger(TestContext ctx) {
        var debugger = ModularUIClientAccess.getUiDebugger(targetUI(ctx));
        if (debugger == null) {
            throw new IllegalStateException("The target UI has no debugger");
        }
        return debugger;
    }

    static com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface requireSurface(TestContext ctx) {
        var surface = window(ctx).surface();
        if (surface == null) {
            throw new IllegalStateException("The debugger window has no surface yet");
        }
        return surface;
    }

    /** Whether {@code element} is {@code ancestor} or sits under it. */
    static boolean isWithin(@Nullable UIElement element, UIElement ancestor) {
        return element != null && element.getStructurePath().contains(ancestor);
    }

    /**
     * Aims the game window's pointer at an element of any UI drawn there.
     *
     * <p>Not {@code hover(selector)}: that resolves against the open screen's UI, and half of these
     * steps target a UI sitting one screen layer underneath it.
     */
    static void moveTo(TestContext ctx, UIElement element) {
        ctx.put(AIMED, element);
        var bounds = ElementBounds.of(element);
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
    }

    static void press(TestContext ctx) {
        var bounds = ElementBounds.of(lastAimed(ctx));
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    static void release(TestContext ctx) {
        var bounds = ElementBounds.of(lastAimed(ctx));
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    /**
     * Press and release in one step, for a control whose own action tears down the host it lives in.
     *
     * <p>The window toggle is the case: it fires on MOUSE_DOWN, and the host switch it asks for is
     * applied on the very next frame — so a release left to a step of its own arrives at a window
     * that has already closed, or at whatever took the layer's place.
     */
    static void click(TestContext ctx) {
        press(ctx);
        release(ctx);
    }

    private static final String AIMED = "debugger_aimed_element";

    private static UIElement lastAimed(TestContext ctx) {
        UIElement element = ctx.get(AIMED);
        if (element == null) {
            throw new IllegalStateException("Nothing was aimed at; call moveTo first");
        }
        return element;
    }
}
