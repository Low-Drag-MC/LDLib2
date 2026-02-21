package com.lowdragmc.lowdraglib2.gui.sync.bindings.impl;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IObserver;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TrackData<T> implements IDataProvider<T>, IObserver<T> {
    @Getter
    private T value;
    private final List<Consumer<T>> listeners = new ArrayList<>();

    public TrackData(T initialValue) {
        this.value = initialValue;
    }

    @Override
    public ISubscription registerListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void onValueChanged(T value) {
        setValueSilent(value);
    }

    public void setValue(T value, boolean notify) {
        this.value = value;
        if (notify) {
            update();
        }
    }

    public void setValue(T value) {
        setValue(value, true);
    }

    public void setValueSilent(T value) {
        setValue(value, false);
    }

    public void update() {
        listeners.forEach(l -> l.accept(value));
    }
}
