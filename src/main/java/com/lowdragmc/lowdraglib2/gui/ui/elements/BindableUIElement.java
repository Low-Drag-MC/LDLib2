package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventListener;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import net.minecraft.MethodsReturnNonnullByDefault;
import com.lowdragmc.lowdraglib2.gui.util.ITickable;

import org.jetbrains.annotations.Nullable;
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
    public Collection<IObserver<T>> getBoundObservers() {
        return observers.keySet();
    }

    @Override
    public BindableUIElement<T> bindObserver(IObserver<T> observer) {
        if (observers.containsKey(observer)) {
            LDLib2.LOGGER.warn("Trying to bind an observer to a bindable UI element that already has a binding to it.");
            return this;
        }
        UIEventListener tickableListener;
        if (observer instanceof ITickable tickable) {
            tickableListener = e -> tickable.tick();
            addEventListener(UIEvents.TICK, tickableListener);
        } else {
            tickableListener = null;
        }
        var subscription = registerValueListener(observer::onValueChanged);
        if (tickableListener != null) {
            subscription.andThen(() -> removeEventListener(UIEvents.TICK, tickableListener));
        }
        observers.put(observer, subscription);
        return this;
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
    public Collection<IDataProvider<T>> getBoundDataSources() {
        return dataSources.keySet();
    }

    @Override
    public BindableUIElement<T> bindDataSource(IDataProvider<T> dataProvider) {
        return bindDataSource(dataProvider, true);
    }

    public BindableUIElement<T> bindDataSource(IDataProvider<T> dataProvider, boolean notify) {
        if (dataSources.containsKey(dataProvider)) {
            LDLib2.LOGGER.warn("Trying to bind an dataProvider to a bindable UI element that already has a binding to it.");
            return this;
        }
        UIEventListener tickableListener;
        if (dataProvider instanceof ITickable tickable) {
            tickableListener = e -> tickable.tick();
            addEventListener(UIEvents.TICK, tickableListener);
        } else {
            tickableListener = null;
        }
        var subscription = dataProvider.registerListener(v -> setValue(v, notify), true);
        if (tickableListener != null) {
            subscription.andThen(() -> removeEventListener(UIEvents.TICK, tickableListener));
        }
        dataSources.put(dataProvider, subscription);
        return this;
    }

    @Override
    public BindableUIElement<T> unbindDataSource(IDataProvider<T> dataProvider) {
        var removed = dataSources.remove(dataProvider);
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
