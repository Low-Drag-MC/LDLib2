package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.io.File;

/**
 * The save notification closes itself, including when the editor is minimized while its progress bar
 * is still filling.
 *
 * <p>That second case is the one worth a client run. {@code Dialog#showNotification(String, float)} has
 * no button and does not auto close, so the only thing that ever takes it off screen is the animation
 * on its progress bar reaching the end — and an animation lives in the {@code AnimationEngine} of the
 * {@link ModularUI} that started it. {@link EditorWindow} survives being minimized by keeping the whole
 * element tree in a static map and handing it back inside a <em>new</em> {@code ModularUI}, so the
 * notification came back attached to that tree with its animation stranded in an engine nobody updates
 * again. It could then never be dismissed by any means at all.
 *
 * <p>The reuse is asserted rather than assumed: same {@code EditorWindow} instance, different
 * {@code ModularUI}, and the dialog still hanging off the reused tree at the moment it is re-hosted.
 * Without those three the run could go green while testing nothing.
 */
@LDLRegisterClient(name = "editor_save_notification", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorSaveNotificationScenario implements UIScenario {

    /** Its own id, so the scenario cannot collide with a real editor's minimized window. */
    private static final ResourceLocation WINDOW_ID = LDLib2.id("uitest_save_notification");

    private static final String WINDOW = "editor_window";
    private static final String REOPENED_WINDOW = "reopened_editor_window";
    private static final String FIRST_UI = "first_modular_ui";
    private static final String PROJECT_FILE = "project_file";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "dialog", "animation");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> {
                    var window = EditorWindow.open(WINDOW_ID, TestEditor::new);
                    ctx.put(WINDOW, window);
                    return new ModularUI(UI.of(window), ctx.player());
                })
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                // A previous run that failed between minimizing and reopening would leave its window in
                // EditorWindow's static map, and this one would then pick that up - notification and all.
                // Fail here rather than let the assertions below quietly test the leftovers.
                .check("no dialog is up to begin with", ctx -> ctx.count("dialog") == 0)
                .step("remember the UI this tree started in", ctx -> ctx.put(FIRST_UI, ctx.requireUI()))
                .step("load a project that already has a file to save to", ctx -> {
                    var file = new File(ctx.mc().gameDirectory, "ldlib2-uitest/save-notification.test.nbt");
                    ctx.require("a place to save to", file.getParentFile().isDirectory()
                            || file.getParentFile().mkdirs());
                    ctx.put(PROJECT_FILE, file);
                    var project = new TestProject();
                    project.initNewProject();
                    editor(ctx).loadProject(project, file);
                })
                .check("the editor has a project and somewhere to save it",
                        ctx -> editor(ctx).getCurrentProject() != null
                                && editor(ctx).getCurrentProjectFile() != null)

                .group("ctrl+s notifies, and the notification closes itself", g -> g
                        // Nothing focused, so the chord goes down the validate/execute-command path that
                        // reaches the Editor, rather than being delivered to whatever holds focus.
                        .blur()
                        .keyDown(Keys.LEFT_CONTROL)
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .keyUp(Keys.LEFT_CONTROL)
                        .waitUntil("the notification is up", ctx -> ctx.count("dialog") > 0)
                        .check("it is the progress bar kind", ctx -> ctx.count(".__dialog_progress-bar__") > 0)
                        .check("the project was actually written",
                                ctx -> ctx.<File>get(PROJECT_FILE).isFile())
                        .screenshot("01_notification")
                        .waitUntil("it goes away on its own", ctx -> ctx.count("dialog") == 0))

                .group("minimize while the bar is still filling", g -> g
                        // Straight to saveProject: the chord is covered above, and racing a two second
                        // bar is not something to leave to key routing.
                        .step("save again", ctx -> editor(ctx).saveProject(null))
                        .waitUntil("the notification is up", ctx -> ctx.count("dialog") > 0)
                        .check("the bar is still on screen", ctx -> ctx.count(".__dialog_progress-bar__") > 0)
                        .screenshot("02_before_minimize")
                        .step("minimize the editor window",
                                ctx -> ctx.<EditorWindow>get(WINDOW).minimizeWindow())
                        .waitUntil("the editor screen closed", ctx -> ctx.screen() == null))

                .group("reopening hands the tree to a new ModularUI", g -> g
                        .step("reopen the minimized window", ctx -> {
                            var window = EditorWindow.open(WINDOW_ID, TestEditor::new);
                            ctx.put(REOPENED_WINDOW, window);
                            // Checked here, before the new screen exists: this is the only moment the
                            // stranded notification is observable for certain. Once the UI is running
                            // it is supposed to disappear, and it does so within a frame or two.
                            ctx.check("the notification came back attached to the reused tree",
                                    window.getChildren().stream().anyMatch(Dialog.class::isInstance));
                            ctx.mc().setScreen(new ModularUIScreen(
                                    new ModularUI(UI.of(window), ctx.player()), Component.empty()));
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .check("the very same window came back",
                                ctx -> ctx.get(REOPENED_WINDOW) == ctx.<EditorWindow>get(WINDOW))
                        .check("wrapped in a different ModularUI",
                                ctx -> ctx.requireUI() != ctx.<ModularUI>get(FIRST_UI))
                        // The regression. Before the animation was handed to the new engine this never
                        // came true, and no input could dismiss the dialog either.
                        .waitUntil("the stranded notification closes", ctx -> ctx.count("dialog") == 0)
                        .screenshot("03_after_reopen"))

                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }
}
