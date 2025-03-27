package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
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
@ConfigAccessor
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
    public Configurator create(String name, Supplier<Enum> supplier, Consumer<Enum> consumer, boolean forceUpdate, Field field) {
        var type = ReflectionUtils.getRawType(field.getGenericType());
        if (type.isEnum()) {
            Stream<Enum> candidates = Arrays.stream(type.getEnumConstants()).map(Enum.class::cast);
            if (field.isAnnotationPresent(ConfigSelector.class)) {
                ConfigSelector configSelector = field.getAnnotation(ConfigSelector.class);
                var candidate = configSelector.candidate();
                if (candidate.length > 0) {
                    candidates = candidates.filter(e -> ArrayUtils.contains(candidate, getEnumName(e)));
                }
            }
            return new SelectorConfigurator<>(name, supplier, consumer, defaultValue(field, type), forceUpdate, candidates.toList(), EnumAccessor::getEnumName);
        }
        return IConfiguratorAccessor.super.create(name, supplier, consumer, forceUpdate, field);
    }

    public static String getEnumName(Enum enumValue) {
        if (enumValue instanceof StringRepresentable provider) {
            return provider.getSerializedName();
        } else {
            return enumValue.name();
        }
    }
}
