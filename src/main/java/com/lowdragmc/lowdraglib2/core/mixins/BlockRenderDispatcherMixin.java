package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.client.model.forge.LDLRendererModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote BlockRenderDispatcherMixin
 */
@Mixin(BlockRenderDispatcher.class)
public class BlockRenderDispatcherMixin {
    @Redirect(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V"),
            remap = false)
    private void ldlib2$injectTesselateBlock(ModelBlockRenderer instance,
                                            BlockAndTintGetter blockAndTintGetter,
                                            BakedModel bakedModel,
                                            BlockState blockState,
                                            BlockPos pos,
                                            PoseStack poseStack,
                                            VertexConsumer vertexConsumer,
                                            boolean b,
                                            RandomSource randomSource,
                                            long l, int i,
                                            ModelData modelData,
                                            RenderType renderType) {
        if (bakedModel instanceof LDLRendererModel.RendererBakedModel model) {
            var te = blockAndTintGetter.getBlockEntity(pos);
            instance.tesselateBlock(blockAndTintGetter, bakedModel, blockState, pos, poseStack, vertexConsumer, b, randomSource, l, i,
                    model.getModelData(blockAndTintGetter, pos, blockState, te == null ? ModelData.EMPTY : te.getModelData()), renderType);
        } else {
            instance.tesselateBlock(blockAndTintGetter, bakedModel, blockState, pos, poseStack, vertexConsumer, b, randomSource, l, i, modelData, renderType);
        }
    }
}
