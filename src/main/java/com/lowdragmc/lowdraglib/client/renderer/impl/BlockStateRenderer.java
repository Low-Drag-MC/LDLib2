package com.lowdragmc.lowdraglib.client.renderer.impl;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib.utils.virtuallevel.FacadeBlockWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

//@LDLRegisterClient(name = "block_model", group = "renderer")
public class BlockStateRenderer implements IRenderer {

    @Getter
    protected BlockInfo blockInfo;
    @OnlyIn(Dist.CLIENT)
    private BakedModel itemModel;

    protected BlockStateRenderer() {
        blockInfo = null;
    }

    public BlockStateRenderer(BlockState state) {
        this(BlockInfo.fromBlockState(state == null ? Blocks.BARRIER.defaultBlockState() : state));
    }

    public BlockStateRenderer(BlockInfo blockInfo) {
        this.blockInfo = blockInfo == null ? new BlockInfo(Blocks.BARRIER) : blockInfo;
        if (LDLib.isClient()) {
            registerEvent();
        }
    }

    public BlockState getState(@Nullable BlockState blockState) {
        BlockState state = getBlockInfo().getBlockState();
        if (blockState == null) return state;
        Direction facing = Direction.NORTH;
        if (blockState.hasProperty(BlockStateProperties.FACING)) {
            facing = blockState.getValue(BlockStateProperties.FACING);
        } else if (blockState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        try {
            switch (facing) {
                case EAST -> state = state.rotate(Rotation.CLOCKWISE_90);
                case WEST -> state = state.rotate(Rotation.COUNTERCLOCKWISE_90);
                case SOUTH -> state = state.rotate(Rotation.CLOCKWISE_180);
            }
        } catch (Exception ignore) {

        }
        return state;
    }

    @OnlyIn(Dist.CLIENT)
    protected BakedModel getItemModel(ItemStack renderItem) {
        if (itemModel == null) {
            itemModel = Minecraft.getInstance().getItemRenderer().getModel(renderItem, null, null, 0);
        }
        return itemModel;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    @Nonnull
    public TextureAtlasSprite getParticleTexture(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, ModelData modelData) {
        ItemStack renderItem = getBlockInfo().getItemStackForm();
        BakedModel model = getItemModel(renderItem);
        if (model == null) {
            return IRenderer.super.getParticleTexture(level, pos, modelData);
        }
        return model.getParticleIcon();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderItem(ItemStack stack, ItemDisplayContext transformType, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay, BakedModel model) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ItemStack renderItem = getBlockInfo().getItemStackForm();
        itemRenderer.render(renderItem, transformType, leftHand, poseStack, buffer, combinedLight, combinedOverlay, getItemModel(renderItem));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean useBlockLight(ItemStack stack) {
        ItemStack renderItem = getBlockInfo().getItemStackForm();
        var model = getItemModel(renderItem);
        if (model != null) {
            return model.usesBlockLight();
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public List<BakedQuad> renderModel(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        state = getState(state);
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
            BakedModel model = brd.getBlockModel(state);
            return model.getQuads(state, side, rand, data, renderType);
        }
        return Collections.emptyList();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ChunkRenderTypeSet getRenderTypes(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource rand, ModelData modelData) {
        state = getState(state);
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            BlockRenderDispatcher brd = Minecraft.getInstance().getBlockRenderer();
            BakedModel model = brd.getBlockModel(state);
            return model.getRenderTypes(state, rand, modelData);
        }
        return IRenderer.super.getRenderTypes(level, pos, state, rand, modelData);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public BlockEntity getBlockEntity(BlockAndTintGetter world, BlockPos pos) {
        BlockInfo blockInfo = getBlockInfo();
        BlockEntity tile = blockInfo.getBlockEntity(pos, Platform.getFrozenRegistry());
        if (tile != null && world instanceof Level) {
            try {
                var state = getState(world.getBlockState(pos));
                tile.setBlockState(state);
                tile.setLevel(new FacadeBlockWorld((Level) world, pos, state, tile));
            } catch (Throwable throwable) {
                blockInfo.setHasBlockEntity(false);
            }
        }
        return tile;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasTESR(BlockEntity tileEntity) {
        if (!getBlockInfo().getBlockState().getFluidState().isEmpty()) {
            return true;
        }
        tileEntity = getBlockEntity(tileEntity.getLevel(), tileEntity.getBlockPos());
        if (tileEntity == null) {
            return false;
        }
        return Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tileEntity) != null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGlobalRenderer(BlockEntity tileEntity) {
        tileEntity = getBlockEntity(tileEntity.getLevel(), tileEntity.getBlockPos());
        if (tileEntity == null) return false;
        BlockEntityRenderer<BlockEntity> tesr = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tileEntity);
        if (tesr != null) {
            return tesr.shouldRenderOffScreen(tileEntity);
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(BlockEntity tileEntity, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        BlockInfo block = getBlockInfo();
        FluidState fluidState = block.getBlockState().getFluidState();
        if (!fluidState.isEmpty()) {
            VertexConsumer builder = buffer.getBuffer(RenderType.translucent());
            Minecraft.getInstance().getBlockRenderer().renderLiquid(tileEntity.getBlockPos(), tileEntity.getLevel(), builder, block.getBlockState(), fluidState);
        }
        tileEntity = getBlockEntity(tileEntity.getLevel(), tileEntity.getBlockPos());
        if (tileEntity == null) return;
        BlockEntityRenderer<BlockEntity> tesr = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tileEntity);
        if (tesr != null) {
            try {
                tesr.render(tileEntity, partialTicks, stack, buffer, combinedLight, combinedOverlay);
            } catch (Exception e){
                getBlockInfo().setHasBlockEntity(false);
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onPrepareTextureAtlas(ResourceLocation atlasName, Consumer<ResourceLocation> register) {
        if (atlasName.equals(TextureAtlas.LOCATION_BLOCKS)) {
            itemModel = null;
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public TriState useAO() {
        var state = getBlockInfo().getBlockState();
        if (state.getRenderShape() != RenderShape.INVISIBLE) {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(state).useAmbientOcclusion() ? TriState.DEFAULT : TriState.FALSE;
        }
        return TriState.DEFAULT;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isGui3d() {
        var model = getItemModel(getBlockInfo().getItemStackForm());
        if (model == null) {
            return IRenderer.super.isGui3d();
        }
        return model.isGui3d();
    }
}
