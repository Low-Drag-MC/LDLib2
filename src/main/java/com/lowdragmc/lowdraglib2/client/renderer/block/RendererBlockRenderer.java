package com.lowdragmc.lowdraglib2.client.renderer.block;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

class RendererBlockRenderer implements IRenderer {

    public Optional<RendererBlockEntity> getMachine(@Nullable BlockEntity blockEntity) {
        return Optional.ofNullable(blockEntity).filter(RendererBlockEntity.class::isInstance).map(RendererBlockEntity.class::cast);
    }

    public Optional<RendererBlockEntity> getMachine(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null || pos == null)
            return Optional.empty();
        return getMachine(level.getBlockEntity(pos));
    }

    // todo renderer
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
//        throw new UnsupportedOperationException("Not implemented yet");
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
//        return getMachine(level, pos)
//                .map(machine -> machine.getRenderer().renderModel(level, pos, state, side, rand, data, renderType))
//                .orElseGet(Collections::emptyList);
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
//        return getMachine(level, pos)
//                .map(machine -> machine.getRenderer().getRenderTypes(level, pos, state, rand, modelData))
//                .orElseGet(() -> IRenderer.super.getRenderTypes(level, pos, state, rand, modelData));
//    }
//
//    @Override
//    @NotNull
//    @OnlyIn(Dist.CLIENT)
//    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
//        return getMachine(level, pos)
//                .map(machine -> machine.getRenderer().getParticleTexture(level, pos, modelData))
//                .orElseGet(() -> IRenderer.super.getParticleTexture(level, pos, modelData));
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
//        return getMachine(blockEntity).map(machine -> machine.getRenderer().hasBlockEntityRenderer(blockEntity)).orElse(false);
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
//        return getMachine(blockEntity).map(machine -> machine.getRenderer().shouldRenderOffScreen(blockEntity)).orElse(false);
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
//        return getMachine(blockEntity).map(machine -> machine.getRenderer().shouldRender(blockEntity, cameraPos)).orElseGet(() -> IRenderer.super.shouldRender(blockEntity, cameraPos));
//    }
//
//    @Override
//    @OnlyIn(Dist.CLIENT)
//    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
//        getMachine(blockEntity).ifPresent(machine -> machine.getRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay));
//    }
}
