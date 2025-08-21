package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BindableUIElement<T> extends UIElement implements IBindable<T>, IObservable<T>, IDataConsumer<T> {
    protected final List<Consumer<T>> listeners = new ArrayList<>();
    protected final Map<IObserver<T>, ISubscription> observers = new LinkedHashMap<>();
    protected final Map<IDataProvider<T>, ISubscription> dataSources = new LinkedHashMap<>();

    public ISubscription registerValueListener(Consumer<T> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public BindableUIElement<T> bindObserver(IObserver<T> observer) {
        bindDataSource(observer, true);
        return this;
    }

    public void bindDataSource(IObserver<T> dataSource, boolean notify) {
        if (observers.containsKey(dataSource)) {
            LDLib2.LOGGER.warn("Trying to bind a data source to a bindable UI element that already has a binding to it.");
            return;
        }
        observers.put(dataSource, registerValueListener(v -> dataSource.setValue(v, notify)));
    }

    @Override
    public BindableUIElement<T> unbindObserver(IObserver<T> observer) {
        var removed = observers.remove(observer);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
    }

    @Override
    public BindableUIElement<T> bindDataSource(IDataProvider<T> dataSource) {
        bindObserver(dataSource, true);
        return this;
    }

    public void bindObserver(IDataProvider<T> observer, boolean notify) {
        if (dataSources.containsKey(observer)) {
            LDLib2.LOGGER.warn("Trying to bind an observer to a bindable UI element that already has a binding to it.");
            return;
        }
        dataSources.put(observer, observer.registerListener(v -> setValue(v, notify), true));
    }

    @Override
    public BindableUIElement<T> unbindDataSource(IDataProvider<T> dataSource) {
        var removed = dataSources.remove(dataSource);
        if (removed != null) {
            removed.unsubscribe();
        }
        return this;
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
    public abstract BindableUIElement<T> setValue(@Nullable T value, boolean notify);

    /**
     * Sets the value of this bindable UI element and notifies listeners.
     *
     * @param value The new value to set.
     */
    public BindableUIElement<T> setValue(@Nullable T value) {
        setValue(value, true);
        return this;
    }

}
