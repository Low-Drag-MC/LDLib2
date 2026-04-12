package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class BlockUIMenuType {
    
    public record BlockUIOpeningData(BlockPos pos, BlockState state) {}
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockState> BLOCK_STATE_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockUIOpeningData> STREAM_CODEC = StreamCodec.of(
        (buf, data) -> {
            buf.writeBlockPos(data.pos());
            BLOCK_STATE_STREAM_CODEC.encode(buf, data.state());
        },
        buf -> new BlockUIOpeningData(buf.readBlockPos(), BLOCK_STATE_STREAM_CODEC.decode(buf))
    );

    public static boolean openUI(ServerPlayer player, BlockPos pos) {
        var blockstate = player.level().getBlockState(pos);
        if (blockstate.getBlock() instanceof BlockUI blockUI) {
            var holder = blockUI.createUIHolder(player, pos, blockstate);
            player.openMenu(holder);
            return true;
        }
        return false;
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, BlockUIOpeningData data) {
        var player = inv.player;
        var pos = data.pos();
        var blockstate = data.state();
        if (blockstate.getBlock() instanceof BlockUI blockUI) {
            var holder = blockUI.createUIHolder(player, pos, blockstate);
            return new ModularUIContainerMenu(LDMenuTypes.BLOCK_UI, windowId, inv, holder);
        }
        throw new IllegalArgumentException("No block ui found for block " + blockstate);
    }

    @FunctionalInterface
    public interface BlockUI {
        ModularUI createUI(BlockUIHolder holder);

        default BlockUIHolder createUIHolder(Player player, BlockPos pos, BlockState blockState) {
            return new BlockUIHolder(this, player, pos, blockState);
        }

        default boolean stillValid(BlockUIHolder holder) {
            return holder.blockState.is(holder.player.level().getBlockState(holder.pos).getBlock());
        }

        default Component getUIDisplayName(BlockUIHolder holder) {
            return holder.blockState.getBlock().getName();
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class BlockUIHolder implements ExtendedScreenHandlerFactory<BlockUIOpeningData>, IContainerUIHolder {
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
            return new ModularUIContainerMenu(LDMenuTypes.BLOCK_UI, containerId, playerInventory, this);
        }

        @Override
        public BlockUIOpeningData getScreenOpeningData(net.minecraft.server.level.ServerPlayer player) { return new BlockUIOpeningData(pos, blockState); }

        @Override
        public ModularUI createUI(Player player) {
            return this.blockUI.createUI(this);
        }
    }
}
