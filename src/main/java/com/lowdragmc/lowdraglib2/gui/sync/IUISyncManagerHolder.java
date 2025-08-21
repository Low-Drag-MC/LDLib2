package com.lowdragmc.lowdraglib2.gui.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;

public interface IUISyncManagerHolder {
    UISyncManager getSyncManager();

    default void writeInitialData(RegistryFriendlyByteBuf buf) {
        getSyncManager().writeInitialData(buf);
    }

    default void readInitialData(RegistryFriendlyByteBuf buf) {
        getSyncManager().readInitialData(buf);
    }
}
