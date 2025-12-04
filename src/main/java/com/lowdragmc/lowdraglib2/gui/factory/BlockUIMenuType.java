package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class BlockUIMenuType {
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC);

    /**
     * Opens a UI for the specified player at the given block position if the block at that position
     * supports a {@link BlockUI}.
     *
     * @param player the {@link Player} for whom the UI should be opened.
     * @param pos the {@link BlockPos} representing the position of the block where the UI is being opened.
     * @return {@code true} if the UI was successfully opened, {@code false} otherwise.
     */
    public static boolean openUI(ServerPlayer player, BlockPos pos) {
        var blockstate = player.level().getBlockState(pos);
        if (blockstate.getBlock() instanceof BlockUI blockUI) {
            var holder = blockUI.createUIHolder(player, pos, blockstate);
            return player.openMenu(holder).isPresent();
        }
        return false;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var player = inv.player;
        var pos = data.readBlockPos();
        var blockstate = BLOCK_STATE_STREAM_CODEC.decode(data);
        if (blockstate.getBlock() instanceof BlockUI blockUI) {
            var holder = blockUI.createUIHolder(player, pos, blockstate);
            return new ModularUIContainerMenu(LDMenuTypes.BLOCK_UI.get(), windowId, inv, holder);
        }
        throw new IllegalArgumentException("No held item ui found for block " + blockstate);
    }

    @FunctionalInterface
    public interface BlockUI {
        /**
         * Creates and returns the {@link ModularUI} associated with the provided {@link BlockUIHolder}.
         *
         * @param holder the {@link BlockUIHolder} containing contextual information for constructing the {@link ModularUI}.
         * @return a {@link ModularUI} generated based on the given {@link BlockUIHolder}.
         */
        ModularUI createUI(BlockUIHolder holder);

        /**
         * Creates a {@link BlockUIHolder} for handling UI interactions associated with a specific block in the world.
         *
         * @param player the {@link Player} interacting with the block.
         * @param pos the {@link BlockPos} representing the position of the block in the world.
         * @param blockState the {@link BlockState} representing the current state of the block.
         * @return a new {@link BlockUIHolder} instance containing the provided contextual information.
         */
        default BlockUIHolder createUIHolder(Player player, BlockPos pos, BlockState blockState) {
            return new BlockUIHolder(this, player, pos, blockState);
        }

        /**
         * Determines whether the block state associated with the given {@link BlockUIHolder}
         * is still valid in the current game context.
         *
         * @param holder the {@link BlockUIHolder} that contains the block state, player, and position data.
         * @return {@code true} if the block state in the {@link BlockUIHolder} matches the current block state at the position;
         *         {@code false} otherwise.
         */
        default boolean stillValid(BlockUIHolder holder) {
            // valid if still the same block
            return holder.blockState.is(holder.player.level().getBlockState(holder.pos).getBlock());
        }

        default Component getUIDisplayName(BlockUIHolder holder) {
            return holder.blockState.getBlock().getName();
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class BlockUIHolder implements MenuProvider, IContainerUIHolder {
        public final BlockUI blockUI;
        public final Player player;
        public final BlockPos pos;
        public final BlockState blockState;

        public BlockUIHolder(BlockUI blockUI, Player player, BlockPos pos, BlockState blockState) {
            this.blockUI = blockUI;
            this.player = player;
            this.pos = pos;
            this.blockState = blockState;
        }

        @Override
        public boolean isStillValid(Player player) {
            return blockUI.stillValid(this);
        }

        @Override
        public Component getDisplayName() {
            return blockUI.getUIDisplayName(this);
        }

        @Override
        @Nullable
        public ModularUIContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new ModularUIContainerMenu(LDMenuTypes.BLOCK_UI.get(), containerId, playerInventory, this);
        }

        @Override
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            BLOCK_STATE_STREAM_CODEC.encode(buffer, blockState);
        }

        @Override
        public ModularUI createUI(Player player) {
            return this.blockUI.createUI(this);
        }
    }
}
