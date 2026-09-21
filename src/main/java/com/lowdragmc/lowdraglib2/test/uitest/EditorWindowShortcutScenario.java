package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.keymap.EditorActions;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.settings.KeymapSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
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
import org.lwjgl.glfw.GLFW;

/**
 * The window actions, pressed as keys on a window that has no id.
 *
 * <p>Such a window cannot be minimized — minimized windows are parked in a map keyed by that id — and
 * its title bar leaves the button out. A keymap action has no such thing as a hidden button, so it used
 * to call the method anyway and take the game down with
 * {@code NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null} from deep inside
 * a {@code ConcurrentHashMap}. Maximize, which has no such precondition, must still work.
 */
@LDLRegisterClient(name = "editor_window_shortcuts", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorWindowShortcutScenario implements UIScenario {

    private static final String WINDOW = "editor_window";
    /** What the editor ships with, so this drives the real default rather than a chord of its own. */
    private static final KeyChord MINIMIZE = KeyChord.ctrlAlt(GLFW.GLFW_KEY_DOWN);
    private static final KeyChord MAXIMIZE = KeyChord.ctrlAlt(GLFW.GLFW_KEY_UP);

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "keymap", "window");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("an editor window with no id", ctx -> {
                    // the constructor that leaves windowID null - the one a caller gets by default
                    var window = new EditorWindow(TestEditor::new);
                    ctx.put(WINDOW, window);
                    return new ModularUI(UI.of(window), ctx.player());
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .check("this window really has no id", ctx -> window(ctx).windowID == null)
                .check("so it says it cannot be minimized", ctx -> !window(ctx).canMinimize())

                // A dev's own config lives in the run directory and would otherwise decide what these
                // chords are. Overrides are dropped from the live keymap only - nothing is written back,
                // because nothing here presses Apply.
                .step("start from the shipped defaults", ctx -> {
                    var editor = editor(ctx);
                    var settings = KeymapSettings.of(editor);
                    settings.getActions().forEach(settings::reset);
                    editor.getEditorSettings().applyCurrentSettings();
                })
                .check("minimize is bound out of the box", ctx -> MINIMIZE.equals(
                        editor(ctx).getKeymap().bindingsOf(EditorActions.MINIMIZE_WINDOW).primary()))
                .check("and so is maximize", ctx -> MAXIMIZE.equals(
                        editor(ctx).getKeymap().bindingsOf(EditorActions.MAXIMIZE_WINDOW).primary()))
                .step("focus the editor", ctx -> editor(ctx).focus())

                .group("minimizing a window that cannot be minimized does nothing", g -> g
                        // the crash: the press itself used to throw out of keyPressed
                        .key(GLFW.GLFW_KEY_DOWN, Keys.MOD_CONTROL | Keys.MOD_ALT)
                        .settleMs(100)
                        .check("the screen is still up", ctx -> ctx.screen() != null)
                        .check("and the editor is still in it", ctx -> ctx.query().type(Editor.class).count() == 1))

                .group("maximize toggles", g -> g
                        .check("it starts maximized", ctx -> window(ctx).isMaximized())
                        .key(GLFW.GLFW_KEY_UP, Keys.MOD_CONTROL | Keys.MOD_ALT)
                        .settleMs(150)
                        .check("the chord restored it", ctx -> !window(ctx).isMaximized())
                        .screenshot("01_restored")
                        .step("focus the editor again", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_UP, Keys.MOD_CONTROL | Keys.MOD_ALT)
                        .settleMs(150)
                        .check("and again maximized it", ctx -> window(ctx).isMaximized()))

                .closeScreen();
    }

    private static EditorWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }
}
