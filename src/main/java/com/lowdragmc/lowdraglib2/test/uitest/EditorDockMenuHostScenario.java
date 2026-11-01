package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorOsWindow;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.editor.UIEditor;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Moving the UI editor out into its own window and back.
 *
 * <p>The editor is opened through a menu, so it lives in a container screen, and recipe viewers only
 * attach to those. Docking it back used to show it in a plain screen, where JEI, REI and EMI all
 * stayed hidden; it now goes back through the menu, carrying the same editor across.
 */
@LDLRegisterClient(name = "editor_dock_menu_host", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorDockMenuHostScenario implements UIScenario {

    private static final String WINDOW = "editor_window";
    private static final String EDITOR = "editor";
    private static final String REMOVALS = "editor_removals";
    private static final String OS_WINDOW = "os_window";
    private static final String MINIMIZED_TOAST = "editor.minimized.title";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "window", "xei").requiresWorld(true).guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.group("a restored editor in its menu has JEI beside it", g -> {
                    openThroughMenu(g);
                    g.step("remember the editor", ctx -> rememberEditor(ctx))
                            .step("restore it", ctx -> {
                                if (window(ctx).isMaximized()) window(ctx).toggleMaximized();
                            })
                            .frames(5)
                            .check("it is restored", ctx -> !window(ctx).isMaximized())
                            .waitUntil("JEI attaches", ctx -> Jei.attachedTo(ctx.mc().screen));
                })

                .group("the title bar toggle moves it into its own window", g -> g
                        // press and release together: the press closes the screen the button is on
                        .step("click the toggle", ctx -> {
                            var bounds = ElementBounds.of(hostToggle(ctx));
                            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                        })
                        .frames(25)
                        .check("it is in its own window", ctx -> window(ctx).isInOsWindow())
                        .check("it fills that window", ctx -> window(ctx).isMaximized())
                        .check("the game window has no screen", ctx -> ctx.mc().screen == null)
                        .check("it prefers its own window now", ctx -> window(ctx).prefersOsWindow()))

                .group("the toggle in its window docks it back through the menu", g -> g
                        .step("click the toggle in its window", ctx -> ctx.input(window(ctx).getOsWindow())
                                .moveTo(hostToggle(ctx))
                                .mouseDown(Keys.MOUSE_LEFT)
                                .mouseUp(Keys.MOUSE_LEFT))
                        .awaitScreen(ModularUIContainerScreen.class)
                        .awaitModularUI()
                        .frames(5)
                        .check("its window closed", ctx -> !window(ctx).isInOsWindow() && !OsWindowManager.hasWindows())
                        .check("the same editor came back", ctx -> sameEditor(ctx))
                        .check("it is restored as it was before popping out", ctx -> !window(ctx).isMaximized())
                        .check("it no longer prefers its own window", ctx -> !window(ctx).prefersOsWindow())
                        .check("no minimized toast for a dock", ctx -> !systemToastKeys(ctx).contains(MINIMIZED_TOAST))
                        .check("the editor was never torn down on the way", ctx -> removals(ctx) == 0)
                        .waitUntil("JEI attaches again", ctx -> Jei.attachedTo(ctx.mc().screen))
                        .screenshot("01_docked_back_with_jei"))

                .group("minimized from its own window, it comes back restored", g -> {
                    g.step("pop it out", ctx -> ctx.check("popped out", window(ctx).popOutToOsWindow()))
                            .frames(20)
                            .check("it is in its own window", ctx -> window(ctx).isInOsWindow())
                            .step("minimize it", ctx -> window(ctx).minimizeWindow())
                            .frames(5)
                            .check("its window closed", ctx -> !OsWindowManager.hasWindows());
                    openThroughMenu(g);
                    g.check("the same editor came back", ctx -> sameEditor(ctx))
                            .check("it is restored as it was before popping out", ctx -> !window(ctx).isMaximized())
                            .check("a minimize the player asked for still says so",
                                    ctx -> systemToastKeys(ctx).contains(MINIMIZED_TOAST))
                            .check("the editor was never torn down on the way", ctx -> removals(ctx) == 0)
                            .waitUntil("JEI attaches", ctx -> Jei.attachedTo(ctx.mc().screen));
                })

                .group("a reopener that cannot reopen docks into a plain screen", g -> g
                        .step("make the reopener refuse", ctx -> window(ctx).setGameWindowReopener(() -> false))
                        .step("pop it out", ctx -> ctx.check("popped out", window(ctx).popOutToOsWindow()))
                        .frames(20)
                        .step("dock it", ctx -> ctx.check("docked", window(ctx).dockIntoGameWindow()))
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .frames(5)
                        .check("its window closed", ctx -> !OsWindowManager.hasWindows())
                        .check("it is in a plain screen", ctx -> ctx.mc().screen != null
                                && ctx.mc().screen.getClass() == ModularUIScreen.class)
                        .check("the same editor came back", ctx -> sameEditor(ctx))
                        .check("it is restored as it was before popping out", ctx -> !window(ctx).isMaximized())
                        .check("the editor was never torn down on the way", ctx -> removals(ctx) == 0))

                // Left parked by the refused reopen, it would survive the close and come back here.
                .group("closing it still tears it down", g -> {
                    g.closeScreen().frames(5);
                    openThroughMenu(g);
                    g.check("the old editor was torn down", ctx -> removals(ctx) > 0)
                            .check("a new editor opened", ctx -> editorWindow(ctx) != window(ctx));
                })

                .group("exiting it from its own window tears it down", g -> {
                    g.step("remember the new editor", ctx -> rememberEditor(ctx))
                            .step("pop it out", ctx -> {
                                ctx.check("popped out", window(ctx).popOutToOsWindow());
                                ctx.put(OS_WINDOW, window(ctx).getOsWindow());
                            })
                            .frames(20)
                            .step("exit it", ctx -> window(ctx).closeWindow())
                            .frames(10)
                            .check("its window closed", ctx -> !OsWindowManager.hasWindows())
                            .check("the window's UI was torn down",
                                    ctx -> ctx.<EditorOsWindow>get(OS_WINDOW).getModularUI().isRemoved())
                            .check("the editor was torn down", ctx -> removals(ctx) > 0);
                    openThroughMenu(g);
                    g.check("it was not left parked: a new editor opened", ctx -> editorWindow(ctx) != window(ctx));
                })

                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                .teardown("close the screen", ctx -> {
                    if (ctx.player() != null) ctx.player().closeContainer();
                    ctx.mc().setScreen(null);
                });
    }

    private static void openThroughMenu(ScenarioBuilder g) {
        g.server("open the UI editor's menu", sc -> PlayerUIMenuType.openUI(sc.player(), UIEditor.WINDOW_ID))
                .awaitScreen(ModularUIContainerScreen.class)
                .awaitModularUI()
                .frames(5);
    }

    private static void rememberEditor(TestContext ctx) {
        var window = editorWindow(ctx);
        var editor = window.getCurrentEditor();
        ctx.require("the editor is open", editor != null);
        var removals = new AtomicInteger();
        editor.addEventListener(UIEvents.REMOVED, e -> removals.incrementAndGet());
        ctx.put(WINDOW, window);
        ctx.put(EDITOR, editor);
        ctx.put(REMOVALS, removals);
    }

    private static int removals(TestContext ctx) {
        return ctx.<AtomicInteger>get(REMOVALS).get();
    }

    private static EditorWindow editorWindow(TestContext ctx) {
        if (ctx.requireUI().ui.rootElement instanceof EditorWindow window) return window;
        throw new IllegalStateException("The open UI is not an editor window");
    }

    private static EditorWindow window(TestContext ctx) {
        return ctx.get(WINDOW);
    }

    private static boolean sameEditor(TestContext ctx) {
        Editor editor = ctx.get(EDITOR);
        return editorWindow(ctx) == window(ctx) && window(ctx).getCurrentEditor() == editor;
    }

    private static UIElement hostToggle(TestContext ctx) {
        var editor = window(ctx).getCurrentEditor();
        ctx.require("the editor is open", editor != null);
        return editor.buttonContainer.selectId(EditorWindow.HOST_TOGGLE_ID).findFirst()
                .orElseThrow(() -> new IllegalStateException("No host toggle in the title bar"));
    }

    /** Translation keys of the system toasts showing or waiting to show. */
    private static List<String> systemToastKeys(TestContext ctx) {
        try {
            var toasts = ctx.mc().getToastManager();
            var all = new ArrayList<Toast>();
            var queued = ToastManager.class.getDeclaredField("queued");
            queued.setAccessible(true);
            for (var toast : (Collection<?>) queued.get(toasts)) {
                all.add((Toast) toast);
            }
            var visible = ToastManager.class.getDeclaredField("visibleToasts");
            visible.setAccessible(true);
            for (var instance : (Collection<?>) visible.get(toasts)) {
                var getToast = instance.getClass().getDeclaredMethod("getToast");
                getToast.setAccessible(true);
                all.add((Toast) getToast.invoke(instance));
            }
            var title = SystemToast.class.getDeclaredField("title");
            title.setAccessible(true);
            var keys = new ArrayList<String>();
            for (var toast : all) {
                if (toast instanceof SystemToast
                        && ((Component) title.get(toast)).getContents() instanceof TranslatableContents contents) {
                    keys.add(contents.getKey());
                }
            }
            return keys;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not read the toast queue", e);
        }
    }

    /**
     * Kept apart so nothing loads JEI's classes unless the check actually runs.
     *
     * <p>Whether JEI takes the screen on at all, which it does for container screens only: that is what
     * the menu host is for. Not whether its ingredient list is showing: the editor's root fills the
     * screen and is reported as an exclusion area, and JEI on 26.1 hides the whole list when no room is
     * left beside the GUI, where 1.21's only hid the covered slots.
     */
    private static final class Jei {
        static boolean attachedTo(@Nullable Screen screen) {
            var runtime = LDLibJEIPlugin.jeiRuntime;
            return runtime != null && screen != null && runtime.getScreenHelper().getGuiProperties(screen).isPresent();
        }
    }
}
