package com.lowdragmc.lowdraglib2.utils.virtuallevel;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WrappedBlockAndTintGetter implements BlockAndTintGetter {
    @Getter @Setter
    private Level world;

    public WrappedBlockAndTintGetter(Level world) {
        this.world = world;
    }

    public int calculateBlockTint(BlockPos pos, ColorResolver colorResolver) {
        int dist = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (dist == 0) {
            return colorResolver.getColor(world.getBiome(pos).value(), pos.getX(), pos.getZ());
        } else {
            int count = (dist * 2 + 1) * (dist * 2 + 1);
            int totalRed = 0;
            int totalGreen = 0;
            int totalBlue = 0;
            Cursor3D cursor = new Cursor3D(pos.getX() - dist, pos.getY(), pos.getZ() - dist, pos.getX() + dist, pos.getY(), pos.getZ() + dist);
            BlockPos.MutableBlockPos nextPos = new BlockPos.MutableBlockPos();

            while (cursor.advance()) {
                nextPos.set(cursor.nextX(), cursor.nextY(), cursor.nextZ());
                int color = colorResolver.getColor(world.getBiome(nextPos).value(), nextPos.getX(), nextPos.getZ());
                totalRed += ARGB.red(color);
                totalGreen += ARGB.green(color);
                totalBlue += ARGB.blue(color);
            }

            return ARGB.color(totalRed / count, totalGreen / count, totalBlue / count);
        }
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return world.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return world.getBlockEntity(pos, type);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return world.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return world.getFluidState(pos);
    }

    @Override
    public int getLightEmission(BlockPos pos) {
        return world.getLightEmission(pos);
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB box) {
        return world.getBlockStates(box);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext c) {
        return world.isBlockInLine(c);
    }

    @Override
    public BlockHitResult clip(ClipContext c) {
        return world.clip(c);
    }

    @Override
    public @Nullable BlockHitResult clipWithInteractionOverride(Vec3 from, Vec3 to, BlockPos pos, VoxelShape blockShape, BlockState blockState) {
        return world.clipWithInteractionOverride(from, to, pos, blockShape, blockState);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape blockShape, Supplier<VoxelShape> belowBlockShape) {
        return world.getBlockFloorHeight(blockShape, belowBlockShape);
    }

    @Override
    public double getBlockFloorHeight(BlockPos pos) {
        return world.getBlockFloorHeight(pos);
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return world.dimensionType().cardinalLightType().get();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return calculateBlockTint(pos, resolver);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return world.getLightEngine();
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return world.getBrightness(layer, pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return world.getRawBrightness(pos, darkening);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return world.canSeeSky(pos);
    }

    @Override
    public int getHeight() {
        return world.getHeight();
    }

    @Override
    public int getMinY() {
        return world.getMinY();
    }

    @Override
    public int getMaxY() {
        return world.getMaxY();
    }

    @Override
    public int getSectionsCount() {
        return world.getSectionsCount();
    }

    @Override
    public int getMinSectionY() {
        return world.getMinSectionY();
    }

    @Override
    public int getMaxSectionY() {
        return world.getMaxSectionY();
    }

    @Override
    public boolean isInsideBuildHeight(BlockPos pos) {
        return world.isInsideBuildHeight(pos);
    }

    @Override
    public boolean isInsideBuildHeight(int blockY) {
        return world.isInsideBuildHeight(blockY);
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return world.isOutsideBuildHeight(pos);
    }

    @Override
    public boolean isOutsideBuildHeight(int blockY) {
        return world.isOutsideBuildHeight(blockY);
    }

    @Override
    public int getSectionIndex(int blockY) {
        return world.getSectionIndex(blockY);
    }

    @Override
    public int getSectionIndexFromSectionY(int sectionY) {
        return world.getSectionIndexFromSectionY(sectionY);
    }

    @Override
    public int getSectionYFromSectionIndex(int sectionIndex) {
        return world.getSectionYFromSectionIndex(sectionIndex);
    }

    @Override
    public @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return world.getAuxLightManager(pos);
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        return world.getModelData(pos);
    }
}
