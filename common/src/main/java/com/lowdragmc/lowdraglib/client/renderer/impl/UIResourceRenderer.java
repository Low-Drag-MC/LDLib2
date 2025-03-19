package com.lowdragmc.lowdraglib.client.renderer.impl;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
    @Environment(EnvType.CLIENT)
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        getRenderer().renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getRenderer().renderModel(level, pos, state, side, rand);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        getRenderer().onPrepareTextureAtlas(atlasName, register);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onAdditionalModel(Consumer<ResourceLocation> registry) {
        getRenderer().onAdditionalModel(registry);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void registerEvent() {
        getRenderer().registerEvent();
    }

    @Override
    public boolean isRaw() {
        return getRenderer().isRaw();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean hasTESR(BlockEntity blockEntity) {
        return getRenderer().hasTESR(blockEntity);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean isGlobalRenderer(BlockEntity blockEntity) {
        return getRenderer().isGlobalRenderer(blockEntity);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public int getViewDistance() {
        return getRenderer().getViewDistance();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return getRenderer().shouldRender(blockEntity, cameraPos);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        getRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
    }

    @NotNull
    @Override
    @Environment(EnvType.CLIENT)
    public TextureAtlasSprite getParticleTexture() {
        return getRenderer().getParticleTexture();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean useAO() {
        return getRenderer().useAO();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean useAO(BlockState state) {
        return getRenderer().useAO(state);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        return getRenderer().useBlockLight(stack);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean reBakeCustomQuads() {
        return getRenderer().reBakeCustomQuads();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public float reBakeCustomQuadsOffset() {
        return getRenderer().reBakeCustomQuadsOffset();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean isGui3d() {
        return getRenderer().isGui3d();
    }
}
