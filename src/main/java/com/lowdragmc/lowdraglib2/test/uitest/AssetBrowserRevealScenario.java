package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.browser.AssetBrowser;
import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
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

import java.io.File;

/**
 * Revealing a file the browser is given in another spelling than its root's.
 *
 * <p>The tree, the breadcrumb and the providers' folders all compare files by path. A dev run's game directory is
 * spelled with a {@code .} in it, so a caller that normalized the path it reveals got a breadcrumb climbing past the
 * root up to the drive, a tree that did not follow, and every resource of a provider's folder drawn as unreadable.
 * The root here is spelled with a {@code .} the same way, and the folder is revealed normalized.
 */
@LDLRegisterClient(name = "asset_browser_reveal", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class AssetBrowserRevealScenario implements UIScenario {

    private static final String ROOT = "browser_root";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "assets");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("root the browser at a folder spelled with a '.' in it", ctx -> {
                    var root = new File(new File(ctx.mc().gameDirectory, "."), "ldlib2-uitest/reveal");
                    ctx.put(ROOT, root);
                    ctx.require("could create the folders", new File(root, "a/b/c").isDirectory()
                            || new File(root, "a/b/c").mkdirs());
                    var view = editor(ctx).resourceView;
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    browser(ctx).setRoot(root);
                })
                .step("reveal a folder in it, given normalized", ctx -> {
                    var revealed = new File(root(ctx), "a/b").toPath().toAbsolutePath().normalize().toFile();
                    ctx.require("the spelling differs from the root's", !revealed.getPath().startsWith(root(ctx).getPath()));
                    browser(ctx).revealFile(revealed);
                })
                .waitUntil("the folder it is in is on show, spelled from the root",
                        ctx -> new File(root(ctx), "a").equals(browser(ctx).getCurrentDirectory()))
                .check("the breadcrumb's path stops at the root", ctx -> {
                    for (var file = browser(ctx).getCurrentDirectory(); file != null; file = file.getParentFile()) {
                        if (file.equals(root(ctx))) return true;
                    }
                    return false;
                })
                .check("the tree follows it", ctx -> browser(ctx).tree.getSelected().stream()
                        .anyMatch(node -> new File(root(ctx), "a").equals(node.getKey())))
                .waitUntil("and the revealed folder is selected in the grid",
                        ctx -> new File(root(ctx), "a/b").equals(browser(ctx).getSelectedFile()))
                .teardown("remove the folders", ctx -> {
                    var root = ctx.<File>get(ROOT);
                    if (root != null) {
                        FileOps.deleteRecursively(root);
                    }
                })
                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static AssetBrowser browser(TestContext ctx) {
        return editor(ctx).resourceView.getAssetBrowser();
    }

    private static File root(TestContext ctx) {
        return ctx.get(ROOT);
    }
}
