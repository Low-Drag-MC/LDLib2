package com.lowdragmc.lowdraglib.editor.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib.client.scene.FBOWorldSceneRenderer;
import com.lowdragmc.lowdraglib.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib.utils.virtuallevel.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class IRendererResource extends Resource<IRenderer> {

    public IRendererResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        builtinResource.addResource("empty", IRenderer.EMPTY);
        addResourceProvider(builtinResource);

    }

    @Override
    public void buildDefault() {
        addResourceProvider(createNewFileResourceProvider(new File(LDLib.getAssetsDir(), "ldlib/resources")).setName("global"));
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
    public ResourceProviderContainer<IRenderer> createResourceProviderContainer(ResourceProvider<IRenderer> provider) {
        var container = super.createResourceProviderContainer(provider);
        container.setUiSupplier(path -> {
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
                layout.setWidthPercent(100);
                layout.setHeightPercent(100);
            }).style(style -> style.backgroundTexture(fboRenderer.drawAsTexture()))
                    // release resources here
                    .addEventListener(UIEvents.REMOVED, e -> fboRenderer.releaseFBO());
        });
        container.setOnEdit((c, path) -> {
            var renderer = provider.getResource(path);
            if (renderer == null) return;
            c.getEditor().inspectorView.inspect(renderer, configurator -> c.markResourceDirty(path), null);
        });

        if (provider.supportAdd()) {
            container.setOnMenu((c, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                for (var holder : LDLibRegistries.RENDERERS) {
                    var name = holder.annotation().name();
                    menu.leaf(name, () -> {
                        var renderer = holder.value().get();
                        renderer.initRenderer();
                        c.addNewResource(renderer);
                    });
                }
            }));
        }
        return container;
    }

}
