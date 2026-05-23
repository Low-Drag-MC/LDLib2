package com.lowdragmc.lowdraglib2.nodegraphtookit.api.node;

import com.lowdragmc.lowdraglib2.gui.ui.data.Tooltips;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.type.ITypeConfigurable;
import com.lowdragmc.lowdraglib2.nodegraphtookit.model.node.definition.IOptionDefinitionContext;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;


public interface IOptionBuilder<T extends IOptionBuilder<T>> {

    /**
     * Builds and returns the final {@link INodeOption} instance based on the current configuration of the builder.
     *
     * <p>This method is optional. All options are automatically built when the node's
     * {@link Node#onDefineOptions(IOptionDefinitionContext)} method completes.</p>
     *
     * <p>Calling this method releases the memory associated with this option back into the pool immediately.
     * You can choose to call this method if there are lots of options being defined to reduce peak memory usage.</p>
     *
     * <p>Only call this after setting all desired configuration options using the builder methods.</p>
     *
     * @return the constructed {@link INodeOption}
     */
    INodeOption build();

    /**
     * Configures the display name of the option being built.
     *
     * <p>The display name doesn't affect functionality; it can improve usability and readability.
     * If not set explicitly using this method, the name passed during creation
     * (calling {@link Node#onDefineOptions(IOptionDefinitionContext)}) is used as the default display name.</p>
     *
     * @param displayName the display name to assign to the option
     * @return the current builder instance for method chaining
     */
    T withDisplayName(Component displayName);

    /**
     * Configures the tooltip text for the option being built.
     *
     * @param tooltip the tooltip text to assign to the option
     * @return the current builder instance for method chaining
     */
    T withTooltips(Tooltips tooltips);

    /**
     * Configures the default value for the option being built.
     *
     * @param defaultValue the default value to assign to the option
     * @return the current builder instance for method chaining
     */
    T withDefaultValue(Object defaultValue);

    /**
     * Configures the option to be shown only in the inspector, not in the node header.
     *
     * @return the current builder instance for method chaining
     */
    T showInInspectorOnly();

    /**
     * Overrides the configurator used to edit this option's value. By default the configurator
     * is resolved from the option's {@code TypeHandle}; setting this lets two options that
     * share a {@code TypeHandle} present different UIs.
     *
     * @param configurable the per-option configurator override
     * @return the current builder instance for method chaining
     */
    T withConfigurable(ITypeConfigurable configurable);

    /**
     * Supplies the {@link Field} + owner used by the default configurator accessor for
     * annotation lookup (e.g. {@code @ConfigNumber} for value ranges). Use this when you want
     * annotated-field behavior on the option without registering a new
     * {@code ITypeConfigurable} against its type.
     *
     * @param field reflection field that mirrors this option's value
     * @param owner instance that owns {@code field}
     * @return the current builder instance for method chaining
     */
    T withFieldContext(Field field, Object owner);

    /**
     * Installs a Mojang {@link Codec} as the serializer for this option's embedded constant.
     * Wins over the default {@code AccessorRegistries} lookup — useful when the value type has
     * no registered accessor, or when you want a different on-disk shape.
     *
     * <p>Ignored if {@link #withoutSerialization()} is also set.</p>
     *
     * @param codec the codec used to encode/decode the option's value and default value
     * @return the current builder instance for method chaining
     */
    T withCodec(Codec<?> codec);

    /**
     * Marks the option as non-persistent. The option's type identifier still survives the
     * round-trip but its value and default value are skipped during serialization. Use for
     * runtime-computed values that have no meaningful saved state.
     *
     * @return the current builder instance for method chaining
     */
    T withoutSerialization();

    /**
     * Suppresses the inspector field for this option. {@code buildConfigurator} becomes a no-op
     * so the option produces no UI row, even though its value/default still live on the model.
     *
     * @return the current builder instance for method chaining
     */
    T withoutConfigurator();
}