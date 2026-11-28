package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
import com.lowdragmc.lowdraglib2.editor.ui.resource.GridMarqueeSelection;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceContainer;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.uitest.ElementBounds;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Verifies multi-selection (Ctrl+Click, Shift+Click, marquee drag, empty-space click to clear),
 * batch operations (copy, paste, delete, drag-move, undo), and wide floating inline rename fields
 * in both standalone {@link ResourceProviderContainer} and {@link AssetBrowser}.
 */
@LDLRegisterClient(name = "resource_multiselect_and_rename", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceMultiSelectAndRenameScenario implements UIScenario {

    private static final String RES_DIR = "res_dir";
    private static final String ASSET_DIR = "asset_dir";
    private static final String COLOR_PROVIDER = "color_provider";
    private static final String ORIG_DISPLAY_MODE = "orig_display_mode";
    private static final String ORIG_UI_WIDTH = "orig_ui_width";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "resources", "assets");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()).shouldCloseOnEsc(false))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("load a project so resource tabs exist", ctx -> {
                    var ed = editor(ctx);
                    var view = ed.resourceView;
                    var vc = view.getViewContainer();
                    ctx.require("resource view is docked", vc != null);
                    vc.selectView(view);
                    ed.loadProject(new TestProject(), null);
                })
                .waitUntil("resource tabs are populated", ctx -> !editor(ctx).resourceView.getResourceTabs().isEmpty())

                // =========================================================================
                // PART 1: Standalone ResourceProviderContainer (ColorsResource)
                // =========================================================================
                .group("standalone resource provider: multi-select, batch ops, wide rename", g -> {
                    g.step("create a custom file provider for ColorsResource with 4 items in narrow grid mode", ctx -> {
                        var dir = new File(ctx.mc().gameDirectory, "ldlib2-uitest/res-multiselect");
                        FileOps.deleteRecursively(dir);
                        ctx.require("created temp color dir", dir.mkdirs());
                        ctx.put(RES_DIR, dir);

                        var instance = ColorsResource.INSTANCE.getResourceInstance();
                        ctx.put(ORIG_DISPLAY_MODE, instance.getDisplayMode());
                        ctx.put(ORIG_UI_WIDTH, instance.getUiWidth());

                        var provider = new FileResourceProvider<>(instance, dir);
                        provider.setName("uitest_colors");
                        provider.addResource(provider.createSubPath("alpha"), 0xFFFF0000);
                        provider.addResource(provider.createSubPath("beta"), 0xFF00FF00);
                        provider.addResource(provider.createSubPath("gamma"), 0xFF0000FF);
                        provider.addResource(provider.createSubPath("delta"), 0xFFFFFF00);
                        instance.addCustomProvider(provider);
                        ctx.put(COLOR_PROVIDER, provider);

                        var view = editor(ctx).resourceView;
                        view.selectResourceInstance(ColorsResource.INSTANCE);
                        var container = colorContainer(ctx);
                        ctx.require("color resource container exists", container != null);
                        container.loadResource();
                        container.selectProvider(provider);
                        var pc = colorProviderContainer(ctx);
                        ctx.require("provider container is ready", pc != null);
                        pc.setDisplayMode(Resource.DisplayMode.GRID);
                        pc.setUiWidth(15, false);
                        pc.setSortMode(Resource.SortMode.NAME);
                        pc.setSortAscending(true);
                    })
                    .waitUntil("4 color cells are laid out", ctx ->
                            colorProviderContainer(ctx).scrollerView.viewContainer.getChildren().size() == 4
                                    && cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha") != null)

                    // Empty-space click clears selection
                    .click("select alpha", ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha"), Keys.MOUSE_LEFT)
                    .check("alpha is selected", ctx -> selectedResourceNames(ctx).equals(Set.of("alpha")))
                    .step("click empty space in resource viewport", ctx -> {
                        var vp = colorProviderContainer(ctx).scrollerView.viewPort;
                        float ex = vp.getPositionX() + vp.getSizeWidth() - 12f;
                        float ey = vp.getPositionY() + vp.getSizeHeight() - 12f;
                        ctx.input().moveTo(ex, ey);
                        ctx.input().mouseDown(ex, ey, Keys.MOUSE_LEFT);
                        ctx.input().mouseUp(ex, ey, Keys.MOUSE_LEFT);
                    })
                    .check("empty-space click clears selection", ctx ->
                            colorProviderContainer(ctx).getSelectedResources().isEmpty()
                                    && colorProviderContainer(ctx).getSelected() == null)

                    // Ctrl+Click multi-selection and toggle
                    .click("select alpha", ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha"), Keys.MOUSE_LEFT)
                    .step("ctrl+click gamma", ctx -> clickWithMod(ctx,
                            cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "gamma"), Keys.MOD_CONTROL))
                    .check("ctrl+click selects both alpha and gamma", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("alpha", "gamma")))
                    .check("bottom bar shows selection count", ctx ->
                            colorProviderContainer(ctx).bottomBar.pathLabel.getText().getString().contains("2"))
                    .step("ctrl+click alpha again to deselect it", ctx -> clickWithMod(ctx,
                            cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha"), Keys.MOD_CONTROL))
                    .check("ctrl+click toggles alpha off leaving gamma", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("gamma")))

                    // Shift+Click range selection (alphabetical order: alpha, beta, delta, gamma)
                    .click("select beta as anchor", ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "beta"), Keys.MOUSE_LEFT)
                    .step("shift+click gamma for range", ctx -> clickWithMod(ctx,
                            cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "gamma"), Keys.MOD_SHIFT))
                    .check("shift+click selects beta, delta, gamma", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("beta", "delta", "gamma")))

                    // Marquee drag selection
                    .step("clear selection before marquee", ctx -> colorProviderContainer(ctx).selectResource(null, false));
                    dragMarquee(g,
                            ctx -> colorProviderContainer(ctx).scrollerView.viewPort,
                            ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha"),
                            ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "beta"));
                    g.check("marquee drag selected alpha and beta", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("alpha", "beta")))
                    .check("marquee rect removed after drag end", ctx ->
                            ctx.query().withClass(GridMarqueeSelection.CLASS_SELECTION_RECT).count() == 0)

                    // Batch Copy via right-click context menu + Undo
                    .click("right-click alpha while alpha+beta selected",
                            ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha"), Keys.MOUSE_RIGHT)
                    .check("right-clicking a selected item preserves multi-selection", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("alpha", "beta")))
                    .step("click Copy in context menu", ctx -> clickMenuLeaf(ctx, "ldlib.gui.editor.menu.copy"))
                    .waitUntil("batch copy dialog opened", ctx -> ctx.query().withClass("__confirm-button__").count() == 1)
                    .click("confirm batch copy", ctx -> ctx.query().withClass("__confirm-button__").one().element(), Keys.MOUSE_LEFT)
                    .waitUntil("2 copies created (6 total)", ctx ->
                            colorProviderContainer(ctx).scrollerView.viewContainer.getChildren().size() == 6)
                    .step("undo batch copy", ctx -> editor(ctx).historyView.undo())
                    .waitUntil("undo restores 4 items", ctx ->
                            colorProviderContainer(ctx).scrollerView.viewContainer.getChildren().size() == 4)

                    // Batch Remove via right-click context menu + Undo
                    .click("select gamma", ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "gamma"), Keys.MOUSE_LEFT)
                    .step("ctrl+click delta", ctx -> clickWithMod(ctx,
                            cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "delta"), Keys.MOD_CONTROL))
                    .check("gamma and delta selected", ctx ->
                            selectedResourceNames(ctx).equals(Set.of("gamma", "delta")))
                    .click("right-click gamma",
                            ctx -> cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "gamma"), Keys.MOUSE_RIGHT)
                    .step("click Remove in context menu", ctx -> clickMenuLeaf(ctx, "ldlib.gui.editor.menu.remove"))
                    .waitUntil("batch remove confirmation dialog opened", ctx -> ctx.query().withClass("__confirm-button__").count() == 1)
                    .click("confirm batch remove", ctx -> ctx.query().withClass("__confirm-button__").one().element(), Keys.MOUSE_LEFT)
                    .waitUntil("gamma and delta removed (2 left)", ctx ->
                            colorProviderContainer(ctx).scrollerView.viewContainer.getChildren().size() == 2)
                    .step("undo batch remove", ctx -> editor(ctx).historyView.undo())
                    .waitUntil("undo restores all 4 items", ctx ->
                            colorProviderContainer(ctx).scrollerView.viewContainer.getChildren().size() == 4)

                    // Wide floating inline rename at narrow uiWidth = 15 (Escape cancel + Enter commit)
                    .step("start inline rename on alpha and cancel with Escape", ctx -> {
                        var pc = colorProviderContainer(ctx);
                        var alphaPath = pc.resourceProvider.createSubPath("alpha");
                        pc.selectResource(alphaPath, false);
                        pc.renameResource(alphaPath);
                    })
                    .waitUntil("inline rename field appeared for cancel test", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).count() == 1)
                    .step("type text then press Escape", ctx -> {
                        var field = (TextField) ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).one().element();
                        field.setText("should_not_commit", false);
                        ctx.input().keyDown(GLFW.GLFW_KEY_ESCAPE, 0);
                        ctx.input().keyUp(GLFW.GLFW_KEY_ESCAPE, 0);
                    })
                    .waitUntil("rename field closed after Escape", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).count() == 0)
                    .check("Escape cancelled rename and restored original label", ctx ->
                            cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha") != null
                                    && cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "should_not_commit") == null)

                    .step("start inline rename on alpha at narrow uiWidth=15", ctx -> {
                        var pc = colorProviderContainer(ctx);
                        var alphaPath = pc.resourceProvider.createSubPath("alpha");
                        pc.selectResource(alphaPath, false);
                        pc.renameResource(alphaPath);
                    })
                    .waitUntil("inline rename field appeared", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).count() == 1)
                    .step("verify rename field is wider than narrow cell", ctx -> {
                        var field = ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).one().element();
                        var cell = cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "");
                        ctx.check("rename field width >= INLINE_RENAME_MIN_WIDTH (90)",
                                field.getSizeWidth() >= ResourceProviderContainer.INLINE_RENAME_MIN_WIDTH - 0.5f,
                                ">= " + ResourceProviderContainer.INLINE_RENAME_MIN_WIDTH, field.getSizeWidth());
                        if (cell != null) {
                            ctx.check("cell itself remained narrow (~15px)", cell.getSizeWidth() <= 20f, "<= 20", cell.getSizeWidth());
                        }
                    })
                    .screenshot("01_resource_wide_rename")
                    .step("type new name and press Enter", ctx -> {
                        var field = (TextField) ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).one().element();
                        field.setText("alpha_renamed", false);
                        ctx.input().keyDown(GLFW.GLFW_KEY_ENTER, 0);
                        ctx.input().keyUp(GLFW.GLFW_KEY_ENTER, 0);
                    })
                    .waitUntil("rename field closed and alpha_renamed exists", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).count() == 0
                                    && cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha_renamed") != null)
                    .check("label width returned to cell width after rename", ctx -> {
                        var cell = cellNamed(colorProviderContainer(ctx).scrollerView.viewContainer, "alpha_renamed");
                        return cell != null && cell.getSizeWidth() <= 20f;
                    });
                })

                // =========================================================================
                // PART 2: AssetBrowser
                // =========================================================================
                .group("asset browser: multi-select, marquee, batch copy/paste/delete, drag-move, wide rename", g -> {
                    g.step("setup temp asset browser directory with sub/ and a.txt, b.txt, c.txt", ctx -> {
                        var root = new File(ctx.mc().gameDirectory, "ldlib2-uitest/asset-multiselect");
                        FileOps.deleteRecursively(root);
                        ctx.require("created sub dir", new File(root, "sub").mkdirs());
                        writeFile(ctx, new File(root, "a.txt"), "aaa");
                        writeFile(ctx, new File(root, "b.txt"), "bbb");
                        writeFile(ctx, new File(root, "c.txt"), "ccc");
                        ctx.put(ASSET_DIR, root);

                        var view = editor(ctx).resourceView;
                        view.tabView.selectTab(view.getAssetBrowserTab());
                        var ab = view.getAssetBrowser();
                        ab.setShowAllFiles(true);
                        ab.setDisplayMode(Resource.DisplayMode.GRID);
                        ab.setUiWidth(15);
                        ab.setRoot(root);
                    })
                    .waitUntil("asset browser shows sub, a.txt, b.txt, c.txt", ctx ->
                            browser(ctx).gridScroller.viewContainer.getChildren().size() == 4
                                    && cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt") != null)

                    // Empty space click clears selection
                    .click("select a.txt", ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"), Keys.MOUSE_LEFT)
                    .check("a.txt selected", ctx -> selectedAssetNames(ctx).equals(Set.of("a.txt")))
                    .step("click empty space in asset browser viewport", ctx -> {
                        var vp = browser(ctx).gridScroller.viewPort;
                        float ex = vp.getPositionX() + vp.getSizeWidth() - 12f;
                        float ey = vp.getPositionY() + vp.getSizeHeight() - 12f;
                        ctx.input().moveTo(ex, ey);
                        ctx.input().mouseDown(ex, ey, Keys.MOUSE_LEFT);
                        ctx.input().mouseUp(ex, ey, Keys.MOUSE_LEFT);
                    })
                    .check("empty-space click clears asset browser selection", ctx ->
                            browser(ctx).getSelectedFiles().isEmpty() && browser(ctx).getSelectedFile() == null)

                    // Ctrl+Click & Shift+Click
                    .click("select a.txt", ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"), Keys.MOUSE_LEFT)
                    .step("ctrl+click c.txt", ctx -> clickWithMod(ctx,
                            cellNamed(browser(ctx).gridScroller.viewContainer, "c.txt"), Keys.MOD_CONTROL))
                    .check("ctrl+click selected a.txt and c.txt", ctx ->
                            selectedAssetNames(ctx).equals(Set.of("a.txt", "c.txt")))
                    .step("shift+click b.txt", ctx -> clickWithMod(ctx,
                            cellNamed(browser(ctx).gridScroller.viewContainer, "b.txt"), Keys.MOD_SHIFT))
                    .check("shift+click range selected b.txt and c.txt", ctx ->
                            selectedAssetNames(ctx).equals(Set.of("b.txt", "c.txt")))

                    // Marquee drag selection
                    .step("clear selection before marquee", ctx -> browser(ctx).clearSelection());
                    dragMarquee(g,
                            ctx -> browser(ctx).gridScroller.viewPort,
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"),
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "b.txt"));
                    g.check("marquee drag selected a.txt and b.txt", ctx ->
                            selectedAssetNames(ctx).equals(Set.of("a.txt", "b.txt")))
                    .screenshot("02_asset_browser_multiselect")

                    // Batch Copy -> open sub -> Paste -> Batch Delete in sub
                    .click("right-click a.txt with a.txt+b.txt selected",
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"), Keys.MOUSE_RIGHT)
                    .step("click Copy", ctx -> clickMenuLeaf(ctx, "ldlib.gui.editor.menu.copy"))
                    .step("open sub directory", ctx -> browser(ctx).openDirectory(new File(ctx.<File>get(ASSET_DIR), "sub")))
                    .waitUntil("sub directory is open and empty", ctx ->
                            browser(ctx).gridScroller.viewContainer.getChildren().isEmpty())
                    .step("right-click empty space in sub", ctx -> {
                        var vp = browser(ctx).gridScroller.viewPort;
                        float ex = vp.getPositionX() + vp.getSizeWidth() / 2f;
                        float ey = vp.getPositionY() + vp.getSizeHeight() / 2f;
                        ctx.input().moveTo(ex, ey);
                        ctx.input().mouseDown(ex, ey, Keys.MOUSE_RIGHT);
                        ctx.input().mouseUp(ex, ey, Keys.MOUSE_RIGHT);
                    })
                    .step("click Paste", ctx -> clickMenuLeaf(ctx, "editor.assets.paste"))
                    .waitUntil("a.txt and b.txt pasted into sub", ctx ->
                            browser(ctx).gridScroller.viewContainer.getChildren().size() == 2)
                    .step("select all in sub", ctx -> browser(ctx).selectAll())
                    .check("both pasted files selected", ctx ->
                            selectedAssetNames(ctx).equals(Set.of("a.txt", "b.txt")))
                    .click("right-click a.txt in sub",
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"), Keys.MOUSE_RIGHT)
                    .step("click Remove", ctx -> clickMenuLeaf(ctx, "ldlib.gui.editor.menu.remove"))
                    .waitUntil("batch delete confirmation opened", ctx -> ctx.query().withClass("__confirm-button__").count() == 1)
                    .click("confirm batch delete", ctx -> ctx.query().withClass("__confirm-button__").one().element(), Keys.MOUSE_LEFT)
                    .waitUntil("sub is empty again", ctx ->
                            browser(ctx).gridScroller.viewContainer.getChildren().isEmpty())

                    // Back to root: multi-file drag-move into sub/
                    .step("return to root", ctx -> browser(ctx).openDirectory(ctx.get(ASSET_DIR)))
                    .waitUntil("root listed again", ctx ->
                            browser(ctx).gridScroller.viewContainer.getChildren().size() == 4)
                    .click("select a.txt", ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"), Keys.MOUSE_LEFT)
                    .step("ctrl+click b.txt", ctx -> clickWithMod(ctx,
                            cellNamed(browser(ctx).gridScroller.viewContainer, "b.txt"), Keys.MOD_CONTROL))
                    .check("a.txt and b.txt selected for drag-move", ctx ->
                            selectedAssetNames(ctx).equals(Set.of("a.txt", "b.txt")));
                    dragCellOnto(g,
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "a.txt"),
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "sub"));
                    g.waitUntil("both a.txt and b.txt moved into sub", ctx ->
                            new File(ctx.<File>get(ASSET_DIR), "sub/a.txt").exists()
                                    && new File(ctx.<File>get(ASSET_DIR), "sub/b.txt").exists()
                                    && browser(ctx).gridScroller.viewContainer.getChildren().size() == 2)

                    // Wide inline rename in AssetBrowser at narrow uiWidth = 15
                    .click("right-click c.txt",
                            ctx -> cellNamed(browser(ctx).gridScroller.viewContainer, "c.txt"), Keys.MOUSE_RIGHT)
                    .step("click Rename", ctx -> clickMenuLeaf(ctx, "ldlib.gui.editor.menu.rename"))
                    .waitUntil("asset browser rename field opened", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).count() == 1)
                    .check("asset browser rename field width >= 90", ctx ->
                            ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).one()
                                    .element().getSizeWidth() >= ResourceProviderContainer.INLINE_RENAME_MIN_WIDTH - 0.5f)
                    .step("commit rename to c_renamed.txt", ctx -> {
                        var field = (TextField) ctx.query().withClass(ResourceProviderContainer.CLASS_RENAME_FIELD).one().element();
                        field.setText("c_renamed.txt", false);
                        ctx.input().keyDown(GLFW.GLFW_KEY_ENTER, 0);
                        ctx.input().keyUp(GLFW.GLFW_KEY_ENTER, 0);
                    })
                    .waitUntil("c_renamed.txt appears on disk and in grid", ctx ->
                            new File(ctx.<File>get(ASSET_DIR), "c_renamed.txt").exists()
                                    && cellNamed(browser(ctx).gridScroller.viewContainer, "c_renamed.txt") != null);
                })

                .closeScreen()
                .teardown("cleanup temp providers and folders", ctx -> {
                    var provider = ctx.<FileResourceProvider<Integer>>get(COLOR_PROVIDER);
                    if (provider != null) {
                        var instance = ColorsResource.INSTANCE.getResourceInstance();
                        instance.removeCustomProvider(provider);
                        var origMode = ctx.<Resource.DisplayMode>get(ORIG_DISPLAY_MODE);
                        var origWidth = ctx.<Integer>get(ORIG_UI_WIDTH);
                        if (origMode != null) instance.setDisplayMode(origMode);
                        if (origWidth != null) instance.setUiWidth(origWidth, false);
                    }
                    var resDir = ctx.<File>get(RES_DIR);
                    if (resDir != null) FileOps.deleteRecursively(resDir);
                    var assetDir = ctx.<File>get(ASSET_DIR);
                    if (assetDir != null) FileOps.deleteRecursively(assetDir);
                });
    }

    private static void dragMarquee(ScenarioBuilder s,
                                    Function<TestContext, UIElement> viewPortFn,
                                    Function<TestContext, UIElement> firstCellFn,
                                    Function<TestContext, UIElement> secondCellFn) {
        var pts = new float[4]; // startX, startY, endX, endY
        s.step("aim at empty space below cells for marquee", ctx -> {
            var vp = viewPortFn.apply(ctx);
            var c1 = firstCellFn.apply(ctx);
            var c2 = secondCellFn.apply(ctx);
            // Start in empty space below c2, drag up-left across c1..c2
            pts[0] = Math.min(vp.getPositionX() + vp.getSizeWidth() - 6f, c2.getPositionX() + c2.getSizeWidth() + 4f);
            pts[1] = Math.min(vp.getPositionY() + vp.getSizeHeight() - 6f, c2.getPositionY() + c2.getSizeHeight() + 18f);
            pts[2] = Math.max(vp.getPositionX() + 2f, c1.getPositionX() + 2f);
            pts[3] = Math.max(vp.getPositionY() + 2f, c1.getPositionY() + 2f);
            ctx.input().moveTo(pts[0], pts[1]);
        });
        s.step("press on empty space", ctx -> ctx.input().mouseDown(pts[0], pts[1], Keys.MOUSE_LEFT));
        s.step("drag marquee across cells", ctx -> ctx.input().dragTo(pts[2], pts[3], Keys.MOUSE_LEFT));
        s.check("marquee overlay is visible mid-drag", ctx ->
                ctx.query().withClass(GridMarqueeSelection.CLASS_SELECTION_RECT).count() == 1);
        s.step("release marquee", ctx -> ctx.input().mouseUp(pts[2], pts[3], Keys.MOUSE_LEFT));
    }

    private static void dragCellOnto(ScenarioBuilder s,
                                     Function<TestContext, UIElement> sourceFn,
                                     Function<TestContext, UIElement> targetFn) {
        var pts = new float[4];
        s.step("aim at cell to drag", ctx -> {
            var src = sourceFn.apply(ctx);
            var dst = targetFn.apply(ctx);
            pts[0] = src.getPositionX() + src.getSizeWidth() / 2f;
            pts[1] = src.getPositionY() + src.getSizeHeight() / 2f;
            pts[2] = dst.getPositionX() + dst.getSizeWidth() / 2f;
            pts[3] = dst.getPositionY() + dst.getSizeHeight() / 2f;
            ctx.input().moveTo(pts[0], pts[1]);
        });
        s.step("press dragged cell", ctx -> ctx.input().mouseDown(pts[0], pts[1], Keys.MOUSE_LEFT));
        s.step("drag out of cell to start drag", ctx -> ctx.input().dragTo((pts[0] + pts[2]) / 2f, pts[1] + 25f, Keys.MOUSE_LEFT));
        s.step("drag onto target folder", ctx -> ctx.input().dragTo(pts[2], pts[3], Keys.MOUSE_LEFT));
        s.step("drop onto target folder", ctx -> ctx.input().mouseUp(pts[2], pts[3], Keys.MOUSE_LEFT));
    }

    private static void clickWithMod(TestContext ctx, @Nullable UIElement element, int mod) {
        ctx.require("target element exists", element != null);
        var b = ElementBounds.of(element);
        ctx.input().moveTo(b.centerX(), b.centerY());
        int key = mod == Keys.MOD_CONTROL ? GLFW.GLFW_KEY_LEFT_CONTROL : GLFW.GLFW_KEY_LEFT_SHIFT;
        ctx.input().keyDown(key, mod);
        ctx.input().mouseDown(b.centerX(), b.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(b.centerX(), b.centerY(), Keys.MOUSE_LEFT);
        ctx.input().keyUp(key, 0);
    }

    private static void clickMenuLeaf(TestContext ctx, String translationKey) {
        var label = Component.translatable(translationKey).getString();
        var matches = new ArrayList<UIElement>();
        for (var ref : ctx.query().type(Menu.class).list()) {
            collectLeaf(ref.element(), label, matches);
        }
        ctx.require("found menu leaf '" + label + "' (found " + matches.size() + ")", matches.size() == 1);
        var b = ElementBounds.of(matches.getFirst());
        ctx.input().moveTo(b.centerX(), b.centerY());
        ctx.input().mouseDown(b.centerX(), b.centerY(), Keys.MOUSE_LEFT);
        ctx.input().mouseUp(b.centerX(), b.centerY(), Keys.MOUSE_LEFT);
    }

    private static void collectLeaf(UIElement el, String label, List<UIElement> out) {
        if (el.hasClass("__menu_leaf-node__") && label.equals(firstText(el))) {
            out.add(el);
            return;
        }
        for (var child : el.getChildren()) {
            collectLeaf(child, label, out);
        }
    }

    private static String firstText(UIElement el) {
        if (el instanceof TextElement text) return text.getText().getString();
        for (var child : el.getChildren()) {
            var s = firstText(child);
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    @Nullable
    private static UIElement cellNamed(UIElement container, String name) {
        for (var cell : container.getChildren()) {
            if (!cell.getChildren().isEmpty() && cell.getChildren().getLast() instanceof TextElement label) {
                if (name.equals(label.getText().getString())) return cell;
            }
        }
        return null;
    }

    private static Set<String> selectedResourceNames(TestContext ctx) {
        var pc = colorProviderContainer(ctx);
        return pc.getSelectedResources().stream()
                .map(p -> pc.getNameSupplier().apply(p))
                .collect(Collectors.toSet());
    }

    private static Set<String> selectedAssetNames(TestContext ctx) {
        return browser(ctx).getSelectedFiles().stream()
                .map(File::getName)
                .collect(Collectors.toSet());
    }

    private static void writeFile(TestContext ctx, File file, String content) {
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            ctx.require("could write " + file + ": " + e, false);
        }
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    @SuppressWarnings("unchecked")
    private static ResourceContainer<Integer> colorContainer(TestContext ctx) {
        var tab = editor(ctx).resourceView.getResourceTabs().get(ColorsResource.INSTANCE);
        return (ResourceContainer<Integer>) tab.getContent();
    }

    private static ResourceProviderContainer<Integer> colorProviderContainer(TestContext ctx) {
        return colorContainer(ctx).getSelectedProviderContainer();
    }

    private static AssetBrowser browser(TestContext ctx) {
        return editor(ctx).resourceView.getAssetBrowser();
    }
}
