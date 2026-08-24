package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
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
import net.minecraft.client.resources.language.I18n;

import java.io.File;
import java.util.List;

/**
 * The asset browser shows file and folder names exactly as they are on disk.
 *
 * <p>Names used to go through {@code setText(String)}, which treats its argument as a localisation
 * key. That is invisible until a name happens to be one — so this scenario makes two folders named
 * after keys the game really does define, and would read "Done" and "Options..." on the old code.
 *
 * <p>The third folder has a percent sign in it. That is harmless through a {@code Component}, which
 * only looks the key up, but the same names also reach {@link com.lowdragmc.lowdraglib2.gui.texture.TextTexture}
 * as the drag ghost, and that route ends in a {@link String#format} — so a name like this comes back
 * as "Format error: ...". The ghost only exists mid-drag and is not asserted here; what is asserted is
 * that no label ever picks up that route.
 */
@LDLRegisterClient(name = "asset_browser_literal_names", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class AssetBrowserLiteralNamesScenario implements UIScenario {

    /** Vanilla keys, so the test does not depend on any mod being present. */
    private static final List<String> NAMES = List.of("gui.done", "menu.options", "50%_off");

    private static final String ROOT = "browser_root";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(60).tags("editor", "assets", "i18n");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("point the browser at folders named after translation keys", ctx -> {
                    var root = new File(ctx.mc().gameDirectory, "ldlib2-uitest/literal-names");
                    ctx.put(ROOT, root);
                    for (var name : NAMES) {
                        var folder = new File(root, name);
                        ctx.require("could create " + name, folder.isDirectory() || folder.mkdirs());
                    }
                    var view = editor(ctx).resourceView;
                    var container = view.getViewContainer();
                    ctx.require("the resource view is docked", container != null);
                    container.selectView(view);
                    view.getAssetBrowser().setRoot(root);
                })
                .waitUntil("the folders are listed",
                        ctx -> ctx.count(".__asset-browser_entry-directory__") >= NAMES.size())
                .step("expand the tree, which starts collapsed on its root row", ctx -> {
                    var tree = editor(ctx).resourceView.getAssetBrowser().tree;
                    var rootNode = tree.getNodeUIs().keySet().stream().findFirst().orElse(null);
                    ctx.require("the tree has its root row", rootNode != null);
                    tree.expandNode(rootNode);
                })
                .waitUntil("the tree rows are built", ctx ->
                        ctx.count(".__asset-browser_tree-node-label__") > NAMES.size())

                // Guards the guard: if the game ever stopped translating these the test would pass
                // without proving anything.
                .check("the keys really do translate to something else",
                        ctx -> NAMES.stream().limit(2).allMatch(key -> !I18n.get(key).equals(key)))

                .check("the grid shows the names verbatim", ctx -> {
                    var labels = ctx.all(".__resource-cell_label__").stream()
                            .map(ref -> ref.text()).toList();
                    return labels.containsAll(NAMES);
                })
                .check("no name was replaced by its translation", ctx -> {
                    var labels = ctx.all(".__resource-cell_label__").stream()
                            .map(ref -> ref.text()).toList();
                    return labels.stream().noneMatch(label ->
                            label.equals(I18n.get("gui.done"))
                                    || label.equals(I18n.get("menu.options"))
                                    || label.startsWith("Format error"));
                })
                .check("the tree shows them verbatim too", ctx -> {
                    var labels = ctx.all(".__asset-browser_tree-node-label__").stream()
                            .map(ref -> ref.text()).toList();
                    // the tree lists folders only, and the root row is the temp directory itself
                    return labels.containsAll(NAMES);
                })
                .screenshot("01_literal_names")

                .teardown("remove the temporary folders", ctx -> {
                    var root = ctx.<File>get(ROOT);
                    if (root == null) return;
                    for (var name : NAMES) {
                        //noinspection ResultOfMethodCallIgnored
                        new File(root, name).delete();
                    }
                    //noinspection ResultOfMethodCallIgnored
                    root.delete();
                })
                .closeScreen();
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }
}
