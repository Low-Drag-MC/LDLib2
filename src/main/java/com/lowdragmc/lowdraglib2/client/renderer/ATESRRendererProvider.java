package com.lowdragmc.lowdraglib2.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Author: KilaBash
 * Date: 2022/04/21
 * Description: 
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ATESRRendererProvider<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public ATESRRendererProvider() {
    }

    public ATESRRendererProvider(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public int getViewDistance() {
        return BlockEntityRenderer.super.getViewDistance();
    }

    @Override
    public boolean shouldRender(T pBlockEntity, Vec3 pCameraPos) {
        IRenderer renderer = getRenderer(pBlockEntity);
        if (renderer != null) {
            return renderer.shouldRender(pBlockEntity, pCameraPos);
        }
        return BlockEntityRenderer.super.shouldRender(pBlockEntity, pCameraPos);
    }

    @Override
    public void render(T pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        IRenderer renderer = getRenderer(pBlockEntity);
        if (renderer != null) {
            renderer.render(pBlockEntity, pPartialTick, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
        }
    }

    @Nullable
    public IRenderer getRenderer(@Nonnull T blockEntity) {
        Level world = blockEntity.getLevel();
        if (world != null) {
            BlockState state = blockEntity.getBlockState();
            if (state.getBlock() instanceof IBlockRendererProvider blockRendererProvider) {
                return blockRendererProvider.getRenderer(state);
            }
        }
        return null;
    }

    public boolean hasRenderer(T blockEntity) {
        IRenderer renderer = getRenderer(blockEntity);
        return renderer != null && renderer.hasTESR(blockEntity);
    }

    @Override
    public boolean shouldRenderOffScreen(@Nonnull T blockEntity) {
        IRenderer renderer = getRenderer(blockEntity);
        if (renderer != null) {
            return renderer.isGlobalRenderer(blockEntity);
        }
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        IRenderer renderer = getRenderer(blockEntity);
        if (renderer != null) {
            return renderer.getRenderBoundingBox(blockEntity);
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(blockEntity);
    }
}

