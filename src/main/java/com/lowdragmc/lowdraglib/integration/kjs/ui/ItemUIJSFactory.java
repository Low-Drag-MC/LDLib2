package com.lowdragmc.lowdraglib.integration.kjs.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ItemUIJSFactory extends UIFactory<ItemUIJSFactory.ItemAccess> {
    public static final ItemUIJSFactory INSTANCE = new ItemUIJSFactory();
    public record ItemAccess(InteractionHand hand, String uiName) { }

    private ItemUIJSFactory() {
        super(LDLib.location("item_js"));
    }

    @RemapForJS("openUI")
    public boolean kjs$openUI(Player player, InteractionHand hand, String uiName) {
        if (player instanceof ServerPlayer serverPlayer) {
            return openUI(new ItemAccess(hand, uiName), serverPlayer);
        }
        return false;
    }

    @Override
    protected ModularUI createUITemplate(ItemAccess holder, Player entityPlayer) {
        var held = entityPlayer.getItemInHand(holder.hand);
        var result = UIEvents.ITEM.post(new UIEvents.ItemUIEventJS(entityPlayer, holder.hand, held), holder.uiName);
        if (result.value() instanceof WidgetGroup root && !result.interruptFalse()) {
            return new ModularUI(root, new IUIHolder() {
                @Override
                public ModularUI createUI(Player entityPlayer) {
                    return null;
                }

                @Override
                public boolean isInvalid() {
                    return !ItemStack.isSameItemSameComponents(entityPlayer.getItemInHand(holder.hand), held);
                }

                @Override
                public boolean isRemote() {
                    return entityPlayer.level().isClientSide;
                }

                @Override
                public void markAsDirty() {

                }
            }, entityPlayer);
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected ItemAccess readHolderFromSyncData(RegistryFriendlyByteBuf syncData) {
        return new ItemAccess(syncData.readEnum(InteractionHand.class), syncData.readUtf());
    }

    @Override
    protected void writeHolderToSyncData(RegistryFriendlyByteBuf syncData, ItemAccess holder) {
        syncData.writeEnum(holder.hand);
        syncData.writeUtf(holder.uiName);
    }
}
