package com.lowdragmc.lowdraglib2.gui.sync;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
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
    /**
     * Listeners invoked on the receiving side, right after an incoming value has been applied.
     */
    public final List<Consumer<T>> listeners = new ArrayList<>();
    private final List<Consumer<T>> preSyncListeners = new ArrayList<>();
    private final List<Consumer<T>> postSyncListeners = new ArrayList<>();
    @Nullable @Setter
    public Supplier<T> valueProvider;
    @Getter @Setter
    public boolean acceptSync = true;
    @Getter @Setter
    public boolean toSync = true;
    @Getter @Setter
    public SyncStrategy syncStrategy = SyncStrategy.CHANGED_PERIODIC;

    public SyncValue(String name, Type type, @Nullable T value) {
        this.syncValueHolder = new SyncValueHolder<>(name, type, value);
    }

    public void setValue(T value) {
        syncValueHolder.setValue(value);
    }

    public T getValue() {
        return syncValueHolder.getValue();
    }

    /**
     * Adds a listener that is invoked on the <b>receiving</b> side, after the incoming value has been
     * written into the underlying reference.
     * <p>
     * Note: for collection / map values the accessor mutates the existing instance in place, so the value
     * handed to the listener is the very same instance it saw last time. Do not use it as a snapshot.
     */
    public ISubscription addListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Adds a listener that is invoked on the <b>sending</b> side, right before the value is written to the buffer.
     * <p>
     * Note: this runs inside {@link com.lowdragmc.lowdraglib2.gui.sync.UISyncManager#tick()} while the sync values
     * are being iterated. Do not add / remove sync values (i.e. do not mutate the UI structure) from here,
     * mark a flag and do it on the next tick instead.
     */
    public ISubscription addPreSyncListener(Consumer<T> listener) {
        preSyncListeners.add(listener);
        return () -> preSyncListeners.remove(listener);
    }

    /**
     * Adds a listener that is invoked on the <b>sending</b> side, right after the value has been written to the buffer.
     * <p>
     * Same iteration caveat as {@link #addPreSyncListener(Consumer)} applies.
     */
    public ISubscription addPostSyncListener(Consumer<T> listener) {
        postSyncListeners.add(listener);
        return () -> postSyncListeners.remove(listener);
    }

    private void notifyListeners(List<Consumer<T>> toNotify) {
        if (toNotify.isEmpty()) return;
        var value = getValue();
        // iterate over a snapshot, listeners are allowed to unsubscribe themselves
        for (var listener : List.copyOf(toNotify)) {
            listener.accept(value);
        }
    }

    public void update() {
        if (!toSync) return;
        if (valueProvider != null) {
            var newValue = valueProvider.get();
            syncValueHolder.setValue(newValue);
        }
        syncValueHolder.ref.update();
    }

    public boolean hasChanged() {
        return toSync && (syncStrategy == SyncStrategy.ALWAYS || syncValueHolder.ref.isSyncDirty());
    }

    public void markAsChanged() {
        if (!toSync) return;
        syncValueHolder.ref.markAsDirty();
    }

    public void clearChanged() {
        syncValueHolder.ref.clearSyncDirty();
    }

    public void writeSyncData(RegistryFriendlyByteBuf buffer) {
        notifyListeners(preSyncListeners);
        syncValueHolder.ref.readSyncToStream(buffer);
        notifyListeners(postSyncListeners);
    }

    public void readSyncData(RegistryFriendlyByteBuf buffer) throws IllegalAccessException {
        if (!acceptSync) {
            throw new IllegalAccessException(syncValueHolder.managedKey.getName() + " receive sync data while it does not accept sync.");
        }
        syncValueHolder.ref.writeSyncFromStream(buffer);
        notifyListeners(listeners);
    }
}
