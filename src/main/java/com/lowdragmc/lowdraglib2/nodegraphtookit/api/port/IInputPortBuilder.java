package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;

import java.lang.reflect.Field;

/**
 * Interface for defining an input port.
 *
 * <p>Use this interface to create an input port before you assign its data type.
 */
public interface IInputPortBuilder<T extends IInputPortBuilder<T>> extends IPortBuilder<T> {

    /**
     * Overrides the configurator used to edit this input port's embedded constant. By default
     * the configurator is resolved from the port's {@code TypeHandle}; setting this lets two
     * ports that share a {@code TypeHandle} present different UIs.
     *
     * @param configurable the per-port configurator override
     * @return the current builder instance for method chaining
     */
    T withConfigurable(ITypeConfigurable configurable);

    /**
     * Supplies the {@link Field} + owner used by the default configurator accessor for
     * annotation lookup (e.g. {@code @ConfigNumber} for ranges). Use this when you want the
     * annotated-field behavior on an option/port instead of registering a new
     * {@code ITypeConfigurable}.
     *
     * @param field reflection field that mirrors this port's value
     * @param owner instance that owns {@code field}
     * @return the current builder instance for method chaining
     */
    T withFieldContext(Field field, Object owner);
}
