package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@KJSBindings
public final class SupplierDataSource<T> implements IDataProvider<T>, ITickable {
    @Getter
    private final Supplier<T> supplier;
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private volatile T lastValue;
    @Setter @Getter @Accessors(chain = true, fluent = true)
    private int frequency = 1;
    // runtime
    private int counter = 0;

    private SupplierDataSource(Supplier<T> supplier) {
        this.supplier = supplier;
        this.lastValue = supplier.get();
    }

    public static <T> SupplierDataSource<T> of(Supplier<T> supplier) {
        return new SupplierDataSource<>(supplier);
    }

    public <D> SupplierDataSource<D> map(Function<T, D> mapper) {
        return SupplierDataSource.of(() -> mapper.apply(getValue()));
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

    @Override
    public void tick() {
        if (frequency > 1) {
            if (++counter % frequency != 0) return;
        }
        checkUpdate();
    }
}
