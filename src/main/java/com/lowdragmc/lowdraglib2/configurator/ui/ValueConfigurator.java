package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Accessors(chain = true)
public class ValueConfigurator<T> extends Configurator {
    protected boolean forceUpdate;
    @Nullable
    protected T value;
    @Nullable @Setter
    protected T defaultValue;
    @Setter
    protected Consumer<@Nullable T> onUpdate;
    @Setter
    protected Supplier<@Nullable T> supplier;
    @Setter
    protected Predicate<Object> canDropPredicate = t -> defaultValue != null && defaultValue.getClass().isAssignableFrom(t.getClass());

    public ValueConfigurator(String name, Supplier<@Nullable T> supplier, Consumer<@Nullable T> onUpdate, @Nullable T defaultValue, boolean forceUpdate) {
        super(name);
        this.supplier = supplier;
        this.onUpdate = onUpdate;
        this.defaultValue = defaultValue;
        this.forceUpdate = forceUpdate;
        this.value = supplier.get();

        inlineContainer.addEventListener(UIEvents.DRAG_PERFORM, this::onDragPerform);
        inlineContainer.addEventListener(UIEvents.DRAG_ENTER, this::onDragEnter, true);
        inlineContainer.addEventListener(UIEvents.DRAG_LEAVE, this::onDragLeave, true);

        if (defaultValue != null) {
            setPastable(defaultValue.getClass(), pasted -> {
                if (pasted != null) {
                    onPaste((T) pasted);
                }
            });
        }
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
    protected void onValueUpdatePassively(@Nullable T newValue) {
        this.value = newValue;
    }

    /**
     * update value actively.
     */
    protected void updateValueActively(@Nullable T newValue) {
        this.value = newValue;
        updateValue();
    }

    /**
     * Set value.
     */
    protected void setValue(T value, boolean notify) {
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
    protected boolean canDropObject(@Nullable Object object) {
        return canDropPredicate.test(object);
    }

    protected void onDropObject(@Nullable Object object) {
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
