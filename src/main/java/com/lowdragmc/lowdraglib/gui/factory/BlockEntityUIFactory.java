package com.lowdragmc.lowdraglib.gui.factory;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockEntityUIFactory extends UIFactory<BlockEntity> {
    public static final BlockEntityUIFactory INSTANCE  = new BlockEntityUIFactory();

    private BlockEntityUIFactory() {
        super(LDLib.id("block_entity"));
    }

    @Override
    protected ModularUI createUITemplate(BlockEntity holder, Player entityPlayer) {
        if (holder instanceof IUIHolder) {
            return ((IUIHolder) holder).createUI(entityPlayer);
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected BlockEntity readHolderFromSyncData(RegistryFriendlyByteBuf syncData) {
        Level world = Minecraft.getInstance().level;
        return world == null ? null : world.getBlockEntity(syncData.readBlockPos());
    }

    @Override
    protected void writeHolderToSyncData(RegistryFriendlyByteBuf syncData, BlockEntity holder) {
        syncData.writeBlockPos(holder.getBlockPos());
    }
}
