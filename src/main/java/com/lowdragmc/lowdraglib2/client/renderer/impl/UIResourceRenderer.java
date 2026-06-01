package com.lowdragmc.lowdraglib2.client.renderer.impl;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinPath;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
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

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@NoArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@LDLRegisterClient(name = "ui_resource_renderer", registry = "ldlib2:renderer")
public class UIResourceRenderer implements IRenderer {
    @Persisted
    private IResourcePath resourcePath = new BuiltinPath("");
    @Getter(lazy = true)
    private final IRenderer internalRenderer = getRendererFromResource();

    public UIResourceRenderer(IResourcePath resourcePath) {
        this.resourcePath = resourcePath;
    }

    private IRenderer getRendererFromResource() {
        return Optional.ofNullable(IRendererResource.INSTANCE.getResourceInstance().getResource(resourcePath))
                .orElse(IRenderer.EMPTY);
    }

    @Override
    public UIResourceRenderer copy() {
        return new UIResourceRenderer(resourcePath);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        getInternalRenderer().renderItem(stack, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, model);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand,  ModelData data, @Nullable RenderType renderType) {
        return getInternalRenderer().renderModel(level, pos, state, side, rand, data, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        getInternalRenderer().onPrepareTextureAtlas(atlasName, register);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onAdditionalModel(Consumer<ModelResourceLocation> registry) {
        getInternalRenderer().onAdditionalModel(registry);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clearCache() {
        getInternalRenderer().clearCache();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasBlockEntityRenderer(BlockEntity blockEntity) {
        return getInternalRenderer().hasBlockEntityRenderer(blockEntity);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderOffScreen(BlockEntity blockEntity) {
        return getInternalRenderer().shouldRenderOffScreen(blockEntity);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public int getViewDistance() {
        return getInternalRenderer().getViewDistance();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        return getInternalRenderer().shouldRender(blockEntity, cameraPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity blockEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        getInternalRenderer().render(blockEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
    }

    @NotNull
    @Override
    @OnlyIn(Dist.CLIENT)
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        return getInternalRenderer().getParticleTexture(level, pos, modelData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        return getInternalRenderer().useAO();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO(BlockState state, ModelData data, RenderType renderType) {
        return getInternalRenderer().useAO(state, data, renderType);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        return getInternalRenderer().useBlockLight(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean reBakeCustomQuads() {
        return getInternalRenderer().reBakeCustomQuads();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public float reBakeCustomQuadsOffset() {
        return getInternalRenderer().reBakeCustomQuadsOffset();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGui3d() {
        return getInternalRenderer().isGui3d();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        return getInternalRenderer().getRenderTypes(level, pos, state, rand, modelData);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return getInternalRenderer().getRenderBoundingBox(blockEntity);
    }
}
