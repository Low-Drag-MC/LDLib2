package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.IResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.browser.ResourceBehaviorCache;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ElementRef;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

/**
 * A folder becomes a resource provider from the asset browser's menu, and the resource grid it shows
 * up in can be sorted and searched. The textures settings are saved, so the teardown restores them.
 */
@LDLRegisterClient(name = "resource_provider_from_folder", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceProviderFromFolderScenario implements UIScenario {
    private static final String ROOT = "provider_root";
    private static final String FOLDER = "textures_src";
    private static final String OLD_SORT = "old_sort";
    private static final String OLD_ASCENDING = "old_ascending";

    private static final String LEAF_ROW = "__menu_leaf-node__";
    private static final String BRANCH_ROW = "__menu_branch-node__";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "assets", "resources");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("load a project, so there are resource types to offer", ctx -> {
                    var view = editor(ctx).resourceView;
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    editor(ctx).loadProject(new TestProject(), null);
                })
                .waitUntil("the resource tabs are up", ctx -> !editor(ctx).resourceView.getResourceTabs().isEmpty())
                .step("point the browser at a folder that is no provider yet", ctx -> {
                    var instance = instanceOf();
                    ctx.put(OLD_SORT, instance.getSortMode());
                    ctx.put(OLD_ASCENDING, instance.isSortAscending());
                    var root = new File(ctx.mc().gameDirectory, "ldlib2-uitest/provider-folder");
                    deleteRecursively(root);
                    ctx.put(ROOT, root);
                    ctx.require("could create the folder", folder(ctx).mkdirs());
                    ctx.require("the folder is no textures provider yet",
                            ResourceBehaviorCache.findFileProvider(instance, folder(ctx)) == null);
                    var view = editor(ctx).resourceView;
                    view.tabView.selectTab(view.getAssetBrowserTab());
                    view.getAssetBrowser().setRoot(root);
                })
                .waitUntil("the folder is listed", ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, FOLDER) != null)

                .group("the folder's menu adds it as a provider", g -> g
                        .step("right click the folder", ctx -> rightClickFolder(ctx))
                        .waitUntil("the menu is up", ctx -> ctx.query().type(Menu.class).count() > 0)
                        .step("open the provider submenu", ctx -> hoverMenuEntry(ctx, BRANCH_ROW,
                                Component.translatable("editor.assets.add_provider").getString()))
                        .waitUntil("the submenu is up", ctx -> ctx.query().type(Menu.class).count() > 1)
                        .screenshot("01_add_provider_menu")
                        .step("pick textures", ctx -> clickMenuEntry(ctx,
                                TexturesResource.INSTANCE.getDisplayName().getString()))
                        .step("the folder is a registered textures provider", ctx -> {
                            var instance = instanceOf();
                            var provider = ResourceBehaviorCache.findFileProvider(instance, folder(ctx));
                            ctx.require("the folder became a provider", provider != null);
                            var custom = instance.getCustomProviders().values().stream()
                                    .anyMatch(list -> list.contains(provider));
                            ctx.check("it was added as a custom provider, which is saved", custom);
                        })
                        .step("close the menus", ResourceProviderFromFolderScenario::clickAway)
                        .waitUntil("the menus are gone", ctx -> ctx.query().type(Menu.class).count() == 0)
                        .step("right click the folder again", ctx -> rightClickFolder(ctx))
                        .waitUntil("the menu is up again", ctx -> ctx.query().type(Menu.class).count() > 0)
                        .step("open the provider submenu again", ctx -> hoverMenuEntry(ctx, BRANCH_ROW,
                                Component.translatable("editor.assets.add_provider").getString()))
                        .waitUntil("the submenu is up again", ctx -> ctx.query().type(Menu.class).count() > 1)
                        .check("textures is not offered twice", ctx -> menuEntry(ctx, LEAF_ROW,
                                TexturesResource.INSTANCE.getDisplayName().getString()) == null)
                        .step("close the menus again", ResourceProviderFromFolderScenario::clickAway)
                        .waitUntil("the menus are gone again", ctx -> ctx.query().type(Menu.class).count() == 0))

                .group("the textures tab lists the folder's resources", g -> g
                        .step("put three textures in the folder, out of name order", ctx -> {
                            var provider = provider(ctx);
                            add(provider, "b_red", ColorPattern.RED.rectTexture());
                            add(provider, "a_green", ColorPattern.GREEN.rectTexture());
                            add(provider, "c_blue", ColorPattern.BLUE.rectTexture());
                            instanceOf().setSortMode(Resource.SortMode.DEFAULT);
                            instanceOf().setSortAscending(true);
                            editor(ctx).resourceView.selectResourceInstance(TexturesResource.INSTANCE);
                        })
                        .waitUntil("the textures tab is up", ctx -> texturesContainer(ctx) != null)
                        .step("show the folder's provider", ctx -> {
                            var container = texturesContainer(ctx);
                            // without waiting for its own poll
                            container.loadResource();
                            container.selectProvider(provider(ctx));
                            grid(ctx).searchField.setId("resource_search");
                        })
                        .waitUntil("its three textures are listed", ctx -> visibleNames(ctx).size() == 3))

                .group("the sort menu orders the grid by name", g -> g
                        .step("open the sort menu", ctx -> {
                            var button = grid(ctx).toolbar.getChildren().getFirst();
                            var x = button.getPositionX() + button.getSizeWidth() / 2f;
                            var y = button.getPositionY() + button.getSizeHeight() / 2f;
                            ctx.input().moveTo(x, y);
                            ctx.input().mouseDown(x, y, Keys.MOUSE_LEFT);
                            ctx.input().mouseUp(x, y, Keys.MOUSE_LEFT);
                        })
                        .waitUntil("the sort menu is up", ctx -> ctx.query().type(Menu.class).count() > 0)
                        .screenshot("02_sort_menu")
                        .step("pick name", ctx -> clickMenuEntry(ctx,
                                Component.translatable("editor.resource.sort_name").getString()))
                        .waitUntil("the grid is in name order", ctx ->
                                visibleNames(ctx).equals(List.of("a_green", "b_red", "c_blue")))
                        .check("the order is saved with the type", ctx ->
                                instanceOf().getSortMode() == Resource.SortMode.NAME)
                        .step("sort descending", ctx -> grid(ctx).setSortAscending(false))
                        .waitUntil("the grid is in reverse name order", ctx ->
                                visibleNames(ctx).equals(List.of("c_blue", "b_red", "a_green"))))

                .group("the search box filters the grid", g -> g
                        .typeInto("#resource_search", "red")
                        .waitUntil("only the match is showing", ctx -> visibleNames(ctx).equals(List.of("b_red")))
                        .check("the rest are hidden, not thrown away", ctx -> allNames(ctx).size() == 3)
                        .screenshot("03_searched")
                        .step("switch to another provider and back", ctx -> {
                            var container = texturesContainer(ctx);
                            var other = otherProvider(ctx);
                            ctx.require("textures has another provider to switch to", other != null);
                            container.selectProvider(other);
                            container.selectProvider(provider(ctx));
                        })
                        .check("the search came along", ctx -> grid(ctx).getSearchText().equals("red"))
                        .waitUntil("and still filters", ctx -> visibleNames(ctx).equals(List.of("b_red"))))

                .group("a resource made while it would be filtered out clears the search", g -> g
                        .step("add one the search does not match", ctx ->
                                grid(ctx).addNewResource(ColorPattern.WHITE.rectTexture(), "z_white"))
                        .check("the search was cleared", ctx -> grid(ctx).getSearchText().isEmpty())
                        .waitUntil("it is showing, in sorted place", ctx -> visibleNames(ctx).equals(
                                List.of("z_white", "c_blue", "b_red", "a_green")))
                        .screenshot("04_new_resource"))

                // a file provider lists one folder, so "sub/..." is a name it cannot store; the rename
                // used to remove the old file anyway
                .group("a rename the provider cannot store keeps the resource", g -> g
                        .check("the rename is refused", ctx -> grid(ctx).renameResourceTo(
                                provider(ctx).createSubPath("a_green"), "sub/a_green") == null)
                        .check("and the resource is still there", ctx ->
                                provider(ctx).hasResource(provider(ctx).createSubPath("a_green"))))

                // teardowns run after the screen is closed
                .teardown("put the textures settings back and remove the folder", ctx -> {
                    var instance = instanceOf();
                    var provider = ctx.<File>get(ROOT) == null ? null
                            : ResourceBehaviorCache.findFileProvider(instance, folder(ctx));
                    if (provider != null) {
                        instance.removeCustomProvider(provider);
                    }
                    var sort = ctx.<Resource.SortMode>get(OLD_SORT);
                    if (sort != null) instance.setSortMode(sort);
                    var ascending = ctx.<Boolean>get(OLD_ASCENDING);
                    if (ascending != null) instance.setSortAscending(ascending);
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

    private static File folder(TestContext ctx) {
        return new File(ctx.<File>get(ROOT), FOLDER);
    }

    private static ResourceInstance<IGuiTexture> instanceOf() {
        return TexturesResource.INSTANCE.getResourceInstance();
    }

    private static IResourceProvider<IGuiTexture> provider(TestContext ctx) {
        var provider = ResourceBehaviorCache.findFileProvider(instanceOf(), folder(ctx));
        ctx.require("the folder is a textures provider", provider != null);
        return provider;
    }

    @Nullable
    private static IResourceProvider<IGuiTexture> otherProvider(TestContext ctx) {
        var mine = provider(ctx);
        return instanceOf().getBuiltinProviders().values().stream().flatMap(List::stream)
                .filter(provider -> provider != mine).findFirst().orElse(null);
    }

    private static void add(IResourceProvider<IGuiTexture> provider, String name, IGuiTexture texture) {
        provider.addResource(provider.createSubPath(name), texture);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static ResourceContainer<IGuiTexture> texturesContainer(TestContext ctx) {
        return ctx.query().type(ResourceContainer.class).list().stream()
                .map(ref -> (ResourceContainer<IGuiTexture>) ref.as(ResourceContainer.class))
                .filter(container -> container.resourceInstance == instanceOf())
                .findFirst().orElse(null);
    }

    private static ResourceProviderContainer<IGuiTexture> grid(TestContext ctx) {
        var container = texturesContainer(ctx);
        ctx.require("the textures tab is up", container != null);
        var grid = container.getSelectedProviderContainer();
        ctx.require("a provider is selected", grid != null);
        return grid;
    }

    private static List<String> visibleNames(TestContext ctx) {
        return grid(ctx).scrollerView.viewContainer.getChildren().stream()
                .filter(UIElement::isDisplayed).map(ResourceProviderFromFolderScenario::labelOf).toList();
    }

    private static List<String> allNames(TestContext ctx) {
        return grid(ctx).scrollerView.viewContainer.getChildren().stream()
                .map(ResourceProviderFromFolderScenario::labelOf).toList();
    }

    @Nullable
    private static UIElement cellNamed(UIElement container, String name) {
        for (var cell : container.getChildren()) {
            if (name.equals(labelOf(cell))) return cell;
        }
        return null;
    }

    private static String labelOf(UIElement cell) {
        return cell.getChildren().isEmpty() || !(cell.getChildren().getLast() instanceof TextElement label)
                ? "" : label.getText().getString();
    }

    /** One step: the menu opens under the pointer, a re-resolved release would land on it. */
    private static void rightClickFolder(TestContext ctx) {
        var cell = cellNamed(browser(ctx).gridScroller.viewContainer, FOLDER);
        ctx.require("the folder is on show", cell != null);
        var x = cell.getPositionX() + cell.getSizeWidth() / 2f;
        var y = cell.getPositionY() + cell.getSizeHeight() / 2f;
        ctx.input().moveTo(x, y);
        ctx.input().mouseDown(x, y, Keys.MOUSE_RIGHT);
        ctx.input().mouseUp(x, y, Keys.MOUSE_RIGHT);
    }

    /** Clicks the bottom bar's path, which does nothing but close the menus. */
    private static void clickAway(TestContext ctx) {
        var label = browser(ctx).bottomBar.pathLabel;
        var x = label.getPositionX() + label.getSizeWidth() / 2f;
        var y = label.getPositionY() + label.getSizeHeight() / 2f;
        ctx.input().moveTo(x, y);
        ctx.input().mouseDown(x, y, Keys.MOUSE_LEFT);
        ctx.input().mouseUp(x, y, Keys.MOUSE_LEFT);
    }

    private static void hoverMenuEntry(TestContext ctx, String styleClass, String label) {
        var entry = menuEntry(ctx, styleClass, label);
        ctx.require("the menu has an entry called " + label, entry != null);
        var bounds = entry.bounds();
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
    }

    private static void clickMenuEntry(TestContext ctx, String label) {
        var entry = menuEntry(ctx, LEAF_ROW, label);
        ctx.require("the menu has an entry for " + label, entry != null);
        var bounds = entry.bounds();
        ctx.input().moveTo(bounds.centerX(), bounds.centerY());
        ctx.input().mouseDown(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(bounds.centerX(), bounds.centerY(), Keys.MOUSE_LEFT);
    }

    @Nullable
    private static ElementRef menuEntry(TestContext ctx, String styleClass, String label) {
        return ctx.query().withClass(styleClass)
                .where(element -> element.selfAndAllChildren()
                        .anyMatch(child -> child instanceof TextElement text
                                && text.getText().getString().equals(label)))
                .optional().orElse(null);
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
