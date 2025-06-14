package com.lowdragmc.lowdraglib2.syncdata.storage;

import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;

import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote FieldUpdateListener
 */
@FunctionalInterface
public
interface IFieldUpdateListener<T> {
    /**
     * It is called at the remote side when a field is updated from the server.
     * @param managedKey the metadata of the field
     * @param currentValue the current value of the field before the update
     * @return the new value of the field after the update
     */
    Consumer<T> onFieldUpdated(ManagedKey managedKey, T currentValue);
}
