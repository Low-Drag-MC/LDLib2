package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowEvent;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorOsWindow;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.resources.Identifier;

/**
 * A whole editor moved out of the game window into an operating-system window of its own, and back.
 *
 * <p>What can break is the move itself: the {@link EditorWindow} tree is handed from the screen's
 * {@link ModularUI} to the window's and back, and the host being left still holds it as its root and calls
 * {@code onRemoved} on the way out. Asserted here as "the very same editor, still laid out, in the other host"
 * rather than through screenshots, which only ever show the game window.
 */
@LDLRegisterClient(name = "editor_os_window", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorOsWindowScenario implements UIScenario {

    /** Its own id, so the scenario cannot collide with a real editor's window. */
    private static final Identifier WINDOW_ID = LDLib2.id("uitest_editor_os_window");

    private static final String WINDOW = "editor_window";
    private static final String EDITOR = "editor";
    private static final String MOVED_TO = "os_window_moved_to";
    private static final String OS_UI = "os_window_modular_ui";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "window").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> {
                    var window = EditorWindow.open(WINDOW_ID, TestEditor::new);
                    ctx.put(WINDOW, window);
                    return new ModularUI(UI.of(window), ctx.player())
                            .shouldCloseOnEsc(false)
                            .shouldCloseOnKeyInventory(false);
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("remember the editor", ctx -> ctx.put(EDITOR, window(ctx).getCurrentEditor()))
                .check("no native windows are open to begin with", ctx -> !OsWindowManager.hasWindows())
                .check("native windows can be opened here", ctx -> OsWindowManager.isAvailable())

                .group("the title bar toggle moves the editor into a window of its own", g -> g
                        .waitUntil("the toggle is on show", ctx ->
                                ctx.query().select("#" + EditorWindow.HOST_TOGGLE_ID).one().element().isDisplayed())
                        // One step: a button acts on press, and this one takes the screen away before the
                        // release a separate click step would aim at it.
                        .step("click the toggle", ctx -> {
                            var bounds = ctx.query().select("#" + EditorWindow.HOST_TOGGLE_ID).one().bounds();
                            ctx.input().moveTo(bounds.centerX(), bounds.centerY());
                            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                        })
                        // created, rendered into and presented more than once
                        .frames(20)
                        .check("a native window is open", ctx -> OsWindowManager.hasWindows())
                        .check("the editor knows it is in it", ctx -> window(ctx).isInOsWindow())
                        .check("and it can be found by its id", ctx -> EditorWindow.osWindowOf(WINDOW_ID) == osWindow(ctx))
                        .check("the game window is left without a screen", ctx -> ctx.screen() == null)
                        .check("the tree moved to the window's ModularUI",
                                ctx -> window(ctx).getModularUI() == osWindow(ctx).getModularUI())
                        .check("it is still the same editor", ctx -> window(ctx).getCurrentEditor() == ctx.get(EDITOR))
                        .check("the editor is laid out in its new host", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .check("the window is titled after the editor",
                                ctx -> osWindow(ctx).getTitle().equals(editor(ctx).getTitle().getString()))
                        .check("the toggle is remembered as the preferred host", ctx -> window(ctx).prefersOsWindow())
                        .check("in its own window it fills it", ctx -> window(ctx).isMaximized())
                        .step("remember the window's UI", ctx -> ctx.put(OS_UI, osWindow(ctx).getModularUI()))
                        .screenshot("01_game_window_after_pop_out"))

                .group("opening it again focuses the window rather than building a second editor", g -> g
                        .step("show it by its id", ctx -> ctx.check("show answers null for an editor already in a window",
                                EditorWindow.show(WINDOW_ID, TestEditor::new) == null))
                        .frames(5)
                        .check("still exactly one editor window", ctx -> OsWindowManager.hosts().stream()
                                .filter(EditorOsWindow.class::isInstance).count() == 1)
                        .check("and no screen came up for a second one", ctx -> ctx.screen() == null))

                .group("docking it brings the same editor back as a screen", g -> g
                        .step("dock it into the game window", ctx ->
                                ctx.check("dockIntoGameWindow reported success", window(ctx).dockIntoGameWindow()))
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .frames(10)
                        .check("the native window closed", ctx -> !OsWindowManager.hasWindows())
                        .check("the window's UI was torn down", ctx -> ctx.<ModularUI>get(OS_UI).isRemoved())
                        .check("the editor is out of it", ctx -> !window(ctx).isInOsWindow())
                        .check("the tree is in the screen's ModularUI", ctx -> window(ctx).getModularUI() == ctx.requireUI())
                        .check("it is still the same editor", ctx -> window(ctx).getCurrentEditor() == ctx.get(EDITOR))
                        .waitUntil("the editor is laid out again", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .check("docking is remembered as the preferred host", ctx -> !window(ctx).prefersOsWindow())
                        .screenshot("02_docked"))

                .group("the window comes back where it was left", g -> g
                        .step("pop it out again", ctx -> ctx.check("popOutToOsWindow reported success",
                                window(ctx).popOutToOsWindow()))
                        .frames(20)
                        .step("move and resize it", ctx -> {
                            var os = osWindow(ctx).window();
                            os.setPosition(180, 140);
                            os.setSize(720, 480);
                        })
                        // the platform answers through callbacks, which only run when Minecraft polls
                        .frames(15)
                        .step("record where it ended up", ctx -> {
                            var os = osWindow(ctx).window();
                            ctx.put(MOVED_TO, new int[]{os.getPositionX(), os.getPositionY(),
                                    os.getWindowWidth(), os.getWindowHeight()});
                        })
                        .step("dock it", ctx -> window(ctx).dockIntoGameWindow())
                        .awaitScreen(ModularUIScreen.class)
                        .frames(10)
                        .check("the window closed", ctx -> !OsWindowManager.hasWindows())
                        .step("pop it out once more", ctx -> window(ctx).popOutToOsWindow())
                        .frames(20)
                        .check("it came back at exactly the same rectangle", ctx -> {
                            int[] before = ctx.get(MOVED_TO);
                            var os = osWindow(ctx).window();
                            return os.getPositionX() == before[0] && os.getPositionY() == before[1]
                                    && os.getWindowWidth() == before[2] && os.getWindowHeight() == before[3];
                        }))

                // The close button's path is the editor's, not the window's: the window has to outlive a
                // "save before closing?" question drawn inside it, so a CloseRequest only asks the editor.
                .group("the window's close button closes the editor", g -> g
                        .step("remember the window's UI", ctx -> ctx.put(OS_UI, osWindow(ctx).getModularUI()))
                        .step("ask the window to close, the way its close button does",
                                ctx -> osWindow(ctx).window().post(new OsWindowEvent.CloseRequest()))
                        .frames(10)
                        .check("the window closed", ctx -> !OsWindowManager.hasWindows())
                        .check("its UI was torn down with the editor in it", ctx -> ctx.<ModularUI>get(OS_UI).isRemoved())
                        .check("the editor closed with it", ctx -> window(ctx).getEditors().isEmpty())
                        .check("and nothing came up in the game window instead", ctx -> ctx.screen() == null))

                .check("no window host ever threw while being driven", ctx -> OsWindowManager.totalFailures() == 0)

                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                .closeScreen();
    }

    private static EditorWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static Editor editor(TestContext ctx) {
        var editor = window(ctx).getCurrentEditor();
        if (editor == null) {
            throw new IllegalStateException("The editor window has no editor");
        }
        return editor;
    }

    private static EditorOsWindow osWindow(TestContext ctx) {
        var osWindow = window(ctx).getOsWindow();
        if (osWindow == null || !osWindow.isOpen()) {
            throw new IllegalStateException("The editor is not in a native window");
        }
        return osWindow;
    }
}
