package com.lowdragmc.lowdraglib2.gui.modular;

import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface WidgetUIAccess {

    boolean attemptMergeStack(ItemStack itemStack, boolean fromContainer, boolean simulate);

    void writeClientAction(Widget widget, int id, Consumer<RegistryFriendlyByteBuf> payloadWriter);

    void writeUpdateInfo(Widget widget, int id, Consumer<RegistryFriendlyByteBuf> payloadWriter);

}
