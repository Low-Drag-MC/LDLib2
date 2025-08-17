package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IObserver;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data(staticConstructor = "of")
public final class SupplierObserver<T> implements IObserver<T> {
    @Getter
    private final Supplier<T> supplier;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private volatile T lastValue;

    private SupplierObserver(Supplier<T> supplier) {
        this.supplier = supplier;
        this.lastValue = supplier.get();
    }

    @Override
    public ISubscription registerListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public T getValue() {
        return supplier.get();
    }

    public void checkUpdate() {
        T currentValue = getValue();
        if (!Objects.equals(lastValue, currentValue)) {
            lastValue = currentValue;
            listeners.forEach(l -> l.accept(currentValue));
        }
    }

}
