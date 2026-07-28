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

    /**
     * Gets the tooltips that were explicitly authored for this option, without the generated fallback
     * {@link #getTooltips()} may provide (e.g. {@code "name (type)"}).
     *
     * <p>This is what {@link #buildConfigurator(com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup)}
     * surfaces as the configurator's tip icon, so options/ports without an authored tooltip don't end up
     * with a help icon repeating what the row already displays.</p>
     */
    default Tooltips getCustomTooltips() {
        return Tooltips.empty();
    }

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
