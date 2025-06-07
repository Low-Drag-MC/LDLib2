package com.lowdragmc.lowdraglib2.client.scene;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scene Render State hooks.
 * This is where you decide whether this group of pos should be rendered. What other requirements do you have for rendering.
 */
public interface ISceneBlockRenderHook {
    /**
     * Called before rendering the given render layer
     */
    default void apply(RenderType layer) {

    }

    /**
     * Called before rendering the given block entity
     */
    default void applyBESR(Level world, BlockPos pos, BlockEntity blockEntity, PoseStack poseStack, float partialTicks) {

    }

    /**
     * Called before pushing the vertex data into the buffer during block rendering.
     */
    default void applyVertexConsumerWrapper(Level world, BlockPos pos, BlockState state, WorldSceneRenderer.VertexConsumerWrapper wrapperBuffer, RenderType layer, float partialTicks) {

    }
}
