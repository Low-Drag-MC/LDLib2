package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.browser.FileOps;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphResourceProviderContainer;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.TestProject;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraphResource;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * A resource in a folder no provider covers is one object while the asset browser has the folder open, so what it
 * edits and saves is what a reference to the file draws with; and a graph there can be saved from another editor,
 * no provider needed.
 *
 * <p>Reported from Photon: the browser's own provider read a material, the library read it again for the
 * inspector, and each save wrote the untouched copy back over the edit. With one copy that cannot happen, and a
 * save no longer makes every reference read the file again.
 */
@LDLRegisterClient(name = "asset_browser_unowned_edit", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class AssetBrowserUnownedEditScenario implements UIScenario {
    private static final int BEFORE = 0xFFFF0000;
    private static final int AFTER = 0xFF0000FF;
    private static final String MARKER = "uitest_saved";

    private static File fixtureDir() {
        return new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID + "/uitest_unowned");
    }

    private static File textureFile() {
        return new File(fixtureDir(), "textures/edited" + TexturesResource.INSTANCE.getFileExtension());
    }

    private static File graphFile() {
        return new File(fixtureDir(), "graphs/sub" + TestGraphResource.INSTANCE.getFileExtension());
    }

    /** How a project saves a reference: the game-relative string, not the File the browser lists. */
    private static IResourcePath referenceTo(File file) {
        return new FilePath(FilePath.toGameRelative(file.getPath()));
    }

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("editor", "assets", "resources");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("write a texture and a graph into folders no provider covers", ctx -> {
                    FileOps.deleteRecursively(fixtureDir());
                    ctx.require("the texture was written", textures().writeUnowned(referenceTo(textureFile()),
                            new ColorRectTexture(BEFORE)));
                    ctx.require("the graph was written", TestGraphResource.INSTANCE.getResourceInstance().writeUnowned(
                            referenceTo(graphFile()),
                            TestGraphResource.INSTANCE.serializeGraphResource(TestGraphResource.INSTANCE.createGraph())));
                    ctx.require("neither folder is a provider", textures().listAllResources().stream()
                            .noneMatch(entry -> entry.getKey().equals(referenceTo(textureFile()))));
                    // what a project referencing it does before the browser goes anywhere near the folder
                    ctx.require("a reference resolves the texture", textures().getResource(referenceTo(textureFile())) != null);
                })
                .openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .step("load a project, so there are resource types", ctx -> editor(ctx).loadProject(new TestProject(), null))
                .waitUntil("the resource tabs are up", ctx -> !editor(ctx).resourceView.getResourceTabs().isEmpty())

                .step("browse the texture's folder", ctx ->
                        editor(ctx).resourceView.getAssetBrowser().openDirectory(textureFile().getParentFile()))
                .frames(5)
                .step("double click the texture", ctx -> editor(ctx).resourceView.getAssetBrowser().activate(textureFile()))
                .waitUntil("the inspector shows it", ctx -> inspected(ctx) instanceof ColorRectTexture)
                .check("a reference resolves the very texture being edited",
                        ctx -> textures().getResource(referenceTo(textureFile())) == inspected(ctx))

                .step("change its colour", ctx -> {
                    var texture = (ColorRectTexture) inspected(ctx);
                    texture.setColor(AFTER);
                    ctx.put("edited", texture);
                    var row = editor(ctx).inspectorView.inspector.selfAndAllChildren()
                            .filter(Configurator.class::isInstance).map(Configurator.class::cast).findFirst().orElse(null);
                    ctx.require("the inspector has a row to report the change", row != null);
                    row.notifyChanges();
                })
                // the browser flushes its dirty resources on its tick
                .frames(10)
                .step("the edit stuck, in one copy", ctx -> {
                    var saved = colorOf(readTexture());
                    ctx.check("the file holds the edit", saved != null && saved == AFTER,
                            Integer.toHexString(AFTER), saved == null ? null : Integer.toHexString(saved));
                    ctx.check("saving did not make the reference read the file again",
                            textures().getResource(referenceTo(textureFile())) == ctx.get("edited"));
                })

                .step("save the graph from an editor of another library", ctx -> {
                    var instance = TestGraphResource.INSTANCE.getResourceInstance();
                    var path = referenceTo(graphFile());
                    var tag = instance.getResource(path);
                    ctx.require("the graph resolves", tag != null);
                    var edited = tag.copy();
                    edited.putBoolean(MARKER, true);
                    var host = new BuiltinResourceProvider<CompoundTag>("uitest_host", instance);
                    var container = TestGraphResource.INSTANCE.createResourceProviderContainer(host);
                    container.setEditor(editor(ctx));
                    GraphResourceProviderContainer.saveRouted(container, host, path, edited);
                    var saved = readGraph();
                    ctx.check("the graph file holds the save", saved != null && saved.getBoolean(MARKER));
                })

                .teardown("close the editor", ctx -> ctx.mc().setScreen(null))
                .teardown("delete the fixture", ctx -> FileOps.deleteRecursively(fixtureDir()));
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceInstance<IGuiTexture> textures() {
        return TexturesResource.INSTANCE.getResourceInstance();
    }

    @Nullable
    private static Object inspected(TestContext ctx) {
        return editor(ctx).inspectorView.inspector.getInspectedConfigurable();
    }

    @Nullable
    private static Integer colorOf(@Nullable IGuiTexture texture) {
        return texture instanceof ColorRectTexture rect ? rect.color : null;
    }

    @Nullable
    private static IGuiTexture readTexture() {
        var nbt = read(textureFile());
        return nbt == null ? null : TexturesResource.INSTANCE.deserializeResource(nbt.get("data"), Platform.getFrozenRegistry());
    }

    @Nullable
    private static CompoundTag readGraph() {
        var nbt = read(graphFile());
        return nbt == null ? null : TestGraphResource.INSTANCE.deserializeResource(nbt.get("data"), Platform.getFrozenRegistry());
    }

    @Nullable
    private static CompoundTag read(File file) {
        try {
            return NbtIo.read(file.toPath());
        } catch (Exception e) {
            return null;
        }
    }
}
