package com.lowdragmc.lowdraglib2.nodegraphtookit.api;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;

import java.lang.reflect.Field;

public interface IFieldValueConfigurable extends IConfigurable {
    /**
     * Sets the value of this option.
     *
     * @param value the value to set
     */
    void setValue(Object value);

    /**
     * Retrieves the value of the current configuration.
     * The return type is generic, allowing flexibility for different implementations.
     *
     * @param <T> the expected type of the value
     * @return the current value, or {@code null} if no value is set
     */
    <T> T getValue();

    /**
     * Gets the default value of the option.
     *
     * @return the default value
     */
    <T> T getDefaultValue();

    /**
     * Gets the tooltips for this option.
     */
    Tooltips getTooltips();

    default void notifyValueChanged() {

    }

    /**
     * Indicates whether the option should only be displayed in the inspector view.
     *
     * @return {@code true} if the option is intended to be shown exclusively in the inspector;
     *         {@code false} otherwise.
     */
    default boolean isShowInInspectorOnly() {
        return false;
    }

    default Field getValueField() {
        return null;
    }

    default Object getValueOwer() {
        return null;
    }

    default boolean forceUpdate() {
        return true;
    }
}
