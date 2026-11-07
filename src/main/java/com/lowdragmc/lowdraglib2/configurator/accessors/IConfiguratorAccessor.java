package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote IConfiguratorAccessor
 */
public interface IConfiguratorAccessor<T> extends ILDLRegister<IConfiguratorAccessor<?>, IConfiguratorAccessor<?>> {
    IConfiguratorAccessor<?> DEFAULT = type -> true;

    /**
     * @param type the class type
     * @return true if this accessor can handle the given type
     */
    boolean test(Class<?> type);

    /**
     * Retrieves the default value for a given field and type. Make sure the return value is a new instance, which will also be used by {@link ArrayConfiguratorAccessor}
     *
     * @param field the field for which the default value is being retrieved
     * @param type the class type of the field
     * @return the default value of the specified type, or null if no default value is specified
     */
    default T defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        return null;
    }

    default T defaultValue(@Nullable Field field) {
        return defaultValue(field, field == null ? null : field.getType());
    }

    /**
     * @param name the name of the configurator
     * @param supplier the supplier for the value
     * @param consumer the consumer for the value
     * @param forceUpdate whether to force update the configurator
     * @param field the field to be configured
     * @param owner the field owner
     * @return a new configurator instance
     */
    default Configurator create(String name, Supplier<T> supplier, Consumer<T> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return new Configurator(name);
    }

    /**
     * The same, told the value's TYPE as well — for a value that has no Java field behind it.
     *
     * <p>⚠️ A graph variable declared at run time is such a value: there is no {@code Field} to
     * reflect on, so an accessor that needs more than the current value to build its editor has
     * nothing to go on. {@code EnumAccessor} is the one that does — an enum's constants can only be
     * listed from the enum class, and the value starts null — and without this it fell back to the
     * bare row above: a label with nothing beside it. A caller that knows the declared type should
     * prefer this overload; the default keeps every existing one behaving as before.
     *
     * @param type the declared type of the value, when the caller knows it; {@code null} otherwise
     */
    default Configurator create(String name, @Nullable Class<?> type, Supplier<T> supplier, Consumer<T> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        return create(name, supplier, consumer, forceUpdate, field, owner);
    }
}
