package com.lowdragmc.lowdraglib2.nodegraphtookit.api.port;

import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.mojang.serialization.Codec;

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

    /**
     * Installs a Mojang {@link Codec} as the serializer for this port's embedded constant. Wins
     * over the default {@code AccessorRegistries} lookup — useful when the value type has no
     * registered accessor, or when you want a different on-disk shape.
     *
     * <p>Ignored if {@link #withoutSerialization()} is also set.</p>
     *
     * @param codec the codec used to encode/decode the port's value and default value
     * @return the current builder instance for method chaining
     */
    T withCodec(Codec<?> codec);

    /**
     * Marks the port as non-persistent. The port's type identifier still survives the round-trip
     * (so the port can be reconstructed), but its value and default value are skipped during
     * serialization. Use for runtime-computed values that have no meaningful saved state.
     *
     * @return the current builder instance for method chaining
     */
    T withoutSerialization();

    /**
     * Suppresses the inspector field for this port. {@code buildConfigurator} becomes a no-op so
     * the port produces no UI row, even though its value/default still live on the model. Useful
     * for purely structural ports whose value the user shouldn't edit.
     *
     * @return the current builder instance for method chaining
     */
    T withoutConfigurator();
}
