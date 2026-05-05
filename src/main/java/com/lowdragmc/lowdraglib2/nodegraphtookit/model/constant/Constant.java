package com.lowdragmc.lowdraglib2.nodegraphtookit.model.constant;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.TypeHandle;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.GraphElementModel;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.utils.TypeUtils;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a constant value embedded in a port or node option.
 *
 * <p>Constants store typed values that can be edited in the graph UI and used as default values
 * for input ports when they are not connected.</p>
 */
public abstract class Constant {
    @Getter @Setter
    protected GraphElementModel owner;
    @Getter
    protected @Nullable TypeHandle typeHandle;
    protected final List<Consumer<Object>> listeners = new ArrayList<>();

    /**
     * Creates an empty constant.
     */
    public Constant() {
    }

    public void init(TypeHandle typeHandle) {
        this.typeHandle = typeHandle;
        if (typeHandle != null) {
            setDefaultValue(typeHandle.getDefaultValue());
        }
        setValue(getDefaultValue());
    }

    public ISubscription addListener(Consumer<Object> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public void notifyListeners() {
        for (Consumer<Object> listener : listeners) {
            listener.accept(getValue());
        }
    }

    public abstract Object getValue();

    public abstract void setValue(Object value);

    public abstract Object getDefaultValue();

    public abstract void setDefaultValue(Object defaultValue);

    public abstract Type getType();

    public abstract Constant copy();

    public boolean isAssignableFrom(Type type) {
        return TypeUtils.isAssignableFrom(getType(), type);
    }

    /**
     * Attempts to retrieve the value of the constant and cast it to the specified type.
     * If the value is {@code null} but the type of the constant is compatible with the expected type,
     * returns a successful result containing {@code null}.
     * If the value is not compatible with the expected type, returns an error result.
     *
     * @param <T> the type of the value to retrieve
     * @param expectedType the class of the type to which the value should be cast
     * @return a {@link DataResult} containing the cast value if successful, or an error if the type is mismatched
     */
    public <T> DataResult<T> tryGetValue(Type expectedType) {
        var value = getValue();
        var type = getType();
        if (value == null && type instanceof Class<?> clazz && TypeUtils.isAssignableFrom(expectedType, clazz)) {
            return DataResult.success(null);
        }
        if (value != null && TypeUtils.isAssignableFrom(expectedType, value.getClass())) {
            return DataResult.success((T) value);
        }
        return DataResult.error(() -> "Type mismatch: " + value + " is not assignable to " + expectedType);
    }

    /**
     * Attempts to set a new value for the constant. This method checks whether the provided value
     * is compatible with the type of the constant and assigns the value if it is valid.
     *
     * @param <T> the type of the value to set
     * @param value the new value to set for the constant; can be {@code null} if the type of the
     *              constant allows {@code null} or is not primitive
     * @return {@code true} if the value is successfully set; {@code false} if the value is incompatible
     *         with the type of the constant or if {@code null} is provided but the type is primitive
     */
    public <T> boolean trySetValue(T value) {
        var currentValue = getValue();
        var type = getType();
        if (value == null && type instanceof Class<?> clazz && clazz.isPrimitive()) {
            return false;
        }
        if (value != null && !isAssignableFrom(value.getClass())) {
            return false;
        }
        setValue(value);
        return true;
    }
}
