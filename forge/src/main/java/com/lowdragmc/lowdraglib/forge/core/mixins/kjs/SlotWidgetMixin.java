package com.lowdragmc.lowdraglib.forge.core.mixins.kjs;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SlotWidget.class)
@RemapPrefixForJS("kjs$")
public abstract class SlotWidgetMixin implements ISlotWidgetKJS {
    @Override
    public void kjs$setHandlerSlot(IItemHandlerModifiable itemHandler, int slot) {
        this.kjs$self().setHandlerSlot(ItemTransferHelperImpl.toItemTransfer(itemHandler), slot);
    }
}
