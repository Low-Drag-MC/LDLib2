package com.lowdragmc.lowdraglib.fabric.core.mixins.kjs;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.kjs.fabric.ISlotWidgetKJS;
import com.lowdragmc.lowdraglib.side.item.fabric.ItemTransferHelperImpl;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SlotWidget.class)
@RemapPrefixForJS("kjs$")
public abstract class SlotWidgetMixin implements ISlotWidgetKJS {
    @Override
    public void kjs$setHandlerSlot(Storage<ItemVariant> itemHandler, int slot) {
        this.kjs$self().setHandlerSlot(ItemTransferHelperImpl.toItemTransfer(itemHandler), slot);
    }
}
