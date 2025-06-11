package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import net.minecraft.util.StringRepresentable;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberAccessor
 */
@LDLRegisterClient(name = "enum", registry = "ldlib2:configurator_accessor")
public class EnumAccessor implements IConfiguratorAccessor<Enum> {

    @Override
    public boolean test(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public Enum defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
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
    public Configurator create(String name, Supplier<Enum> supplier, Consumer<Enum> consumer, boolean forceUpdate, Field field, Object owner) {
        var type = ReflectionUtils.getRawType(field.getGenericType());
        if (type.isEnum()) {
            Stream<Enum> candidates = Arrays.stream(type.getEnumConstants()).map(Enum.class::cast);
            ConfigSelector configSelector = null;
            if (field.isAnnotationPresent(ConfigSelector.class)) {
                configSelector = field.getAnnotation(ConfigSelector.class);
                var candidate = configSelector.candidate();
                if (candidate.length > 0) {
                    candidates = candidates.filter(e -> ArrayUtils.contains(candidate, getEnumName(e)));
                }
            }
            var selector = new SelectorConfigurator<>(name, supplier, consumer, defaultValue(field, type), forceUpdate, candidates.toList(), EnumAccessor::getEnumName);
            if (configSelector != null) {
                final var maxItems = configSelector.max();
                selector.selector.selectorStyle(style -> style.maxItemCount(maxItems));
            }
            return selector;
        }
        return IConfiguratorAccessor.super.create(name, supplier, consumer, forceUpdate, field, owner);
    }

    public static String getEnumName(Enum enumValue) {
        if (enumValue instanceof StringRepresentable provider) {
            return provider.getSerializedName();
        } else {
            return enumValue.name();
        }
    }
}
