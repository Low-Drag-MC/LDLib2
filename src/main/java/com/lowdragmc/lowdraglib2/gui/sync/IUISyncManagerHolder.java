package com.lowdragmc.lowdraglib2.gui.sync;

import net.minecraft.network.RegistryFriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

public interface IUISyncManagerHolder {
    @Nullable
    UISyncManager getSyncManager();

    default void writeInitialData(RegistryFriendlyByteBuf buf) {
        var syncManager = getSyncManager();
        if (syncManager == null) return;
        syncManager.writeInitialData(buf);
    }

    default void readInitialData(RegistryFriendlyByteBuf buf) {
        var syncManager = getSyncManager();
        if (syncManager == null) return;
        syncManager.readInitialData(buf);
    }
}
