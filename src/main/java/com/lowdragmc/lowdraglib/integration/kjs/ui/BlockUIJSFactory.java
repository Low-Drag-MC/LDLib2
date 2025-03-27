package com.lowdragmc.lowdraglib.integration.kjs.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class BlockUIJSFactory extends UIFactory<BlockUIJSFactory.BlockAccess> {
    public static final BlockUIJSFactory INSTANCE = new BlockUIJSFactory();
    public record BlockAccess(BlockPos pos, String uiName) { }

    private BlockUIJSFactory() {
        super(LDLib.location("block_js"));
    }

    @RemapForJS("openUI")
    @Info("Opens a UI for a player at a specific block position.")
    public boolean kjs$openUI(Player player, BlockPos pos, String uiName) {
        if (player instanceof ServerPlayer serverPlayer) {
            return openUI(new BlockAccess(pos, uiName), serverPlayer);
        }
        return false;
    }

    @Override
    protected ModularUI createUITemplate(BlockAccess holder, Player entityPlayer) {
        var level = entityPlayer.level();
        var pos = holder.pos;
        var block = level.getBlockState(pos);
        var blockEntity = level.getBlockEntity(pos);
        var result = UIEvents.BLOCK.post(new UIEvents.BlockUIEventJS(level, pos, new BlockContainerJS(level, pos), entityPlayer), holder.uiName);
        if (result.value() instanceof WidgetGroup root && !result.interruptFalse()) {
            return new ModularUI(root, new IUIHolder() {
                @Override
                public ModularUI createUI(Player entityPlayer) {
                    return null;
                }

                @Override
                public boolean isInvalid() {
                    if (blockEntity != null && blockEntity.isRemoved()) return true;
                    return !level.getBlockState(pos).is(block.getBlock());
                }

                @Override
                public boolean isRemote() {
                    return level.isClientSide;
                }

                @Override
                public void markAsDirty() {
                    if (blockEntity != null) blockEntity.setChanged();
                }
            }, entityPlayer);
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected BlockAccess readHolderFromSyncData(RegistryFriendlyByteBuf syncData) {
        return new BlockAccess(syncData.readBlockPos(), syncData.readUtf());
    }

    @Override
    protected void writeHolderToSyncData(RegistryFriendlyByteBuf syncData, BlockAccess holder) {
        syncData.writeBlockPos(holder.pos());
        syncData.writeUtf(holder.uiName());
    }
}
