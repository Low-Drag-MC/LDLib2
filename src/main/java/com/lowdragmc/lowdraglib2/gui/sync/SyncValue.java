package com.lowdragmc.lowdraglib2.gui.sync;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.SyncValueHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SyncValue<T> {
    public final SyncValueHolder<T> syncValueHolder;
    public final List<Consumer<T>> listeners = new ArrayList<>();
    @Nullable @Setter
    public Supplier<T> valueProvider;
    @Getter @Setter
    public boolean acceptSync = true;

    public SyncValue(String name, Type type, @Nullable T value) {
        this.syncValueHolder = new SyncValueHolder<>(name, type, value);
    }

    public void setValue(T value) {
        syncValueHolder.setValue(value);
    }

    public T getValue() {
        return syncValueHolder.getValue();
    }

    public ISubscription addListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void update() {
        if (valueProvider != null) {
            var newValue = valueProvider.get();
            syncValueHolder.setValue(newValue);
        }
        syncValueHolder.ref.update();
    }

    public boolean hasChanged() {
        return syncValueHolder.ref.isSyncDirty();
    }

    public void markAsChanged() {
        syncValueHolder.ref.markAsDirty();
    }

    public void clearChanged() {
        syncValueHolder.ref.clearSyncDirty();
    }

    public void writeSyncData(RegistryFriendlyByteBuf buffer) {
        syncValueHolder.ref.readSyncToStream(buffer);
    }

    public void readSyncData(RegistryFriendlyByteBuf buffer) throws IllegalAccessException {
        if (!acceptSync) {
            throw new IllegalAccessException(syncValueHolder.managedKey.getName() + " receive sync data while it does not accept sync.");
        }
        syncValueHolder.ref.writeSyncFromStream(buffer);
        listeners.forEach(l -> l.accept(getValue()));
    }
}
