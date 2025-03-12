package com.lowdragmc.lowdraglib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface IRenderer {
    Set<IRenderer> EVENT_REGISTERS = new HashSet<>();
    IRenderer EMPTY = new IRenderer() {};

    /**
     * Render itemstack.
     */
    @OnlyIn(Dist.CLIENT)
    default void renderItem(ItemStack stack,
                    ItemDisplayContext transformType,
                    boolean leftHand, PoseStack poseStack,
                    MultiBufferSource buffer, int combinedLight,
                    int combinedOverlay, BakedModel model) {

    }

    /**
     * Render static block model.
     */
    @OnlyIn(Dist.CLIENT)
    default List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        return Collections.emptyList();
    }

    /**
     * Register TextureSprite here.
     */
    @OnlyIn(Dist.CLIENT)
    default void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {

    }

    /**
     * Register additional model here.
     */
    @OnlyIn(Dist.CLIENT)
    default void onAdditionalModel(Consumer<ModelResourceLocation> registry) {

    }

    /**
     * If the renderer requires event registration either {@link #onPrepareTextureAtlas} or {@link #onAdditionalModel}, call this method in the constructor.
     */
    @OnlyIn(Dist.CLIENT)
    default void registerEvent() {
        synchronized (EVENT_REGISTERS) {
            EVENT_REGISTERS.add(this);
        }
    }

    /**
     * If the renderer is ready to be rendered.
     */
    default boolean isRaw() {
        return false;
    }

    /**
     * Does the block entity have the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean hasTESR(BlockEntity blockEntity) {
        return false;
    }

    /**
     * Is the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer} global.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean isGlobalRenderer(BlockEntity blockEntity) {
        return false;
    }

    /**
     * Get the view distance for TESR.
     */
    @OnlyIn(Dist.CLIENT)
    default int getViewDistance() {
        return 64;
    }

    /**
     * Should the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer} render.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos()).closerThan(cameraPos, this.getViewDistance());
    }

    /**
     * Render the TESR {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}.
     */
    @OnlyIn(Dist.CLIENT)
    default void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {

    }

    /**
     * Get the particle texture.
     */
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    default TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
    }

    /**
     * Whether to apply AO for the model.
     */
    @OnlyIn(Dist.CLIENT)
    default TriState useAO() {
        return TriState.FALSE;
    }

    /**
     * Whether to apply AO for the model.
     */
    @OnlyIn(Dist.CLIENT)
    default TriState useAO(BlockState state, ModelData modelData, RenderType renderType) {
        return useAO();
    }

    /**
     * Whether to apply block light during the itemstack rendering.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean useBlockLight(ItemStack stack) {
        return false;
    }

    /**
     * Should we rebake quads for mcmeta data?
     */
    @OnlyIn(Dist.CLIENT)
    default boolean reBakeCustomQuads() {
        return false;
    }

    /**
     * Offset for rebake's quads sides while {@link #reBakeCustomQuads()} return true.
     */
    @OnlyIn(Dist.CLIENT)
    default float reBakeCustomQuadsOffset() {
        return 0;
    }

    /**
     * Whether to apply gui 3d transform during itemstack rendering.
     */
    @OnlyIn(Dist.CLIENT)
    default boolean isGui3d() {
        return true;
    }
}
