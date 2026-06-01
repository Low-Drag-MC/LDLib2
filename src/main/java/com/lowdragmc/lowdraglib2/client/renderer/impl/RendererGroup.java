package com.lowdragmc.lowdraglib2.client.renderer.impl;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@LDLRegisterClient(name = "renderer_group", registry = "ldlib2:renderer")
public class RendererGroup implements IRenderer {
    @Configurable(collapse = false)
    @Getter
    private IRenderer[] renderers;

    public RendererGroup() {
        this(new IRenderer[0]);
    }

    public RendererGroup(IRenderer... renderers) {
        this.renderers = renderers;
    }

    public RendererGroup setRenderers(IRenderer... renderers) {
        this.renderers = renderers;
        return this;
    }

    @Override
    public RendererGroup copy() {
        return new RendererGroup(Arrays.stream(renderers).map(IRenderer::copy).toArray(IRenderer[]::new));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        for (IRenderer renderer : renderers) {
            renderer.onPrepareTextureAtlas(atlasName, register);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        for (IRenderer renderer : renderers) {
            renderer.onAdditionalModel(registry);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clearCache() {
        for (IRenderer renderer : renderers) {
            renderer.clearCache();
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        for (IRenderer renderer : renderers) {
            renderer.renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        var result = new ArrayList<BakedQuad>();
        for (IRenderer renderer : renderers) {
            result.addAll(renderer.renderModel(level, pos, state, side, rand, data, renderType));
        }
        return result;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        return ChunkRenderTypeSet.union(Arrays.stream(renderers).map(renderer -> renderer.getRenderTypes(level, pos, state, rand, modelData)).toList());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        for (IRenderer renderer : renderers) {
            if (renderer.hasBlockEntityRenderer(blockEntity))
                return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        for (IRenderer renderer : renderers) {
            if (renderer.shouldRenderOffScreen(blockEntity))
                return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        for (IRenderer renderer : renderers) {
            if (renderer.shouldRender(blockEntity, cameraPos))
                return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        for (IRenderer renderer : renderers) {
            renderer.render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        for (IRenderer renderer : renderers) {
            return renderer.getParticleTexture(level, pos, modelData);
        }
        return IRenderer.EMPTY.getParticleTexture(level, pos, modelData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        for (IRenderer renderer : renderers) {
            return renderer.useAO();
        }
        return IRenderer.EMPTY.useAO();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO(BlockState state, ModelData modelData, RenderType renderType) {
        for (IRenderer renderer : renderers) {
            return renderer.useAO(state, modelData, renderType);
        }
        return IRenderer.EMPTY.useAO(state, modelData, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        for (IRenderer renderer : renderers) {
            return renderer.useBlockLight(stack);
        }
        return IRenderer.EMPTY.useBlockLight(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean reBakeCustomQuads() {
        for (IRenderer renderer : renderers) {
            if (renderer.reBakeCustomQuads())
                return true;
        }
        return IRenderer.EMPTY.reBakeCustomQuads();
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGui3d() {
        for (IRenderer renderer : renderers) {
            return renderer.isGui3d();
        }
        return IRenderer.EMPTY.isGui3d();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        var result = new AABB(blockEntity.getBlockPos());
        for (IRenderer renderer : renderers) {
            result = result.minmax(renderer.getRenderBoundingBox(blockEntity));
        }
        return result;
    }
}
