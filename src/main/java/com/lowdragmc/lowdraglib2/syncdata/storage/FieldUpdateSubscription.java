package com.lowdragmc.lowdraglib2.syncdata.storage;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote FieldUpdateSubscription
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
public abstract class FieldUpdateSubscription implements ISubscription {
    @NotNull
    public final ManagedKey key;
    @NotNull
    public final IFieldUpdateListener<?> listener;

    public FieldUpdateSubscription(@NotNull ManagedKey key, @NotNull IFieldUpdateListener<?> listener) {
        this.key = key;
        this.listener = listener;
    }

    @Override
    abstract public void unsubscribe();
}
