package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowEvent;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingViewWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import org.lwjgl.glfw.GLFW;

/**
 * Tears a view out of the editor into a real operating-system window and puts it back.
 *
 * <p>What this exercises cannot be seen in the screenshots — they only ever show the game window —
 * but it is the part most likely to break: creating a second GLFW window whose context shares
 * objects with Minecraft's, building capabilities for it, rendering the view into an off-screen
 * target on Minecraft's context, blitting that texture across the context boundary, and tearing all
 * of it down again. Any of that going wrong surfaces here as a failed check or a dead client rather
 * than as something a user finds later.
 *
 * <p>The frames after floating matter as much as the assertions: they prove Minecraft's own frame is
 * undisturbed, i.e. that the render target binding and viewport were put back.
 */
@LDLRegisterClient(name = "floating_view", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class FloatingViewScenario implements UIScenario {

    private static final String DOCKED_BACKGROUND = "docked_code_editor_background";
    private static final String PANE_ON_DESKTOP = "inspector_pane_on_desktop";
    private static final String MOVED_TO = "inspector_window_moved_to";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "dock", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .check("no native windows are open to begin with", ctx -> !OsWindowManager.hasWindows())
                .screenshot("01_before_float")

                // First, while nothing has been floated yet and there is therefore nothing remembered.
                .group("a view floated for the first time comes off its own pane", g -> g
                        .step("note where the pane is on the desktop",
                                ctx -> ctx.put(PANE_ON_DESKTOP, paneOnDesktop(ctx, editor(ctx).inspectorView)))
                        .step("float the inspector",
                                ctx -> ctx.check("floatView reported success",
                                        editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView)))
                        .frames(20)
                        // Size in window pixels, so this fails if the GUI scale is ever dropped from
                        // the conversion — a 2x-too-small window is the shape that mistake takes.
                        .check("the window is the size the pane was", ctx -> {
                            int[] pane = ctx.get(PANE_ON_DESKTOP);
                            var os = inspectorWindow(ctx).window();
                            return near(os.getWindowWidth(), pane[2], 4) && near(os.getWindowHeight(), pane[3], 4);
                        })
                        // And near where the pane was, which fails if the game window's own desktop
                        // position is ever dropped — that mistake puts it at the top-left of the
                        // screen. Loose, because the exact nudge is not what is being pinned.
                        .check("and sits over where the pane was, give or take a nudge", ctx -> {
                            int[] pane = ctx.get(PANE_ON_DESKTOP);
                            var os = inspectorWindow(ctx).window();
                            return near(os.getPositionX(), pane[0], 64) && near(os.getPositionY(), pane[1], 64);
                        }))

                .group("and remembers where it was left the next time", g -> g
                        .step("move and resize it", ctx -> {
                            var os = inspectorWindow(ctx).window();
                            os.setPosition(180, 140);
                            os.setSize(520, 380);
                        })
                        // The platform answers through callbacks, which only run when Minecraft polls,
                        // so what is recorded is what it actually did rather than what it was asked.
                        .frames(15)
                        .step("record where it ended up", ctx -> {
                            var os = inspectorWindow(ctx).window();
                            ctx.put(MOVED_TO, new int[]{os.getPositionX(), os.getPositionY(),
                                    os.getWindowWidth(), os.getWindowHeight()});
                        })
                        .step("dock it back", ctx ->
                                editor(ctx).getFloatingViews().dockBack(editor(ctx).inspectorView))
                        .frames(15)
                        .check("the window closed", ctx -> !OsWindowManager.hasWindows())
                        .step("float it again", ctx ->
                                editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView))
                        .frames(20)
                        .check("it came back at exactly the same rectangle", ctx -> {
                            int[] before = ctx.get(MOVED_TO);
                            var os = inspectorWindow(ctx).window();
                            return os.getPositionX() == before[0] && os.getPositionY() == before[1]
                                    && os.getWindowWidth() == before[2] && os.getWindowHeight() == before[3];
                        })
                        .step("dock it back again, so the groups below start from the docked state",
                                ctx -> editor(ctx).getFloatingViews().dockBack(editor(ctx).inspectorView))
                        .frames(15)
                        .check("no native window was left behind", ctx -> !OsWindowManager.hasWindows()))

                .group("float a view", g -> g
                        .step("float the inspector", ctx -> {
                            var editor = editor(ctx);
                            ctx.require("the inspector is docked",
                                    editor.rootWindow.getAllViews().contains(editor.inspectorView));
                            ctx.check("floatView reported success",
                                    editor.getFloatingViews().floatView(editor.inspectorView));
                        })
                        // Several frames, so the window is created, rendered into and presented more
                        // than once. A one-frame check would miss anything that only fails on reuse.
                        .frames(20)
                        .check("a native window is open", ctx -> OsWindowManager.hasWindows())
                        .check("the inspector left the dock tree",
                                ctx -> !editor(ctx).rootWindow.getAllViews().contains(editor(ctx).inspectorView))
                        .check("the editor still counts the inspector among its views",
                                ctx -> editor(ctx).getAllViews().contains(editor(ctx).inspectorView))
                        .check("the inspector moved to a different ModularUI",
                                ctx -> editor(ctx).inspectorView.getModularUI() != ctx.requireUI())
                        .check("the floated view can still find its editor",
                                ctx -> editor(ctx).inspectorView.getEditor() == editor(ctx))
                        // The game window is what gets captured, so this is the check that the
                        // floating render put Minecraft's own target and viewport back.
                        .screenshot("02_after_float"))

                .group("dock it back", g -> g
                        .step("dock the inspector back", ctx ->
                                editor(ctx).getFloatingViews().dockBack(editor(ctx).inspectorView))
                        .frames(10)
                        .check("the native window closed", ctx -> !OsWindowManager.hasWindows())
                        .check("the inspector is docked again",
                                ctx -> editor(ctx).rootWindow.getAllViews().contains(editor(ctx).inspectorView))
                        .check("the inspector is back in the editor's ModularUI",
                                ctx -> editor(ctx).inspectorView.getModularUI() == ctx.requireUI())
                        .screenshot("03_after_dock_back"))

                // The close button's own path: the request is delivered through the window's event
                // queue, so the close runs inside drainInput and the manager then has to carry on
                // driving a host whose window it has just destroyed. Getting that ordering wrong is
                // invisible from the outside — the throw is swallowed — hence the failure counter.
                .group("the close button tears the window down cleanly", g -> g
                        .step("float the inspector again", ctx ->
                                editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView))
                        .frames(10)
                        .check("a native window is open again", ctx -> OsWindowManager.hasWindows())
                        .step("ask the window to close, the way its close button does", ctx -> {
                            var window = editor(ctx).getFloatingViews().windowOf(editor(ctx).inspectorView);
                            ctx.require("the inspector is in a native window", window != null);
                            window.window().post(new OsWindowEvent.CloseRequest());
                        })
                        .frames(10)
                        .check("the window closed", ctx -> !OsWindowManager.hasWindows())
                        .check("the inspector came home",
                                ctx -> editor(ctx).rootWindow.getAllViews().contains(editor(ctx).inspectorView)))

                .group("styling follows the editor into the window", g -> g
                        .step("float the inspector once more", ctx ->
                                editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView))
                        .frames(10)
                        .check("the floating UI has the editor's stylesheets", ctx -> {
                            var window = editor(ctx).getFloatingViews().windowOf(editor(ctx).inspectorView);
                            if (window == null) return false;
                            var editorSheets = ctx.requireUI().getStyleEngine().globalSheets;
                            var floatSheets = window.getModularUI().getStyleEngine().globalSheets;
                            return floatSheets.containsAll(editorSheets);
                        })
                        // Sheets alone are not enough: the themes scope editor styling under the
                        // editor element's classes, so the floating root has to stand in for it or
                        // every `.__editor__ x` rule silently stops matching.
                        .check("the floating root carries the editor's style classes", ctx -> {
                            var window = editor(ctx).getFloatingViews().windowOf(editor(ctx).inspectorView);
                            if (window == null) return false;
                            return window.getRoot().getClasses().containsAll(editor(ctx).getClasses());
                        }))

                // The reported case: a code editor is styled by `.__editor__ code-editor` in every
                // built-in theme, so floating it out from under the editor element used to strip its
                // background entirely.
                .group("a floated view keeps theme rules scoped to the editor", g -> g
                        .step("remember the code editor's docked background", ctx -> {
                            var codeEditor = ((TestEditor) editor(ctx)).stylesheetEditor;
                            var background = codeEditor.getStyle().backgroundTexture();
                            ctx.check("the docked code editor is themed at all",
                                    background != null && background != IGuiTexture.EMPTY);
                            ctx.put(DOCKED_BACKGROUND, background);
                        })
                        .step("float the view holding the code editor", ctx -> {
                            var view = ((TestEditor) editor(ctx)).stylesheetEditor.getFirstAncestorOfType(View.class);
                            ctx.require("the code editor lives in a view", view != null);
                            ctx.check("floatView reported success",
                                    editor(ctx).getFloatingViews().floatView(view));
                        })
                        .frames(20)
                        .check("the code editor kept its themed background", ctx ->
                                ((TestEditor) editor(ctx)).stylesheetEditor.getStyle().backgroundTexture()
                                        == ctx.get(DOCKED_BACKGROUND)))

                // Closing the editor while a view is still floating is the path that leaks a real
                // window onto the user's desktop if the teardown is wrong.
                .group("closing the screen takes any floating window with it", g -> g
                        .closeScreen()
                        .frames(10)
                        .check("closing the editor closed the native window",
                                ctx -> !OsWindowManager.hasWindows()))

                .check("no window host ever threw while being driven",
                        ctx -> OsWindowManager.totalFailures() == 0)

                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                });
    }

    private static Editor editor(TestContext ctx) {
        return EditorPaneMaximizeScenario.editor(ctx);
    }

    private static FloatingViewWindow inspectorWindow(TestContext ctx) {
        var window = editor(ctx).getFloatingViews().windowOf(editor(ctx).inspectorView);
        if (window == null || !window.isOpen()) {
            throw new IllegalStateException("The inspector is not in a native window");
        }
        return window;
    }

    /**
     * A view's pane as a desktop rectangle {@code {x, y, width, height}} — the space a window is
     * positioned and sized in.
     *
     * <p>Restated here rather than shared with the production code on purpose: the point of the two
     * checks above is that the conversion is done <em>at all</em>, and a test that called the same
     * helper would agree with it however wrong it was.
     */
    private static int[] paneOnDesktop(TestContext ctx, View view) {
        var pane = view.getViewContainer();
        ctx.require("the view is in a pane", pane != null);
        var topLeft = pane.getWorldMouse(pane.getPositionX(), pane.getPositionY());
        var scale = ctx.mc().getWindow().getGuiScale();
        var originX = new int[1];
        var originY = new int[1];
        GLFW.glfwGetWindowPos(ctx.mc().getWindow().handle(), originX, originY);
        return new int[]{
                originX[0] + (int) (topLeft.x * scale),
                originY[0] + (int) (topLeft.y * scale),
                (int) (pane.getSizeWidth() * scale),
                (int) (pane.getSizeHeight() * scale)};
    }

    private static boolean near(int actual, int expected, int tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }


}
