package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import com.lowdragmc.lowdraglib2.gui.factory.BlockUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

public class KJSBlockUIMenuType {
    public static boolean openUI(ServerPlayer player, BlockPos pos, String id) {
        var blockstate = player.level().getBlockState(pos);
        var event = new BlockUIEventJS(player, pos, blockstate, id);
        UIEvents.BLOCK.post(ScriptType.SERVER, id, event);
        return player.openMenu(event).isPresent();
    }

    public static ModularUIContainerMenu create(int windowId, Inventory inv, RegistryFriendlyByteBuf data) {
        var player = inv.player;
        var pos = data.readBlockPos();
        var blockstate = BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.decode(data);
        var id = data.readUtf();
        var event = new BlockUIEventJS(player, pos, blockstate, id);
        UIEvents.BLOCK.post(ScriptType.CLIENT, id, event);
        return event.createMenu(windowId, inv, player);
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class BlockUIEventJS extends UIEventJS {
        public final BlockPos pos;
        public final BlockState blockState;

        public BlockUIEventJS(Player player, BlockPos pos, BlockState blockState, String id) {
            super(player, id);
            this.pos = pos;
            this.blockState = blockState;

            // valid if still the same block
            this.validator = p -> blockState.is(p.level().getBlockState(pos).getBlock());
            this.displayName = blockState.getBlock().getName();
        }

        @Override
        public MenuType<ModularUIContainerMenu> getMenuType() {
            return LDKJSMenuTypes.BLOCK_UI.get();
        }

        @Override
        @HideFromJS
        public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            BlockUIMenuType.BLOCK_STATE_STREAM_CODEC.encode(buffer, blockState);
            super.writeClientSideData(menu, buffer);
        }
    }
}
