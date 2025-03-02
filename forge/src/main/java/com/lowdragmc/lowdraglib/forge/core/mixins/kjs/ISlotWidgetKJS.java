package com.lowdragmc.lowdraglib.forge.core.mixins.kjs;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import dev.latvian.mods.kubejs.core.NoMixinException;
import net.minecraftforge.items.IItemHandlerModifiable;

public interface ISlotWidgetKJS {

    default SlotWidget kjs$self() {
        return (SlotWidget) this;
    }

    default void kjs$setHandlerSlot(IItemHandlerModifiable itemHandler, int slot) {
        throw new NoMixinException();
    }
}
