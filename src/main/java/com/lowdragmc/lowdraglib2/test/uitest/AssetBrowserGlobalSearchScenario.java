package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.util.FileNode;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

/**
 * The asset browser's global search finds matches anywhere under the root, and clearing the search box
 * lands in the picked result's folder with it selected.
 */
@LDLRegisterClient(name = "asset_browser_global_search", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class AssetBrowserGlobalSearchScenario implements UIScenario {
    private static final String ROOT = "search_root";
    private static final String SEARCH = "#asset_search";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "assets", "search");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .check("a fresh browser searches the open folder only", ctx -> !browser(ctx).isGlobalSearch())
                .step("point the browser at a tree with matches at every depth", ctx -> {
                    var root = new File(ctx.mc().gameDirectory, "ldlib2-uitest/global-search");
                    deleteRecursively(root);
                    ctx.put(ROOT, root);
                    for (var dir : List.of("blocks/stone", "blocks/needle_folder", "items")) {
                        ctx.require("could create " + dir, new File(root, dir).mkdirs());
                    }
                    for (var path : List.of("blocks/stone/needle_deep.txt", "items/needle_item.txt", "haystack.txt")) {
                        try {
                            Files.writeString(new File(root, path).toPath(), path);
                        } catch (IOException e) {
                            ctx.require("could write " + path + ": " + e, false);
                        }
                    }
                    var view = editor(ctx).resourceView;
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    view.tabView.selectTab(view.getAssetBrowserTab());
                    var browser = view.getAssetBrowser();
                    browser.setRoot(root);
                    browser.setShowAllFiles(true);
                    browser.searchField.setId("asset_search");
                })
                .waitUntil("the root is listed", ctx ->
                        gridNames(ctx).containsAll(List.of("blocks", "items", "haystack.txt")))

                .group("the folder search does not look into sub folders", g -> g
                        .typeInto(SEARCH, "needle")
                        .waitUntil("the decoy is filtered out", ctx -> !gridNames(ctx).contains("haystack.txt"))
                        .step("no nested match turns up", ctx -> {
                            var names = gridNames(ctx);
                            ctx.check("only the open folder's own entries are listed",
                                    !names.contains("needle_deep.txt") && !names.contains("needle_item.txt"),
                                    "no nested matches", names);
                        })
                        .check("the breadcrumb still shows the path", ctx ->
                                !browser(ctx).breadcrumb.hasChild(browser(ctx).searchStatus)))

                .group("switching the scope finds matches anywhere under the root", g -> g
                        .click(".__asset-browser_search-scope__")
                        .check("the scope is global now", ctx -> browser(ctx).isGlobalSearch())
                        .waitUntil("the search has finished", ctx -> statusText(ctx).equals(doneText(ctx, 3)))
                        .step("folders first, then by name", ctx -> {
                            var expected = List.of("needle_folder", "needle_deep.txt", "needle_item.txt");
                            var actual = gridNames(ctx);
                            ctx.check("the results are in grid order", actual.equals(expected), expected, actual);
                        })
                        .check("the status line has taken the breadcrumb's place", ctx ->
                                browser(ctx).breadcrumb.hasChild(browser(ctx).searchStatus))
                        .screenshot("01_global_results"))

                .group("the results follow the sort", g -> g
                        .step("sort descending", ctx -> browser(ctx).setSortAscending(false))
                        .waitUntil("the results are re-sorted", ctx -> gridNames(ctx).equals(
                                List.of("needle_folder", "needle_item.txt", "needle_deep.txt")))
                        .step("sort ascending again", ctx -> browser(ctx).setSortAscending(true))
                        .waitUntil("the results are back in order", ctx -> gridNames(ctx).equals(
                                List.of("needle_folder", "needle_deep.txt", "needle_item.txt"))))

                .group("clearing the search goes to the picked result", g -> g
                        .step("name the deep result's cell", ctx -> {
                            var cell = cellNamed(ctx, "needle_deep.txt");
                            ctx.require("the deep result is on show", cell != null);
                            cell.setId("deep_hit");
                        })
                        .click("#deep_hit")
                        .check("the result is selected", ctx ->
                                file(ctx, "blocks/stone/needle_deep.txt").equals(browser(ctx).getSelectedFile()))
                        .typeInto(SEARCH, "")
                        .waitUntil("the browser is in the result's folder", ctx ->
                                file(ctx, "blocks/stone").equals(browser(ctx).getCurrentDirectory())
                                        && gridNames(ctx).contains("needle_deep.txt"))
                        .check("the result is still selected there", ctx ->
                                file(ctx, "blocks/stone/needle_deep.txt").equals(browser(ctx).getSelectedFile()))
                        .step("the tree has followed to the folder", ctx -> {
                            var selected = browser(ctx).tree.getSelected().stream().map(FileNode::getKey).toList();
                            var expected = List.of(file(ctx, "blocks/stone"));
                            ctx.check("the tree selects the result's folder", selected.equals(expected), expected, selected);
                        })
                        .check("the breadcrumb is back", ctx ->
                                !browser(ctx).breadcrumb.hasChild(browser(ctx).searchStatus))
                        .check("the scope stays global for the next search", ctx -> browser(ctx).isGlobalSearch())
                        .screenshot("02_revealed"))

                .group("double clicking a folder result opens it", g -> g
                        .typeInto(SEARCH, "needle_f")
                        .waitUntil("the folder is the only result", ctx ->
                                statusText(ctx).equals(doneText(ctx, 1))
                                        && gridNames(ctx).equals(List.of("needle_folder")))
                        .step("double click it", ctx -> {
                            var cell = cellNamed(ctx, "needle_folder");
                            ctx.require("the folder result is on show", cell != null);
                            // one step: the grid may be rebuilt before a re-resolved second click
                            var x = cell.getPositionX() + cell.getSizeWidth() / 2f;
                            var y = cell.getPositionY() + cell.getSizeHeight() / 2f;
                            ctx.input().moveTo(x, y);
                            for (var i = 0; i < 2; i++) {
                                ctx.input().mouseDown(x, y, Keys.MOUSE_LEFT);
                                ctx.input().mouseUp(x, y, Keys.MOUSE_LEFT);
                            }
                        })
                        .waitUntil("the folder is open", ctx ->
                                file(ctx, "blocks/needle_folder").equals(browser(ctx).getCurrentDirectory()))
                        .check("the search box was cleared", ctx -> browser(ctx).searchField.getText().isEmpty())
                        .check("the grid is a folder again", ctx -> !browser(ctx).isGlobalSearchActive()))

                // teardowns run after the screen is closed
                .teardown("remove the temporary tree", ctx -> {
                    var root = ctx.<File>get(ROOT);
                    if (root != null) deleteRecursively(root);
                })
                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static AssetBrowser browser(TestContext ctx) {
        return editor(ctx).resourceView.getAssetBrowser();
    }

    private static File file(TestContext ctx, String path) {
        return new File(ctx.<File>get(ROOT), path);
    }

    /** In grid order, which the query's registration order is not. */
    private static List<String> gridNames(TestContext ctx) {
        return browser(ctx).gridScroller.viewContainer.getChildren().stream()
                .map(AssetBrowserGlobalSearchScenario::labelOf).toList();
    }

    @Nullable
    private static UIElement cellNamed(TestContext ctx, String name) {
        for (var cell : browser(ctx).gridScroller.viewContainer.getChildren()) {
            if (name.equals(labelOf(cell))) return cell;
        }
        return null;
    }

    private static String labelOf(UIElement cell) {
        return cell.getChildren().isEmpty() || !(cell.getChildren().getLast() instanceof TextElement label)
                ? "" : label.getText().getString();
    }

    private static String statusText(TestContext ctx) {
        return browser(ctx).searchStatus.getText().getString();
    }

    private static String doneText(TestContext ctx, int found) {
        return I18n.get("editor.assets.search_done", ctx.<File>get(ROOT).getName(), found);
    }

    private static void deleteRecursively(File root) {
        if (!root.exists()) return;
        try (var paths = Files.walk(root.toPath())) {
            //noinspection ResultOfMethodCallIgnored
            paths.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        } catch (IOException ignored) {
        }
    }
}
