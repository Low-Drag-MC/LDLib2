package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorAction;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorActions;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.keymap.Keymap;
import com.lowdragmc.lowdraglib2.editor.keymap.ui.KeyChordField;
import com.lowdragmc.lowdraglib2.editor.keymap.ui.KeymapConfigurator;
import com.lowdragmc.lowdraglib2.editor.settings.KeymapSettings;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The editor keymap end to end: a chord reaches its action, a focused text field keeps the keys it
 * types, a rebinding takes effect, and the field that assigns a shortcut swallows the chord it is given.
 *
 * <p>Those are the parts a unit test cannot reach, because they are about how a key event travels
 * through the element tree rather than about what the keymap resolves.
 */
@LDLRegisterClient(name = "editor_keymap", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class EditorKeymapScenario implements UIScenario {

    private static final ResourceLocation CHORD_ACTION = LDLib2.id("uitest.keymap_chord");
    private static final ResourceLocation BARE_ACTION = LDLib2.id("uitest.keymap_bare");

    private static final KeyChord PROBE_CHORD = KeyChord.ctrlShift(GLFW.GLFW_KEY_K);
    private static final KeyChord REBOUND_CHORD = KeyChord.ctrlShift(GLFW.GLFW_KEY_J);

    private static final String CHORD_RUNS = "chord_runs";
    private static final String BARE_RUNS = "bare_runs";
    private static final String FIELD = "text_field";
    private static final String SAVE_COMMANDS = "save_commands";
    private static final String DIALOG_SIZE = "dialog_size";
    private static final String SELECTED_VIEW = "selected_view";

    /** One texture-based theme, one flat dark, one light, and the light-button one the icons fight. */
    private static final List<String> THEMES = List.of("mc", "carbon", "latte", "ore");

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "keymap").guiScale(2);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", EditorKeymapScenario::buildEditorUI)
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)

                // A dev's own config lives in the run directory and would otherwise decide what these
                // chords are. Overrides are dropped from the live keymap only - nothing is written back,
                // because nothing here presses Apply.
                .step("start from the shipped defaults", ctx -> {
                    var editor = editor(ctx);
                    var settings = KeymapSettings.of(editor);
                    settings.getActions().forEach(settings::reset);
                    editor.getEditorSettings().applyCurrentSettings();
                })
                .step("register two probe actions", ctx -> {
                    var chordRuns = new AtomicInteger();
                    var bareRuns = new AtomicInteger();
                    ctx.put(CHORD_RUNS, chordRuns);
                    ctx.put(BARE_RUNS, bareRuns);
                    editor(ctx).getKeymap().registerAll(
                            EditorAction.builder(CHORD_ACTION)
                                    .defaultChord(PROBE_CHORD)
                                    .onAction(chordRuns::incrementAndGet)
                                    .build(),
                            // a bare key: the kind that has to stand aside while the user is typing
                            EditorAction.builder(BARE_ACTION)
                                    .defaultChord(KeyChord.key(GLFW.GLFW_KEY_SPACE))
                                    .onAction(bareRuns::incrementAndGet)
                                    .build());
                })
                .step("put a text field in the editor", ctx -> {
                    var field = new TextField();
                    field.setAnyString();
                    field.setText("");
                    field.layout(layout -> layout.width(120).height(12));
                    ctx.put(FIELD, field);
                    editor(ctx).topPlaceholder.addChild(field);
                })
                .settleMs(120)

                .group("a chord reaches its action", g -> g
                        .step("focus the editor itself", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_K, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("the action ran once", ctx -> runs(ctx, CHORD_RUNS) == 1)
                        .check("the bare-key action did not", ctx -> runs(ctx, BARE_RUNS) == 0)
                        .key(GLFW.GLFW_KEY_SPACE)
                        .check("a bare space runs its action when nothing is being typed into",
                                ctx -> runs(ctx, BARE_RUNS) == 1))

                .group("a focused text field keeps the keys it types", g -> g
                        .step("focus the text field", ctx -> field(ctx).focus())
                        .key(GLFW.GLFW_KEY_SPACE)
                        .check("the space did not run the shortcut", ctx -> runs(ctx, BARE_RUNS) == 1)
                        .type("ab")
                        .check("and typing still reaches the field", ctx -> fieldText(ctx).contains("ab")))

                .group("a modified chord still reaches the editor while typing", g -> g
                        .check("the field still has the focus", ctx -> field(ctx).isFocused())
                        .key(GLFW.GLFW_KEY_K, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("the action ran again", ctx -> runs(ctx, CHORD_RUNS) == 2)
                        .check("and nothing was typed into the field", ctx -> fieldText(ctx).equals("ab")))

                .group("rebinding through the settings takes effect", g -> g
                        .step("bind the probe to ctrl+shift+j instead", ctx -> {
                            var editor = editor(ctx);
                            var settings = KeymapSettings.of(editor);
                            settings.setBindings(action(ctx, CHORD_ACTION),
                                    Keymap.Bindings.of(REBOUND_CHORD, KeyChord.UNBOUND));
                            editor.getEditorSettings().applyCurrentSettings();
                        })
                        .step("focus the editor itself", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_K, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("the old chord does nothing", ctx -> runs(ctx, CHORD_RUNS) == 2)
                        .key(GLFW.GLFW_KEY_J, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("the new one runs it", ctx -> runs(ctx, CHORD_RUNS) == 3))

                // The chords the keymap's Edit and File actions use are also hardcoded in ModularUI's
                // built-in command table, which is the fallback for UIs that have no keymap. Rebinding
                // has to take the old key away, or the user ends up with two keys for one action and no
                // way to get rid of the first.
                .group("rebinding an action frees its old chord", g -> g
                        .step("count the save commands the editor receives", ctx -> {
                            var saves = new AtomicInteger();
                            ctx.put(SAVE_COMMANDS, saves);
                            editor(ctx).addEventListener(UIEvents.EXECUTE_COMMAND, event -> {
                                if (CommandEvents.SAVE.equals(event.command)) saves.incrementAndGet();
                            });
                        })
                        .step("focus the editor itself", ctx -> editor(ctx).focus())
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .check("ctrl+s saves while that is what save is bound to",
                                ctx -> runs(ctx, SAVE_COMMANDS) == 1)
                        .step("move save onto ctrl+shift+b", ctx -> {
                            var editor = editor(ctx);
                            KeymapSettings.of(editor).setBindings(action(ctx, EditorActions.SAVE),
                                    Keymap.Bindings.of(KeyChord.ctrlShift(GLFW.GLFW_KEY_B), KeyChord.UNBOUND));
                            editor.getEditorSettings().applyCurrentSettings();
                        })
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .check("the old chord no longer saves", ctx -> runs(ctx, SAVE_COMMANDS) == 1)
                        .key(GLFW.GLFW_KEY_B, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("and the new one does", ctx -> runs(ctx, SAVE_COMMANDS) == 2)
                        .step("put save back", ctx -> {
                            var editor = editor(ctx);
                            KeymapSettings.of(editor).reset(action(ctx, EditorActions.SAVE));
                            editor.getEditorSettings().applyCurrentSettings();
                        })
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .check("resetting brings ctrl+s back", ctx -> runs(ctx, SAVE_COMMANDS) == 3))

                .group("the keymap page lists every action", g -> g
                        // Built the way the settings dialog builds it - buildConfigurator on the same
                        // settings instance - but shown in a dialog of its own, because selecting a page
                        // in the settings tree is that tree's behaviour rather than the keymap's.
                        .step("open the keymap page", ctx -> {
                            var editor = editor(ctx);
                            // in a box the width the settings dialog gives its pages, so the capture
                            // shows what the user would see rather than an unconstrained page
                            var page = new UIElement();
                            page.layout(layout -> layout.width(235));
                            page.addChild(new KeymapConfigurator(KeymapSettings.of(editor)));
                            var dialog = new Dialog();
                            dialog.setTitle("keymap");
                            dialog.addContent(page);
                            dialog.show(editor.getModularUI());
                        })
                        .settleMs(200)
                        .check("two chord fields per action",
                                ctx -> ctx.count("." + KeyChordField.FIELD_CLASS)
                                        == editor(ctx).getKeymap().getActions().size() * 2)
                        // counting elements is not the same as showing them: a page that laid out to
                        // nothing would pass the count and be invisible
                        .check("and they are actually laid out", ctx -> {
                            var bounds = ctx.query().select("." + KeyChordField.FIELD_CLASS).nth(0).one().bounds();
                            return bounds.width() > 20 && bounds.height() > 5;
                        })
                        .screenshot("01_keymap_page"))

                .group("assigning a chord swallows it", g -> g
                        .step("start listening on the probe's row", ctx -> captureField(ctx, REBOUND_CHORD)
                                .startCapture())
                        // the very chord the probe answers to: it must be recorded, not run
                        .key(GLFW.GLFW_KEY_J, Keys.MOD_CONTROL | Keys.MOD_SHIFT)
                        .check("the action did not run", ctx -> runs(ctx, CHORD_RUNS) == 3)
                        .check("and the chord is still what it was",
                                ctx -> REBOUND_CHORD.equals(binding(ctx, CHORD_ACTION))))

                .group("a clash with a built-in action is marked, not refused", g -> g
                        .step("assign ctrl+s, which save already uses",
                                ctx -> captureField(ctx, REBOUND_CHORD).startCapture())
                        .key(GLFW.GLFW_KEY_S, Keys.MOD_CONTROL)
                        .check("the chord was taken",
                                ctx -> KeyChord.ctrl(GLFW.GLFW_KEY_S).equals(binding(ctx, CHORD_ACTION)))
                        .check("assigning it did not save anything",
                                ctx -> ctx.count(".__dialog_progress-bar__") == 0)
                        .check("the page reports the clash with save", ctx -> KeymapSettings.of(editor(ctx))
                                .conflictsWith(action(ctx, CHORD_ACTION), KeyChord.ctrl(GLFW.GLFW_KEY_S))
                                .stream().anyMatch(other -> other.id().equals(EditorActions.SAVE)))
                        .screenshot("02_conflict"))

                // The page is built out of configurator groups and a field the stylesheets style like a
                // text field, so "does it follow the theme" is a question only a picture answers.
                .group("the page follows the stylesheet", g -> {
                    // left listening for the shots, so each theme shows both states of the field
                    g.step("put one field into its listening state",
                            ctx -> captureField(ctx, KeyChord.ctrl(GLFW.GLFW_KEY_O)).startCapture());
                    for (var theme : THEMES) {
                        g.step("switch to " + theme, ctx -> applyTheme(ctx, theme))
                                .frames(2)
                                .check(theme + " is loaded", ctx -> StylesheetManager.INSTANCE
                                        .getStylesheetSafe(themeLocation(theme)).rules.size() > 10)
                                .screenshotElement("03_theme_" + theme, "." + KeymapConfigurator.PAGE_CLASS);
                    }
                })

                // The settings dialog is a window now: draggable by its title bar and resizable by its
                // borders, because the keymap page is a long list nobody wants to read through a slot.
                .group("the settings dialog resizes by its border", g -> {
                    // held across the steps below: a drag is one gesture spread over frames, and each
                    // move has to be its own step for the UI to see it
                    var corner = new float[2];
                    g.step("open the editor settings", ctx -> editor(ctx).openSettingsPanel())
                            .settleMs(200)
                            .step("aim at its bottom-right corner", ctx -> {
                                var bounds = dialogBounds(ctx);
                                ctx.put(DIALOG_SIZE, new float[]{bounds.width(), bounds.height()});
                                // inside the 2px border the resize handle lives in
                                corner[0] = bounds.x() + bounds.width() - 1;
                                corner[1] = bounds.y() + bounds.height() - 1;
                                ctx.input().moveTo(corner[0], corner[1]);
                            })
                            .step("press", ctx -> ctx.input().mouseDown(corner[0], corner[1], Keys.MOUSE_LEFT))
                            .step("nudge", ctx -> ctx.input().dragTo(corner[0] + 1, corner[1] + 1, Keys.MOUSE_LEFT))
                            .step("pull out", ctx -> ctx.input().dragTo(corner[0] + 40, corner[1] + 25, Keys.MOUSE_LEFT))
                            .step("keep pulling", ctx -> ctx.input().dragTo(corner[0] + 80, corner[1] + 50, Keys.MOUSE_LEFT))
                            .step("drop", ctx -> ctx.input().mouseUp(corner[0] + 80, corner[1] + 50, Keys.MOUSE_LEFT))
                            .settleMs(150)
                            .check("it got bigger", ctx -> {
                                var before = ctx.<float[]>get(DIALOG_SIZE);
                                var now = dialogBounds(ctx);
                                ctx.log("dialog %.0fx%.0f -> %.0fx%.0f".formatted(
                                        before[0], before[1], now.width(), now.height()));
                                return now.width() > before[0] + 10 && now.height() > before[1] + 10;
                            })
                            .screenshot("04_settings_resized");
                })

                // The File menu prints the chord next to each entry. It used to be spelled out in the
                // translation, which is a lie the moment a shortcut is rebound.
                .group("menu entries show the chord the keymap has now", g -> g
                        .step("close the settings dialog", ctx -> ctx.query().type(Dialog.class).nth(0)
                                .one().as(Dialog.class).close())
                        .settleMs(150)
                        .step("move the settings action onto ctrl+alt+g", ctx -> {
                            var editor = editor(ctx);
                            KeymapSettings.of(editor).setBindings(action(ctx, EditorActions.SETTINGS),
                                    Keymap.Bindings.of(KeyChord.ctrlAlt(GLFW.GLFW_KEY_G), KeyChord.UNBOUND));
                            editor.getEditorSettings().applyCurrentSettings();
                        })
                        .step("open the file menu", ctx -> {
                            var tab = textElementSaying(ctx, "File");
                            ctx.input().moveTo(tab.centerX(), tab.centerY());
                            ctx.input().mouseDown(tab.centerX(), tab.centerY(), Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(tab.centerX(), tab.centerY(), Keys.MOUSE_LEFT);
                        })
                        .settleMs(200)
                        .check("the settings entry reads the chord it is bound to now",
                                ctx -> menuShows(ctx, "Ctrl+Alt+G"))
                        .check("and no longer the default it was built with",
                                ctx -> !menuShows(ctx, "Ctrl+Alt+S"))
                        .screenshot("05_file_menu"))

                // Tab switching used to need the focus to already be inside the view, which is the one
                // moment the user is least likely to be in: they just came from the menu bar, or the
                // editor has only just opened.
                .group("tab switching does not need the view to be focused", g -> g
                        .step("click in the panel that has two tabs", ctx -> {
                            var bounds = ctx.query().type(ViewContainer.class)
                                    .where(element -> element instanceof ViewContainer container
                                            && container.getAllViews().size() > 1)
                                    .nth(0).one().bounds();
                            ctx.input().moveTo(bounds.centerX(), bounds.centerY());
                            ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
                        })
                        .settleMs(120)
                        .step("then move the focus right out of it, onto the editor", ctx -> editor(ctx).focus())
                        .check("the focus really is outside every view", ctx -> {
                            var focused = ctx.requireUI().getFocusedElement();
                            return focused != null && focused.getFirstAncestorOfType(ViewContainer.class) == null;
                        })
                        .step("remember which tab is up", ctx -> ctx.put(SELECTED_VIEW, selectedTab(ctx)))
                        .key(GLFW.GLFW_KEY_PAGE_DOWN, Keys.MOD_CONTROL)
                        .settleMs(120)
                        .check("the next tab came up anyway",
                                ctx -> !ctx.<String>get(SELECTED_VIEW).equals(selectedTab(ctx)))
                        .key(GLFW.GLFW_KEY_PAGE_UP, Keys.MOD_CONTROL)
                        .settleMs(120)
                        .check("and back", ctx -> ctx.<String>get(SELECTED_VIEW).equals(selectedTab(ctx)))
                        .screenshot("06_tabs"))

                .closeScreen();
    }

    /** The editor sits under a plain root so that a query finds it like any other element. */
    private static ModularUI buildEditorUI(TestContext ctx) {
        var root = new UIElement().setId("root");
        root.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        root.addChild(new TestEditor());
        return new ModularUI(UI.of(root), ctx.player());
    }

    private static void applyTheme(TestContext ctx, String theme) {
        var engine = ctx.requireUI().getStyleEngine();
        engine.clearAllStylesheets();
        engine.addStylesheet(StylesheetManager.INSTANCE.getStylesheetSafe(themeLocation(theme)));
    }

    private static ResourceLocation themeLocation(String theme) {
        return LDLib2.id(StylesheetManager.PATH + "/" + theme + ".lss");
    }

    private static ElementBounds dialogBounds(TestContext ctx) {
        return ctx.query().select(".__dialog_overlay__").nth(0).one().bounds();
    }

    /** Any text anywhere on screen — menu rows build their own labels, so nothing is excluded here. */
    private static boolean menuShows(TestContext ctx, String text) {
        return ctx.query().type(TextElement.class).list().stream()
                .map(ref -> ref.as(TextElement.class).getText().getString())
                .anyMatch(shown -> shown.contains(text));
    }

    private static ElementBounds textElementSaying(TestContext ctx, String text) {
        return ctx.query().type(TextElement.class)
                .where(element -> element instanceof TextElement label
                        && text.equals(label.getText().getString()))
                .nth(0).one().bounds();
    }

    /** The name of the tab showing in the first container that has more than one. */
    private static String selectedTab(TestContext ctx) {
        var container = ctx.query().type(ViewContainer.class)
                .where(element -> element instanceof ViewContainer view && view.getAllViews().size() > 1)
                .nth(0).one().as(ViewContainer.class);
        return container.getAllViews().stream()
                .filter(container::isViewSelected)
                .map(View::getName)
                .findFirst()
                .orElse("<none>");
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static EditorAction action(TestContext ctx, ResourceLocation id) {
        return editor(ctx).getKeymap().getAction(id).orElseThrow();
    }

    /** What the settings page currently has this action bound to. */
    private static KeyChord binding(TestContext ctx, ResourceLocation id) {
        return KeymapSettings.of(editor(ctx)).bindingsOf(action(ctx, id)).primary();
    }

    /**
     * The chord field showing this chord. Found by what it shows rather than by position: the page
     * groups rows by category, and it rebuilds itself after every change.
     */
    private static KeyChordField captureField(TestContext ctx, KeyChord chord) {
        return ctx.query().select("." + KeyChordField.FIELD_CLASS)
                .where(element -> element instanceof KeyChordField field && field.getChord().equals(chord))
                .one().as(KeyChordField.class);
    }

    private static TextField field(TestContext ctx) {
        return ctx.get(FIELD);
    }

    private static String fieldText(TestContext ctx) {
        return field(ctx).getValue();
    }

    private static int runs(TestContext ctx, String key) {
        return ctx.<AtomicInteger>get(key).get();
    }
}
