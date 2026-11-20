package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorActions;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.keymap.Keymap;
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
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * An editor in the game window gives the game its own gui scale, and whatever takes it off screen puts
 * the game's back — not only its close button.
 */
@LDLRegisterClient(name = "editor_close_gui_scale", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorCloseGuiScaleScenario implements UIScenario {

    private static final String WINDOW = "editor_window";
    private static final ResourceLocation MINIMIZABLE_ID = LDLib2.id("uitest_editor_close_gui_scale");
    private static final int GAME_SCALE = 2;
    private static final int EDITOR_SCALE = 1;
    private static final int PLAYER_SCALE = 3;

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "window", "scale").guiScale(GAME_SCALE);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.group("Esc bound to closing the editor, in the screen LDLib2 hosts it in", g -> g
                        .step("start from the game's own scale", ctx -> setGameScale(ctx, GAME_SCALE))
                        .step("open an editor window", ctx -> {
                            var window = new EditorWindow(TestEditor::new);
                            ctx.put(WINDOW, window);
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .step("give the editor a scale of its own", ctx -> setGameScale(ctx, EDITOR_SCALE))
                        // live keymap only: nothing presses Apply, so the settings file is left alone
                        .step("bind Esc to closing the editor", ctx -> editor(ctx).getKeymap().setBindings(
                                EditorActions.CLOSE_EDITOR,
                                Keymap.Bindings.of(KeyChord.key(GLFW.GLFW_KEY_ESCAPE), KeyChord.UNBOUND)))
                        .step("focus the editor", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_ESCAPE)
                        .settleMs(150)
                        .check("the editor closed", ctx -> ctx.screen() == null)
                        .check("the game's scale is back", ctx -> gameScale(ctx) == GAME_SCALE))

                // What a mod gets from the obvious way of showing one: ModularUI closes on Esc by default.
                .group("Esc in a plain ModularUI, which closes the screen itself", g -> g
                        .step("start from the game's own scale", ctx -> setGameScale(ctx, GAME_SCALE))
                        .openModularUI("an editor window in a plain ModularUI", ctx -> {
                            var window = new EditorWindow(TestEditor::new);
                            ctx.put(WINDOW, window);
                            return new ModularUI(UI.of(window), ctx.player());
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .step("give the editor a scale of its own", ctx -> setGameScale(ctx, EDITOR_SCALE))
                        .step("focus the editor", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_ESCAPE)
                        .settleMs(150)
                        .check("the screen closed", ctx -> ctx.screen() == null)
                        .check("the game's scale is back", ctx -> gameScale(ctx) == GAME_SCALE)
                        .check("and the game window is laid out at it", ctx -> ctx.mc().getWindow().getGuiScale() == GAME_SCALE))

                // a server closing the menu, another mod opening its own screen
                .group("the screen replaced from outside", g -> g
                        .step("start from the game's own scale", ctx -> setGameScale(ctx, GAME_SCALE))
                        .step("open an editor window", ctx -> {
                            var window = new EditorWindow(TestEditor::new);
                            ctx.put(WINDOW, window);
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .step("give the editor a scale of its own", ctx -> setGameScale(ctx, EDITOR_SCALE))
                        .step("close the screen", ctx -> ctx.mc().setScreen(null))
                        .settleMs(150)
                        .check("the game's scale is back", ctx -> gameScale(ctx) == GAME_SCALE)
                        .check("and the game window is laid out at it", ctx -> ctx.mc().getWindow().getGuiScale() == GAME_SCALE))

                .group("minimizing gives the scale back, and so does closing it once shown again", g -> g
                        .step("start from the game's own scale", ctx -> setGameScale(ctx, GAME_SCALE))
                        .step("open an editor window", ctx -> {
                            var window = new EditorWindow(MINIMIZABLE_ID, TestEditor::new);
                            ctx.put(WINDOW, window);
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .step("give the editor a scale of its own", ctx -> setGameScale(ctx, EDITOR_SCALE))
                        .step("minimize it", ctx -> window(ctx).minimizeWindow())
                        .settleMs(150)
                        .check("the screen closed", ctx -> ctx.screen() == null)
                        .check("the game's scale is back", ctx -> gameScale(ctx) == GAME_SCALE)
                        .step("show it again", ctx -> {
                            var window = EditorWindow.open(MINIMIZABLE_ID, TestEditor::new);
                            ctx.check("the minimized window came back", window == window(ctx));
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .step("close it", ctx -> window(ctx).closeWindow())
                        .settleMs(150)
                        .check("the screen closed", ctx -> ctx.screen() == null)
                        .check("the game's scale is still the game's", ctx -> gameScale(ctx) == GAME_SCALE))

                // in a window of its own the editor never touched the game's scale, so whatever the player
                // set it to meanwhile is theirs to keep
                .group("closing an editor in its own window leaves the game's scale alone", g -> g
                        .step("start from the game's own scale", ctx -> setGameScale(ctx, GAME_SCALE))
                        .step("open an editor window", ctx -> {
                            var window = new EditorWindow(TestEditor::new);
                            ctx.put(WINDOW, window);
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                        .step("pop it out", ctx -> ctx.check("popped out", window(ctx).popOutToOsWindow()))
                        .frames(20)
                        .check("it is in its own window", ctx -> window(ctx).isInOsWindow())
                        .step("the player picks a scale of their own", ctx -> setGameScale(ctx, PLAYER_SCALE))
                        .step("close it", ctx -> window(ctx).closeWindow())
                        .frames(20)
                        .check("the window is gone", ctx -> !window(ctx).isInOsWindow())
                        .check("the player's scale stands", ctx -> gameScale(ctx) == PLAYER_SCALE))

                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                // showing the minimized editor again toasts, which a later scenario would see
                .teardown("clear the toasts", ctx -> ctx.mc().getToasts().clear())
                .teardown("close the screen", ctx -> ctx.mc().setScreen(null))
                .teardown("put the game's scale back", ctx -> setGameScale(ctx, GAME_SCALE));
    }

    private static void setGameScale(TestContext ctx, int scale) {
        var options = ctx.mc().options;
        if (options.guiScale().get() != scale) {
            options.guiScale().set(scale);
            ctx.mc().resizeDisplay();
        }
    }

    private static int gameScale(TestContext ctx) {
        return ctx.mc().options.guiScale().get();
    }

    private static EditorWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static Editor editor(TestContext ctx) {
        var editor = window(ctx).getCurrentEditor();
        if (editor == null) throw new IllegalStateException("The editor window has no editor");
        return editor;
    }
}
