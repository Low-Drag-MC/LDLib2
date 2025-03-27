package com.lowdragmc.lowdraglib.utils.virtuallevel;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Author: KilaBash
 * Date: 2022/04/21
 * Description:
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FacadeBlockAndTintGetter implements BlockAndTintGetter {
    public final BlockAndTintGetter parent;
    public final BlockPos pos;
    public final BlockState state;
    public final BlockEntity tile;

    public FacadeBlockAndTintGetter(BlockAndTintGetter parent, BlockPos pos, BlockState state, @Nullable BlockEntity tile) {
        this.parent = parent;
        this.pos = pos;
        this.state = state;
        this.tile = tile;
    }


    @Override
    public float getShade(Direction pDirection, boolean pIsShade) {
        return parent.getShade(pDirection, pIsShade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return parent.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pBlockPos, ColorResolver pColorResolver) {
        return parent.getBlockTint(pBlockPos, pColorResolver);
    }

    @Override
    public int getBrightness(LightLayer pLightType, BlockPos pBlockPos) {
        return parent.getBrightness(pLightType, pBlockPos);
    }

    @Override
    public int getRawBrightness(BlockPos pBlockPos, int pAmount) {
        return parent.getRawBrightness(pBlockPos, pAmount);
    }

    @Override
    public boolean canSeeSky(BlockPos pBlockPos) {
        return parent.canSeeSky(pBlockPos);
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pPos) {
        return pPos.equals(pos) ? tile : parent.getBlockEntity(pPos);
    }

    @Override
    public BlockState getBlockState(BlockPos pPos) {
        return pPos.equals(pos) ? state : parent.getBlockState(pPos);

    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        return parent.getFluidState(pPos);
    }

    @Override
    public int getLightEmission(BlockPos pPos) {
        return parent.getLightEmission(pPos);
    }

    @Override
    public int getMaxLightLevel() {
        return parent.getMaxLightLevel();
    }

    @Override
    public int getHeight() {
        return parent.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return parent.getMinBuildHeight();
    }

    @Override
    public int getMaxBuildHeight() {
        return parent.getMaxBuildHeight();
    }

    @Override
    public int getSectionsCount() {
        return parent.getSectionsCount();
    }

    @Override
    public int getMinSection() {
        return parent.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return parent.getMaxSection();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pPos) {
        return parent.isOutsideBuildHeight(pPos);
    }

    @Override
    public boolean isOutsideBuildHeight(int pY) {
        return parent.isOutsideBuildHeight(pY);
    }

    @Override
    public int getSectionIndex(int pY) {
        return parent.getSectionIndex(pY);
    }

    @Override
    public int getSectionIndexFromSectionY(int pSectionIndex) {
        return parent.getSectionIndexFromSectionY(pSectionIndex);
    }

    @Override
    public int getSectionYFromSectionIndex(int pSectionIndex) {
        return parent.getSectionYFromSectionIndex(pSectionIndex);
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB pArea) {
        return parent.getBlockStates(pArea);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext pContext) {
        return parent.isBlockInLine(pContext);
    }

    @Override
    public BlockHitResult clip(ClipContext pContext) {
        return parent.clip(pContext);
    }

    @Nullable
    @Override
    public BlockHitResult clipWithInteractionOverride(Vec3 pStartVec, Vec3 pEndVec, BlockPos pPos, VoxelShape pShape, BlockState pState) {
        return parent.clipWithInteractionOverride(pStartVec, pEndVec, pPos, pShape, pState);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape pShape, Supplier<VoxelShape> p_242402_2_) {
        return parent.getBlockFloorHeight(pShape, p_242402_2_);
    }

    @Override
    public double getBlockFloorHeight(BlockPos pPos) {
        return parent.getBlockFloorHeight(pPos);
    }

//    @Override
//    public @Nullable BlockEntity getExistingBlockEntity(BlockPos pos) {
//        return pos.equals(this.pos) ? tile : BlockAndTintGetter.super.getExistingBlockEntity(pos);
//    }
}
