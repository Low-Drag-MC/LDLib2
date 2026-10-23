package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import com.lowdragmc.lowdraglib2.editor.resource.UIResource;
import com.lowdragmc.lowdraglib2.editor.resource.UIResourceProviderContainer;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.nodegraphtookit.editor.GraphEditorView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.TestEditor;
import com.lowdragmc.lowdraglib2.test.noddegraphtoolkit.TestGraphResource;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A resource opened for edit comes to the front of its container, over the Scene when there is no saved layout;
 * opening it again in this editor brings its tab forward, whichever container it is opened from.
 */
@LDLRegisterClient(name = "resource_editor_reopen", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ResourceEditorReopenScenario implements UIScenario {
    private static final String PATH = "path";
    private static final String PANEL = "panel";
    private static final String BROWSER = "browser";
    private static final String OPENED = "opened";

    @Override
    public void configure(ScenarioOptions options) {
        options.defaultSettleMs(30).tags("editor", "resources");
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.openModularUI("editor", ctx -> new ModularUI(UI.of(new TestEditor()), ctx.player()))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .waitUntil("the editor has laid out", ctx -> editor(ctx).centerWindow.getSizeWidth() > 0)
                .group("a UI template", g -> openTwice(g, UIEditorView.class, ctx -> {
                    var provider = new BuiltinResourceProvider<UITemplate>("uitest", new ResourceInstance<>(UIResource.INSTANCE));
                    var path = provider.createSubPath("reopen");
                    provider.addResource(path, UITemplate.of(new UIElement()));
                    reachFromTwoContainers(ctx, path, () -> new UIResourceProviderContainer(provider));
                }))
                .group("a graph", g -> openTwice(g, GraphEditorView.class, ctx -> {
                    var graphs = TestGraphResource.INSTANCE;
                    var provider = new BuiltinResourceProvider<CompoundTag>("uitest", graphs.getResourceInstance());
                    var path = provider.createSubPath("reopen");
                    provider.addResource(path, graphs.serializeGraphResource(graphs.createGraph()));
                    reachFromTwoContainers(ctx, path, () -> graphs.createResourceProviderContainer(provider));
                }))
                .closeScreen();
    }

    private static void openTwice(ScenarioBuilder s, Class<? extends View> viewType, Consumer<TestContext> setup) {
        s.step("reach one resource from two containers, as the resource panel and the asset browser do", setup)
                .step("open it from the panel", ctx -> container(ctx, PANEL).editResource(ctx.get(PATH)))
                .waitUntil("its editor is open", ctx -> openedViews(ctx, viewType).size() == 1)
                .step("note it", ctx -> ctx.put(OPENED, openedViews(ctx, viewType).getFirst()))
                .check("it opens in front of the tab already there", ctx -> isSelected(ctx.get(OPENED)))
                .step("switch to another tab", ctx -> {
                    var view = ctx.<View>get(OPENED);
                    var container = view.getViewContainer();
                    ctx.require("the editor is docked", container != null);
                    var other = container.getAllViews().stream().filter(v -> v != view).findFirst().orElse(null);
                    ctx.require("there is another tab to switch to", other != null);
                    container.selectView(other);
                })
                .check("its tab is hidden", ctx -> !isSelected(ctx.get(OPENED)))
                .step("open it again from the browser", ctx -> container(ctx, BROWSER).editResource(ctx.get(PATH)))
                .check("no second editor is opened", ctx -> openedViews(ctx, viewType).size() == 1)
                .check("the open one is brought forward", ctx -> isSelected(ctx.get(OPENED)))
                .step("close it", ctx -> ctx.<View>get(OPENED).removeSelf());
    }

    private static void reachFromTwoContainers(TestContext ctx, IResourcePath path, Supplier<ResourceProviderContainer<?>> factory) {
        ctx.put(PATH, path);
        for (var key : List.of(PANEL, BROWSER)) {
            var container = factory.get();
            container.setEditor(editor(ctx));
            ctx.put(key, container);
        }
    }

    private static Editor editor(TestContext ctx) {
        return ctx.query().type(Editor.class).one().as(Editor.class);
    }

    private static ResourceProviderContainer<?> container(TestContext ctx, String key) {
        return ctx.get(key);
    }

    private static List<View> openedViews(TestContext ctx, Class<? extends View> type) {
        var found = new ArrayList<View>();
        collect(editor(ctx), type, found);
        return found;
    }

    private static void collect(UIElement element, Class<? extends View> type, List<View> found) {
        if (type.isInstance(element)) {
            found.add((View) element);
            return;
        }
        for (var child : element.getChildren()) {
            collect(child, type, found);
        }
    }

    private static boolean isSelected(View view) {
        var container = view.getViewContainer();
        return container != null && container.isViewSelected(view);
    }
}
