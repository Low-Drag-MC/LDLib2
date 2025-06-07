package com.lowdragmc.lowdraglib2.editor_outdated.configurator;

import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ValueConfigurator<T> extends Configurator {
    protected boolean forceUpdate;
    @Nullable
    protected T value;
    @Nonnull
    protected T defaultValue;
    @Setter
    protected Consumer<T> onUpdate;
    @Setter
    protected Supplier<T> supplier;

    public ValueConfigurator(String name, Supplier<T> supplier, Consumer<T> onUpdate, @Nonnull T defaultValue, boolean forceUpdate) {
        super(name);
        setClientSideWidget();
        this.supplier = supplier;
        this.onUpdate = onUpdate;
        this.defaultValue = defaultValue;
        this.forceUpdate = forceUpdate;
        this.value = supplier.get();
        this.name = name;
    }

    /**
     * when you update value, you have to call it to notify changes.
     * if necessary you should call {@link #onValueUpdate(T)} to update the value. (e.g. do some widget update in the method)
     */
    protected void updateValue() {
        if (onUpdate != null) {
            onUpdate.accept(value);
        }
        notifyChanges();
    }

    /**
     * it will be called when the value is updated and be detected passively.
     * <br/>
     * you can update widget or do something else in this method.
     * <br/>
     * to update the value, call {@link #updateValue()} as well
     */
    protected void onValueUpdate(T newValue) {
        value = newValue;
    }

    public void setValue(T value) {
        onValueUpdate(value);
    }

    @Nullable
    public T getValue() {
        return value;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (forceUpdate) {
            onValueUpdate(supplier.get());
        }
    }

}
