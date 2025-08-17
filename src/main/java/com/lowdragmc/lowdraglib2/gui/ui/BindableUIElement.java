package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IObserver;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BindableUIElement<T> extends UIElement implements IBindable<T> {
    protected final List<Consumer<T>> listeners = new ArrayList<>();
    protected final Map<IDataSource<T>, ISubscription> dataSources = new LinkedHashMap<>();
    protected final Map<IObserver<T>, ISubscription> observers = new LinkedHashMap<>();

    public ISubscription registerValueListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void bindDataSource(IDataSource<T> dataSource) {
        bindDataSource(dataSource, true);
    }

    public void bindDataSource(IDataSource<T> dataSource, boolean notify) {
        if (dataSources.containsKey(dataSource)) {
            LDLib2.LOGGER.warn("Trying to bind a data source to a bindable UI element that already has a binding to it.");
            return;
        }
        dataSources.put(dataSource, registerValueListener(v -> dataSource.setValue(v, notify)));
    }

    @Override
    public void unbindDataSource(IDataSource<T> dataSource) {
        var removed = dataSources.remove(dataSource);
        if (removed != null) {
            removed.unsubscribe();
        }
    }

    @Override
    public void bindObserver(IObserver<T> observer) {
        bindObserver(observer, true);
    }

    public void bindObserver(IObserver<T> observer, boolean notify) {
        if (observers.containsKey(observer)) {
            LDLib2.LOGGER.warn("Trying to bind an observer to a bindable UI element that already has a binding to it.");
            return;
        }
        observers.put(observer, observer.registerListener(v -> setValue(v, notify), true));
    }

    @Override
    public void unbindObserver(IObserver<T> observer) {
        var removed = observers.remove(observer);
        if (removed != null) {
            removed.unsubscribe();
        }
    }

    protected final void notifyListeners() {
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
