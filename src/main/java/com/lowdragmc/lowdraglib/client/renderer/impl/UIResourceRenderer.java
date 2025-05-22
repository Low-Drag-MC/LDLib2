package com.lowdragmc.lowdraglib.client.renderer.impl;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.editor_outdated.data.resource.Resource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.Setter;
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

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class UIResourceRenderer implements IRenderer {
    @Getter
    private static Resource<IRenderer> projectResource;
    @Getter
    private static boolean isProject;

    public static void setCurrentResource(Resource<IRenderer> resource, boolean isProject) {
        projectResource = resource;
        UIResourceRenderer.isProject = isProject;
    }

    public static void clearCurrentResource() {
        projectResource = null;
        UIResourceRenderer.isProject = false;
    }

    @Setter
    private Resource<IRenderer> resource;

    public final Either<String, File> key;

    public UIResourceRenderer(Either<String, File> key) {
        this.key = key;
    }

    public UIResourceRenderer(Resource<IRenderer> resource, Either<String, File> key) {
        this.resource = resource;
        this.key = key;
    }

    public IRenderer getRenderer() {
        return resource == null ? IRenderer.EMPTY : resource.getResourceOrDefault(key, IRenderer.EMPTY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        getRenderer().renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand,  ModelData data, @Nullable RenderType renderType) {
        return getRenderer().renderModel(level, pos, state, side, rand, data, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        getRenderer().onPrepareTextureAtlas(atlasName, register);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        getRenderer().onAdditionalModel(registry);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void registerEvent() {
        getRenderer().registerEvent();
    }


    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity blockEntity) {
        return getRenderer().hasTESR(blockEntity);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return getRenderer().isGlobalRenderer(blockEntity);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getViewDistance() {
        return getRenderer().getViewDistance();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return getRenderer().shouldRender(blockEntity, cameraPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        getRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
    }

    @NotNull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return getRenderer().getParticleTexture(level, pos, modelData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        return getRenderer().useAO();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO(BlockState state, ModelData data, RenderType renderType) {
        return getRenderer().useAO(state, data, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        return getRenderer().useBlockLight(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean reBakeCustomQuads() {
        return getRenderer().reBakeCustomQuads();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public float reBakeCustomQuadsOffset() {
        return getRenderer().reBakeCustomQuadsOffset();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGui3d() {
        return getRenderer().isGui3d();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        return getRenderer().getRenderTypes(level, pos, state, rand, modelData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return getRenderer().getRenderBoundingBox(blockEntity);
    }
}
