package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.client.window.OsWindowManager;
import com.lowdragmc.lowdraglib2.editor.settings.AppearanceSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingLayout;
import com.lowdragmc.lowdraglib2.editor.ui.floating.FloatingViewWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Tab;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.OffscreenSurface;
import com.lowdragmc.lowdraglib2.gui.ui.window.ModularUIWindow;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * An OS window drawing at a gui scale of its own, without moving the game's.
 *
 * <p>A floated tab picks its scale from its title bar's or its tab's right-click menu; an editor in a
 * window of its own takes it from View > Window Size. Asserted on what actually changes: the scale the
 * window draws at, the size its UI is laid out against, that clicks still land on what is under the
 * pointer, and that the game window stays exactly as it was.
 */
@LDLRegisterClient(name = "os_window_gui_scale", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class OsWindowGuiScaleScenario implements UIScenario {

    private static final String GAME_SCALE = "game_scale";
    private static final String GAME_UI_WIDTH = "game_ui_width";
    private static final String EDITOR_WINDOW = "editor_window";
    private static final String ORIGINAL_SETTING = "original_screen_scale";
    private static final String ORIGINAL_PREFERENCE = "original_prefer_os_window";

    private static final String BRANCH = "__menu_branch-node__";
    private static final String LEAF = "__menu_leaf-node__";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "window", "scale").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("remember the game window's scale and layout", ctx -> {
                    ctx.put(GAME_SCALE, (double) ctx.mc().getWindow().getGuiScale()); // an int on 26.1
                    ctx.put(GAME_UI_WIDTH, ctx.requireUI().getScreenWidth());
                })

                .group("a floated tab follows the game by default", g -> g
                        .step("float the inspector", ctx -> ctx.check("floated",
                                editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView)))
                        .frames(20)
                        .step("size its window", ctx -> floating(ctx).window().setSize(1300, 1000))
                        .frames(15)
                        .check("it follows the game", ctx -> floating(ctx).getGuiScaleSetting() == OffscreenSurface.FOLLOW_GAME
                                && floating(ctx).getGuiScale() == gameScale(ctx))
                        .check("its UI is laid out at that scale", ctx -> laidOutAt(floating(ctx), gameScale(ctx)))
                        // a strip built empty and filled after the window's first layout used to stay 0 high
                        .check("its tab strip has height", ctx -> {
                            var container = editor(ctx).inspectorView.getViewContainer();
                            return container != null && container.tabView.tabHeaderContainer.getSizeHeight() > 0;
                        }))

                .group("its title bar's right-click menu gives it a scale of its own", g -> g
                        .step("right-click the title bar", ctx -> ctx.input(floating(ctx))
                                .moveTo(floating(ctx).getRoot().titleLabel)
                                .mouseDown(Keys.MOUSE_RIGHT).mouseUp(Keys.MOUSE_RIGHT))
                        .frames(5)
                        .step("open Window Scale", ctx -> ctx.input(floating(ctx))
                                .moveTo(menuEntry(floating(ctx), BRANCH, "editor.window.scale")))
                        .frames(5)
                        .check("it offers following the game", ctx -> menuEntries(floating(ctx), LEAF, "editor.window.scale.follow_game").size() == 1)
                        .step("pick 1", ctx -> clickMenuEntry(ctx, floating(ctx), LEAF, "1"))
                        .frames(10)
                        .check("the window draws at 1", ctx -> floating(ctx).getGuiScaleSetting() == 1 && floating(ctx).getGuiScale() == 1)
                        .check("its UI is laid out at 1", ctx -> laidOutAt(floating(ctx), 1))
                        .check("the game's scale did not move", ctx -> ctx.mc().getWindow().getGuiScale() == gameScale(ctx))
                        .check("nor did the editor in the game window", ctx -> ctx.requireUI().getScreenWidth() == ctx.<Integer>get(GAME_UI_WIDTH))
                        .screenshotSurface("01_floated_tab_at_scale_1", ctx -> floating(ctx).surface()))

                // The entries are aimed at where they are drawn at the new scale; mapped through the old one
                // the click would land on a different entry, and the check below would see another setting.
                .group("at the new scale clicks still land on what is under them", g -> g
                        .step("right-click the title bar again", ctx -> ctx.input(floating(ctx))
                                .moveTo(floating(ctx).getRoot().titleLabel)
                                .mouseDown(Keys.MOUSE_RIGHT).mouseUp(Keys.MOUSE_RIGHT))
                        .frames(5)
                        .step("open Window Scale", ctx -> ctx.input(floating(ctx))
                                .moveTo(menuEntry(floating(ctx), BRANCH, "editor.window.scale")))
                        .frames(5)
                        .step("pick auto", ctx -> clickMenuEntry(ctx, floating(ctx), LEAF, "options.guiScale.auto"))
                        .frames(10)
                        .check("auto is the largest scale that keeps the UI at 320x240", ctx -> floating(ctx).getGuiScaleSetting() == 0
                                && floating(ctx).getGuiScale() == largestFitting(floating(ctx))))

                .group("the tab's own right-click menu has the scale too", g -> g
                        .step("right-click the inspector's tab", ctx -> ctx.input(floating(ctx))
                                .moveTo(tabOf(ctx))
                                .mouseDown(Keys.MOUSE_RIGHT).mouseUp(Keys.MOUSE_RIGHT))
                        .frames(5)
                        .check("the tab's menu opened", ctx -> menuEntries(floating(ctx), LEAF, "editor.tab.dock_back").size() == 1)
                        .step("open Window Scale", ctx -> ctx.input(floating(ctx))
                                .moveTo(menuEntry(floating(ctx), BRANCH, "editor.window.scale")))
                        .frames(5)
                        .step("pick following the game", ctx -> clickMenuEntry(ctx, floating(ctx), LEAF, "editor.window.scale.follow_game"))
                        .frames(10)
                        .check("it follows the game again", ctx -> floating(ctx).getGuiScaleSetting() == OffscreenSurface.FOLLOW_GAME
                                && floating(ctx).getGuiScale() == gameScale(ctx)))

                .group("a fixed scale is lowered while the window is too small for it", g -> g
                        .step("set 4", ctx -> editor(ctx).getFloatingViews().setWindowScale(floating(ctx), 4))
                        .step("shrink the window", ctx -> floating(ctx).window().setSize(700, 500))
                        .frames(15)
                        .check("it draws at what fits", ctx -> floating(ctx).getGuiScale() == Math.min(4, largestFitting(floating(ctx))))
                        .check("and it was lowered", ctx -> floating(ctx).getGuiScale() < 4)
                        .check("but still asks for 4", ctx -> floating(ctx).getGuiScaleSetting() == 4)
                        .step("grow it back", ctx -> floating(ctx).window().setSize(1300, 1000))
                        .frames(15)
                        .check("back at 4", ctx -> floating(ctx).getGuiScale() == 4)
                        .check("laid out at 4", ctx -> laidOutAt(floating(ctx), 4)))

                .group("the scale goes with the tab", g -> g
                        .step("dock it back", ctx -> editor(ctx).getFloatingViews().dockBack(editor(ctx).inspectorView))
                        .frames(10)
                        .step("float it again", ctx -> ctx.check("floated",
                                editor(ctx).getFloatingViews().floatView(editor(ctx).inspectorView)))
                        .frames(20)
                        .check("it comes back at 4", ctx -> floating(ctx).getGuiScaleSetting() == 4)
                        .check("the saved layout carries it", ctx -> {
                            var layouts = editor(ctx).getFloatingViews().capture();
                            var scales = editor(ctx).getFloatingViews().captureScales();
                            return layouts.size() == 1 && layouts.getFirst().guiScale() == 4
                                    && Integer.valueOf(4).equals(scales.get(editor(ctx).inspectorView.getName()));
                        })
                        .check("and reads it back", ctx -> {
                            var layout = editor(ctx).getFloatingViews().capture().getFirst();
                            return FloatingLayout.deserialize(layout.serialize()).guiScale() == 4;
                        })
                        .check("a layout saved before scales existed follows the game", ctx -> {
                            var tag = editor(ctx).getFloatingViews().capture().getFirst().serialize();
                            tag.remove("guiScale");
                            return FloatingLayout.deserialize(tag).guiScale() == OffscreenSurface.FOLLOW_GAME;
                        })
                        .step("follow the game again", ctx -> editor(ctx).getFloatingViews()
                                .setWindowScale(floating(ctx), OffscreenSurface.FOLLOW_GAME))
                        .frames(10)
                        .check("it follows the game", ctx -> floating(ctx).getGuiScale() == gameScale(ctx))
                        .check("laid out at the game's scale", ctx -> laidOutAt(floating(ctx), gameScale(ctx))))

                .group("an editor in its own window takes View > Window Size for itself", g -> g
                        .step("open an editor window", ctx -> {
                            var window = new EditorWindow(TestEditor::new);
                            ctx.put(EDITOR_WINDOW, window);
                            window.hostAsScreen();
                        })
                        .awaitScreen(ModularUIScreen.class)
                        .awaitModularUI()
                        .frames(5)
                        .step("start it without a scale or a host of its own", ctx -> {
                            var appearance = appearance(ctx);
                            ctx.put(ORIGINAL_SETTING, appearance.getScreenScale());
                            ctx.put(ORIGINAL_PREFERENCE, appearance.isPreferOsWindow());
                            appearance.setScreenScale(OffscreenSurface.FOLLOW_GAME);
                            appearance.setPreferOsWindow(false);
                        })
                        .step("pop it out", ctx -> ctx.check("popped out", editorWindow(ctx).popOutToOsWindow()))
                        .frames(20)
                        // or the settings applied below, which move it to the host it prefers, would pull it back
                        .check("popping out recorded that it prefers its own window", ctx -> editorWindow(ctx).prefersOsWindow())
                        .step("size its window", ctx -> editorOsWindow(ctx).window().setSize(1300, 1000))
                        .frames(15)
                        .check("it follows the game", ctx -> editorOsWindow(ctx).getGuiScale() == gameScale(ctx))
                        .step("open the View menu", ctx -> ctx.input(editorOsWindow(ctx))
                                .moveTo(textElement(editorOsWindow(ctx).getModularUI().ui.rootElement,
                                        Component.translatable("editor.view").getString()))
                                .mouseDown(Keys.MOUSE_LEFT).mouseUp(Keys.MOUSE_LEFT))
                        .frames(5)
                        .step("open Window Size", ctx -> ctx.input(editorOsWindow(ctx))
                                .moveTo(menuEntry(editorOsWindow(ctx), BRANCH, "ldlib.gui.editor.menu.view.window_size")))
                        .frames(5)
                        .check("it offers following the game", ctx -> menuEntries(editorOsWindow(ctx), LEAF, "editor.window.scale.follow_game").size() == 1)
                        .step("pick 1", ctx -> clickMenuEntry(ctx, editorOsWindow(ctx), LEAF, "1"))
                        .frames(10)
                        .check("its window draws at 1", ctx -> editorOsWindow(ctx).getGuiScale() == 1)
                        .check("laid out at 1", ctx -> laidOutAt(editorOsWindow(ctx), 1))
                        .check("the game's scale did not move", ctx -> ctx.mc().getWindow().getGuiScale() == gameScale(ctx))
                        .check("it is the editor's setting", ctx -> appearance(ctx).getScreenScale() == 1)
                        .screenshotSurface("02_editor_window_at_scale_1", ctx -> editorOsWindow(ctx).surface())
                        .step("unset it", ctx -> {
                            appearance(ctx).setScreenScale(OffscreenSurface.FOLLOW_GAME);
                            editor(editorWindow(ctx)).getEditorSettings().applyCurrentSettings();
                        })
                        .frames(10)
                        .check("applying the settings left it in its own window", ctx -> editorWindow(ctx).isInOsWindow())
                        .check("it follows the game again", ctx -> editorOsWindow(ctx).getGuiScale() == gameScale(ctx)))

                .check("no window host ever threw while being driven", ctx -> OsWindowManager.totalFailures() == 0)

                // the setting is saved to the test editor's settings file, which every later scenario reads
                .teardown("put the editor's settings back", ctx -> {
                    EditorWindow window = ctx.get(EDITOR_WINDOW);
                    Integer scale = ctx.get(ORIGINAL_SETTING);
                    Boolean preference = ctx.get(ORIGINAL_PREFERENCE);
                    if (window == null || scale == null || preference == null || window.getCurrentEditor() == null) return;
                    AppearanceSettings.of(window.getCurrentEditor()).ifPresent(appearance -> {
                        appearance.setScreenScale(scale);
                        appearance.setPreferOsWindow(preference);
                    });
                    window.getCurrentEditor().getEditorSettings().saveAllSettingsToFile();
                })
                .teardown("close any window the run left behind", ctx -> {
                    for (var host : OsWindowManager.hosts()) {
                        OsWindowManager.close(host);
                    }
                })
                .teardown("close the screen", ctx -> ctx.mc().gui.setScreen(null));
    }

    private static Editor editor(TestContext ctx) {
        return EditorPaneMaximizeScenario.editor(ctx);
    }

    private static Editor editor(EditorWindow window) {
        var editor = window.getCurrentEditor();
        if (editor == null) throw new IllegalStateException("The editor window has no editor");
        return editor;
    }

    private static FloatingViewWindow floating(TestContext ctx) {
        var window = editor(ctx).getFloatingViews().windowOf(editor(ctx).inspectorView);
        if (window == null || !window.isOpen()) {
            throw new IllegalStateException("The inspector is not in a native window");
        }
        return window;
    }

    private static EditorWindow editorWindow(TestContext ctx) {
        return ctx.get(EDITOR_WINDOW);
    }

    private static ModularUIWindow editorOsWindow(TestContext ctx) {
        var window = editorWindow(ctx).getOsWindow();
        if (window == null || !window.isOpen()) throw new IllegalStateException("The editor is not in its own window");
        return window;
    }

    private static AppearanceSettings appearance(TestContext ctx) {
        return AppearanceSettings.of(editor(editorWindow(ctx)))
                .orElseThrow(() -> new IllegalStateException("The test editor has no appearance settings"));
    }

    private static double gameScale(TestContext ctx) {
        return ctx.get(GAME_SCALE);
    }

    /** The UI is laid out against the window's framebuffer at {@code scale}, not the game's. */
    private static boolean laidOutAt(ModularUIWindow window, double scale) {
        var surface = window.surface();
        if (surface == null) return false;
        return window.getModularUI().getScreenWidth() == Mth.ceil(surface.framebufferWidth() / scale)
                && window.getModularUI().getScreenHeight() == Mth.ceil(surface.framebufferHeight() / scale);
    }

    /** The game's rule for auto, worked out independently of the loop that implements it. */
    private static int largestFitting(ModularUIWindow window) {
        var surface = window.surface();
        if (surface == null) return 1;
        return Math.max(1, Math.min(surface.framebufferWidth() / 320, surface.framebufferHeight() / 240));
    }


    private static UIElement tabOf(TestContext ctx) {
        var view = editor(ctx).inspectorView;
        var container = view.getViewContainer();
        ctx.require("the inspector is docked somewhere", container != null);
        var tabs = new ArrayList<Tab>();
        collect(container, Tab.class, tabs);
        var name = view.getViewNameComponent().getString();
        for (var tab : tabs) {
            if (name.equals(labelOf(tab))) return tab;
        }
        throw new IllegalStateException("No tab labelled " + name);
    }

    private static UIElement textElement(UIElement root, String text) {
        var texts = new ArrayList<TextElement>();
        collect(root, TextElement.class, texts);
        for (var element : texts) {
            if (text.equals(element.getText().getString())) return element;
        }
        throw new IllegalStateException("No text '" + text + "'");
    }

    /** Clicks a menu entry, pressing and releasing together: the click closes the menu it is in. */
    private static void clickMenuEntry(TestContext ctx, ModularUIWindow window, String kind, String key) {
        ctx.input(window).moveTo(menuEntry(window, kind, key))
                .mouseDown(Keys.MOUSE_LEFT).mouseUp(Keys.MOUSE_LEFT);
    }

    private static UIElement menuEntry(ModularUIWindow window, String kind, String key) {
        var entries = menuEntries(window, kind, key);
        if (entries.size() != 1) {
            throw new IllegalStateException("Expected one menu entry '" + key + "' but found " + entries.size());
        }
        return entries.getFirst();
    }

    /** Entries of open menus in {@code window}'s UI labelled {@code key}; a submenu is a child of its branch. */
    private static List<UIElement> menuEntries(ModularUIWindow window, String kind, String key) {
        var label = Component.translatable(key).getString();
        var menus = new ArrayList<Menu>();
        collect(window.getModularUI().ui.rootElement, Menu.class, menus);
        var entries = new ArrayList<UIElement>();
        for (var menu : menus) {
            collectEntries(menu, kind, label, entries);
        }
        return entries.stream().distinct().toList();
    }

    private static void collectEntries(UIElement element, String kind, String label, List<UIElement> out) {
        if (element.hasClass(kind) && label.equals(labelOf(element))) out.add(element);
        for (var child : element.getChildren()) {
            collectEntries(child, kind, label, out);
        }
    }

    private static <T> void collect(UIElement element, Class<T> type, List<T> out) {
        if (type.isInstance(element)) out.add(type.cast(element));
        for (var child : element.getChildren()) {
            collect(child, type, out);
        }
    }

    private static String labelOf(UIElement element) {
        if (element instanceof TextElement text) return text.getText().getString();
        for (var child : element.getChildren()) {
            var text = labelOf(child);
            if (!text.isEmpty()) return text;
        }
        return "";
    }
}
