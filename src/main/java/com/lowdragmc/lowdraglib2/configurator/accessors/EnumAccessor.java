package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberAccessor
 */
@LDLRegisterClient(name = "enum", registry = "ldlib2:configurator_accessor")
@SuppressWarnings({"rawtypes"})
public class EnumAccessor implements IConfiguratorAccessor<Enum> {

    @Override
    public boolean test(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public Enum defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (type == null) return null;
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            String name = field.getAnnotation(DefaultValue.class).stringValue()[0];
            for (var value : type.getEnumConstants()) {
                String enumName = getEnumName((Enum) value);
                if (enumName.equals(name)) {
                    return (Enum) value;
                }
            }
        }
        return (Enum) type.getEnumConstants()[0];
    }

    @Override
    public Configurator create(String name, Supplier<Enum> supplier, Consumer<Enum> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        if (field == null) return IConfiguratorAccessor.super.create(name, supplier, consumer, forceUpdate, field, owner);
        var type = ReflectionUtils.getRawType(field.getGenericType());
        if (type.isEnum()) {
            Stream<Enum> candidates = Arrays.stream(type.getEnumConstants()).map(Enum.class::cast);
            ConfigSelector configSelector = null;
            SelectorConfigurator<Enum> selector = null;
            var defaultValue = defaultValue(field, type);
            if (field.isAnnotationPresent(ConfigSelector.class)) {
                configSelector = field.getAnnotation(ConfigSelector.class);
                var candidate = configSelector.candidate();
                if (candidate.length > 0) {
                    candidates = candidates.filter(e -> ArrayUtils.contains(candidate, getEnumName(e)));
                }
                if (!configSelector.subConfiguratorBuilder().isEmpty()) {
                    Method builderMethod = null;
                    try {
                        for (Method m : field.getDeclaringClass().getDeclaredMethods()) {
                            if (!m.getName().equals(configSelector.subConfiguratorBuilder())) continue;
                            if (m.getParameterCount() != 2) continue;
                            if (Enum.class.isAssignableFrom(m.getParameterTypes()[0]) && ConfiguratorGroup.class == m.getParameterTypes()[1]) {
                                builderMethod = m;
                                break;
                            }
                        }
                        if (builderMethod == null) throw new NoSuchMethodException();
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                    builderMethod.setAccessible(true);
                    Method finalBuilderMethod = builderMethod;
                    selector = new ConfiguratorSelectorConfigurator<>(name, supplier, consumer, defaultValue,
                            forceUpdate, candidates.toList(), EnumAccessor::getEnumName,
                            (value, group) -> {
                                try {
                                    finalBuilderMethod.invoke(owner, value, group);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
            if (selector == null) {
                selector = new SelectorConfigurator<>(name, supplier, consumer, defaultValue, forceUpdate, candidates.toList(), EnumAccessor::getEnumName);
                selector.setCopiable(value -> value);
            }
            if (configSelector != null) {
                final var maxItems = configSelector.max();
                selector.selector.selectorStyle(style -> style.maxItemCount(maxItems));
            }
            return selector;
        }
        return IConfiguratorAccessor.super.create(name, supplier, consumer, forceUpdate, field, owner);
    }

    public static String getEnumName(Enum enumValue) {
        if (enumValue == null) return "null";
        if (enumValue instanceof StringRepresentable provider) {
            return provider.getSerializedName();
        } else {
            return enumValue.name();
        }
    }

    public static <T extends Enum<T>> SelectorConfigurator<T> create(String name, List<T> candidates, Supplier<T> supplier, Consumer<T> consumer, T defaultValue, boolean forceUpdate) {
        var selector = new SelectorConfigurator<>(name, supplier, consumer, defaultValue, forceUpdate, candidates, EnumAccessor::getEnumName);
        selector.setCopiable(value -> value);
        return selector;
    }

    public static <T extends Enum<T>> ToggleSelectorConfigurator<T> create(String name, List<T> candidates, Supplier<T> supplier, Consumer<T> consumer, T defaultValue, boolean forceUpdate, Function<T, IGuiTexture> iconProvider) {
        var selector = new ToggleSelectorConfigurator<>(name, supplier, consumer, defaultValue, forceUpdate,
                candidates, EnumAccessor::getEnumName, iconProvider);
        selector.setCopiable(value -> value);
        return selector;
    }
}
