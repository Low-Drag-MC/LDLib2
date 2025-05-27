package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.bindings.IBindable;
import com.lowdragmc.lowdraglib.gui.ui.bindings.IDataSource;
import com.lowdragmc.lowdraglib.gui.ui.bindings.IObserver;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class BindableUIElement<T> extends UIElement implements IBindable<T> {
    protected final List<Consumer<T>> listeners = new ArrayList<>();
    protected final List<ISubscription> observers = new ArrayList<>();

    public ISubscription registerValueListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void bindDataSource(IDataSource<T> dataSource) {
        listeners.add(dataSource::setValue);
    }

    @Override
    public void bindObserver(IObserver<T> observer) {
        observers.add(observer.registerListener(this::setValue, true));
    }

    protected void notifyListeners() {
        var currentValue = getValue();
        for (var listener : listeners) {
            listener.accept(currentValue);
        }
    }

    /**
     * Gets the current value of this bindable UI element.
     */
    public abstract T getValue();

    /**
     * Sets the value of this bindable UI element.
     *
     * @param value   The new value to set.
     * @param notify  Whether to notify listeners of the change.
     */
    public abstract BindableUIElement<T> setValue(T value, boolean notify);

    /**
     * Sets the value of this bindable UI element and notifies listeners.
     *
     * @param value The new value to set.
     */
    public BindableUIElement<T> setValue(T value) {
        setValue(value, true);
        return this;
    }

}
