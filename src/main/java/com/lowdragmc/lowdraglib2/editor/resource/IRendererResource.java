package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib2.client.scene.FBOWorldSceneRenderer;
import com.lowdragmc.lowdraglib2.client.scene.ImmediateWorldSceneRenderer;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@KJSBindings
public class IRendererResource extends Resource<IRenderer> {
    public static final IRendererResource INSTANCE = new IRendererResource();
    private final Set<ResourceProviderContainer<IRenderer>> openedContainers = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

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

    @Override
    public ResourceProviderContainer<IRenderer> createResourceProviderContainer(IResourceProvider<IRenderer> provider) {
        var container = super.createResourceProviderContainer(provider);
        openedContainers.add(container);
        container.addEventListener(UIEvents.REMOVED, e -> openedContainers.remove(container));
        container.setUiSupplier(path -> ClientWrapper.uiProvider(provider, path));
        container.setOnEdit((c, path) -> {
            var renderer = provider.getResource(path);
            if (renderer == null) return;
            c.getEditor().inspectorView.inspect(renderer, configurator -> c.markResourceDirty(path));
        });
        container.setOnDragProvider(UIResourceRenderer::new);

        container.setOnMenu((c, m) -> {
            m.leaf("ldlib.gui.editor.menu.reload_resource", this::reloadResourcesAndRefreshOpenedContainers);
            if (provider.supportAdd()) {
                m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                    for (var holder : LDLib2Registries.RENDERERS) {
                        var name = holder.annotation().name();
                        if (name.equals("empty") || name.equals("ui_resource_renderer")) continue;
                        menu.leaf(name, () -> {
                            var renderer = holder.value().get();
                            c.addNewResource(renderer);
                        });
                    }
                });
            }
        });
        return container;
    }

    public void reloadResourcesAndRefreshOpenedContainers() {
        var minecraft = Minecraft.getInstance();
        minecraft.reloadResourcePacks().thenRun(() ->
                minecraft.execute(this::refreshOpenedContainers));
    }

    public void refreshOpenedContainers() {
        for (var renderer : getLoadedResourceRenderers()) {
            renderer.clearCache();
        }
        getResourceInstance().clearCache();
        synchronized (openedContainers) {
            for (var container : openedContainers) {
                container.reloadResourceContainer();
            }
        }
    }

    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        for (var renderer : getLoadedResourceRenderers()) {
            renderer.onPrepareTextureAtlas(atlasName, register);
        }
    }

    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        for (var renderer : getLoadedResourceRenderers()) {
            renderer.onAdditionalModel(registry);
        }
    }

    private List<IRenderer> getLoadedResourceRenderers() {
        var instance = getResourceInstance();
        refreshProviders(instance.getBuiltinProviders());
        refreshProviders(instance.getCustomProviders());
        var renderers = new ArrayList<IRenderer>();
        for (var entry : instance.listAllResources()) {
            if (entry.getValue() != null) {
                renderers.add(entry.getValue());
            }
        }
        return renderers;
    }

    private static void refreshProviders(Map<ResourceProviderType, List<IResourceProvider<IRenderer>>> providersByType) {
        for (var providers : providersByType.values()) {
            for (var provider : providers) {
                provider.checkAndUpdateResourceProvider();
            }
        }
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
            var renderer = new ImmediateWorldSceneRenderer(level);
            renderer.useOrtho(true);
            renderer.setCameraOrtho(1, 1, 1);
            renderer.addRenderedBlocks(List.of(BlockPos.ZERO), null);
            renderer.setCameraLookAt(new Vector3f(0.5f), 0.1f, Math.toRadians(-135), Math.toRadians(25));
            return new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.heightPercent(100);
            }).style(style -> style.backgroundTexture((graphics, mouseX, mouseY, x, y, width, height, partialTicks) -> {
                renderer.render(graphics.pose(), x, y, width, height, 0, 0);
            }));
        }
    }
}
