package com.lowdragmc.lowdraglib.kjs.fabric;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import dev.latvian.mods.kubejs.core.NoMixinException;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

public interface ISlotWidgetKJS {

    default SlotWidget kjs$self() {
        return (SlotWidget) this;
    }

    default void kjs$setHandlerSlot(Storage<ItemVariant> itemHandler, int slot) {
        throw new NoMixinException();
    }
}
