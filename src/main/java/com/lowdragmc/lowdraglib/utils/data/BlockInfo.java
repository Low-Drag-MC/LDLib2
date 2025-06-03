package com.lowdragmc.lowdraglib.utils.data;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@NoArgsConstructor
public class BlockInfo implements IPersistedSerializable, IConfigurable {
    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR);

    @Setter
    @Configurable
    private BlockState blockState;
    @Setter
    private boolean hasBlockEntity;
    @Setter
    @Configurable
    private CompoundTag tag;
    @Setter
    @Configurable
    private ItemStack itemStack;
    @Setter
    private Consumer<BlockEntity> postCreate;

    public BlockInfo(Block block) {
        this(block.defaultBlockState());
    }

    public BlockInfo(BlockState blockState) {
        this(blockState, false);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity) {
        this(blockState, hasBlockEntity, null, null);
    }

    public BlockInfo(BlockState blockState, Consumer<BlockEntity> postCreate) {
        this(blockState, true, null, postCreate);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack, Consumer<BlockEntity> postCreate) {
        this.blockState = blockState;
        this.hasBlockEntity = hasBlockEntity;
        this.itemStack = itemStack;
        this.postCreate = postCreate;
    }

    public static BlockInfo fromBlockState(BlockState state) {
        try {
            if (state.getBlock() instanceof EntityBlock) {
                BlockEntity blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state);
                if (blockEntity != null) {
                    return new BlockInfo(state, true);
                }
            }
        } catch (Exception ignored){ }
        return new BlockInfo(state);
    }

    public static BlockInfo fromBlock(Block block) {
        return BlockInfo.fromBlockState(block.defaultBlockState());
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public void postEntity(BlockEntity blockEntity) {
        if (tag != null && blockEntity != null) {
            var compoundTag2 = blockEntity.saveWithoutMetadata(Platform.getFrozenRegistry());
            var compoundTag3 = compoundTag2.copy();
            compoundTag2.merge(tag);
            if (!compoundTag2.equals(compoundTag3)) {
                blockEntity.loadWithComponents(compoundTag2, Platform.getFrozenRegistry());
            }
        }
        if (postCreate != null && blockEntity != null) {
            postCreate.accept(blockEntity);
        }
    }

    public ItemStack getItemStackForm() {
        return itemStack == null ? new ItemStack(blockState.getBlock()) : itemStack;
    }

    public ItemStack getItemStackForm(LevelReader level, BlockPos pos) {
        if (itemStack != null) return itemStack;
        return blockState.getBlock().getCloneItemStack(level, pos, blockState);
    }

}
