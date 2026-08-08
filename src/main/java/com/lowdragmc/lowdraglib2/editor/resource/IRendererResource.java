package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
// todo renderer
//import com.lowdragmc.lowdraglib2.client.renderer.impl.IModelRenderer;
//import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib2.client.scene.FBOWorldSceneRenderer;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

@KJSBindings
public class IRendererResource extends Resource<IRenderer> {
    public static final IRendererResource INSTANCE = new IRendererResource();

    @Override
    public void buildBuiltin(BuiltinResourceProvider<IRenderer> provider) {
        provider.addResource("empty", IRenderer.EMPTY);
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.MODEL;
    }

    @Override
    public String getName() {
        return "renderer";
    }

    @Nullable
    @Override
    public Tag serializeResource(IRenderer renderer, HolderLookup.Provider provider) {
        return renderer.serializeWrapper();
    }

    @Override
    public IRenderer deserializeResource(Tag tag, HolderLookup.Provider provider) {
        return IRenderer.deserializeWrapper(tag);
    }

    // todo renderer: importing a model json needs IModelRenderer, which is not ported to 26.1 yet.
    //  Until it is, only this type's own resource files are importable (the Resource default).
    //  1.21 reference implementation:
    //  @Override
    //  public boolean canImportFile(File file) {
    //      return super.canImportFile(file) || file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".json");
    //  }
    //
    //  @Override
    //  public void importFile(ResourceImportContext<IRenderer> context) {
    //      var file = context.getFile();
    //      if (super.canImportFile(file)) {
    //          super.importFile(context);
    //          return;
    //      }
    //      // a model json is only usable once it has been baked, which happens during a resource reload,
    //      // so unlike a texture the packs have to be reloaded before the renderer will draw anything
    //      ResourceFileImport.resolveOrImport(context.getOwner(), file, "models", location -> {
    //          // the model location drops the "models/" prefix and the extension, that is how the bakery
    //          // addresses it: models/block/foo.json -> block/foo
    //          var path = location.getPath();
    //          if (path.startsWith("models/")) path = path.substring("models/".length());
    //          if (path.endsWith(".json")) path = path.substring(0, path.length() - ".json".length());
    //          context.complete(new IModelRenderer(Identifier.fromNamespaceAndPath(location.getNamespace(), path)));
    //          reloadResourcesAndRefreshOpenedContainers();
    //      }, context::cancel);
    //  }

    @Override
    public ResourceProviderContainer<IRenderer> createResourceProviderContainer(IResourceProvider<IRenderer> provider) {
        var container = super.createResourceProviderContainer(provider);
        container.setUiSupplier(path -> ClientWrapper.uiProvider(provider, path));
        container.setOnEdit((c, path) -> {
            var renderer = provider.getResource(path);
            if (renderer == null) return;
            c.getEditor().inspectorView.inspect(renderer, configurator -> c.markResourceDirty(path));
        });
        // todo renderer
//        container.setOnDragProvider(UIResourceRenderer::new);

        // todo renderer: 1.21 offers a "reload_resource" entry here, which needs the openedContainers /
        //  resource-pack reload plumbing that IRenderer has not been ported to 26.1 with yet.
        if (provider.supportAdd()) {
            container.setOnCreateMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLib2ClientRegistries.RENDERERS) {
                    var name = holder.annotation().name();
                    if (name.equals("empty") || name.equals("ui_resource_renderer")) continue;
                    menu.leaf(name, () -> {
                        var renderer = holder.value().get();
                        c.addNewResource(renderer);
                    });
                }
            }));
        }
        return container;
    }

    private static class ClientWrapper {
        private static UIElement uiProvider(IResourceProvider<IRenderer> provider, IResourcePath path) {
            var level = new TrackedDummyWorld();
            level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
            Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
                if (blockEntity instanceof RendererBlockEntity holder) {
                    holder.setRenderer(provider.getResource(path));
                }
            });
            var fboRenderer = new FBOWorldSceneRenderer(level, 512, 512);
            fboRenderer.setFov(40);
            fboRenderer.addRenderedBlocks(List.of(BlockPos.ZERO), null);
            fboRenderer.setCameraLookAt(new Vector3f(0.5f), 2.5, Math.toRadians(-135), Math.toRadians(25));
            return new UIElement().layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                    })
                    // todo rendering
//                    .style(style -> style.backgroundTexture(fboRenderer.drawAsTexture()))
                    // release resources here
                    .addEventListener(UIEvents.REMOVED, e -> fboRenderer.releaseResource());
        }
    }
}
