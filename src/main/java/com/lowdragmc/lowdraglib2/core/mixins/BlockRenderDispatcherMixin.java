package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.model.forge.LDLRendererModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote BlockRenderDispatcherMixin
 */
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherMixin {
    @Shadow
    @Final
    private BlockModelShaper blockModelShaper;

    @Shadow
    @Final
    private ModelBlockRenderer modelRenderer;

    @Shadow
    @Final
    private RandomSource random;

    @Inject(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V", at = @At(value = "HEAD"), cancellable = true)
    private void ldlib2$renderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level,
                                             PoseStack poseStack, VertexConsumer consumer,
                                             ModelData modelData, CallbackInfo ci) {
        if (state.getRenderShape() == RenderShape.MODEL) {
            var bakedModel = this.blockModelShaper.getBlockModel(state);
            if (bakedModel instanceof LDLRendererModel.RendererBakedModel) {
                var seed = state.getSeed(pos);
                modelData = bakedModel.getModelData(level, pos, state, modelData);
                this.modelRenderer.tesselateBlock(
                        level, bakedModel, state, pos, poseStack, consumer, true, this.random, seed,
                        OverlayTexture.NO_OVERLAY,
                        modelData,
                        null);
                ci.cancel();
            }
        }
    }
}
