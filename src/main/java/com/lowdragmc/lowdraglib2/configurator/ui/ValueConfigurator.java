package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
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
        this.supplier = supplier;
        this.onUpdate = onUpdate;
        this.defaultValue = defaultValue;
        this.forceUpdate = forceUpdate;
        this.value = supplier.get();

        inlineContainer.addEventListener(UIEvents.DRAG_PERFORM, this::onDragPerform);
        inlineContainer.addEventListener(UIEvents.DRAG_ENTER, this::onDragEnter, true);
        inlineContainer.addEventListener(UIEvents.DRAG_LEAVE, this::onDragLeave, true);

        setPastable(defaultValue.getClass(), pasted -> {
            if (pasted != null) {
                onPaste((T) pasted);
            }
        });
    }

    public ValueConfigurator<T> setCopiable(Function<T, T> copyFunction) {
        setCopiable(() -> {
           var copied = copyFunction.apply(value);
           return () -> copyFunction.apply(copied);
        });
        return this;
    }

    protected void onPaste(T pasted) {
        onValueUpdatePassively(pasted);
        updateValue();
    }

    /**
     * when you update value, you have to call it to notify changes.
     * if necessary you should call {@link #onValueUpdatePassively(T)} to update the value. (e.g. do some widget update in the method)
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
     * to notify the value change, use {@link #updateValueActively} instead
     */
    protected void onValueUpdatePassively(T newValue) {
        this.value = newValue;
    }

    /**
     * update value actively.
     */
    protected void updateValueActively(T newValue) {
        this.value = newValue;
        updateValue();
    }

    /**
     * Set value.
     */
    private void setValue(T value, boolean notify) {
        onValueUpdatePassively(value);
        if (notify) {
            updateValue();
        }
    }

    @Nullable
    public T getValue() {
        return value;
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (forceUpdate) {
            onValueUpdatePassively(supplier.get());
        }
    }

    /// Drag value handler
    protected boolean canDropObject(@Nonnull Object object) {
        return defaultValue.getClass().isAssignableFrom(object.getClass());
    }

    protected void onDropObject(@Nonnull Object object) {
        if (canDropObject(object)) {
            onValueUpdatePassively((T) object);
            updateValue();
        }
    }

    protected void onDragEnter(UIEvent event) {
        if (event.dragHandler.draggingObject != null && canDropObject(event.dragHandler.draggingObject) && event.dragHandler.dragSource != this) {
            showDroppableOverlay();
        }
    }

    protected void showDroppableOverlay() {
        inlineContainer.style(style -> style.overlayTexture(ColorPattern.T_BLUE.rectTexture()));
    }

    protected void onDragLeave(UIEvent event) {
        hideDroppableOverlay();
    }

    protected void hideDroppableOverlay() {
        inlineContainer.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
    }

    protected void onDragPerform(UIEvent event) {
        if (event.dragHandler.draggingObject != null && canDropObject(event.dragHandler.draggingObject) && event.dragHandler.dragSource != this) {
            onDropObject(event.dragHandler.draggingObject);
        }
        hideDroppableOverlay();
    }
}
