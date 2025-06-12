package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote NumberAccessor
 */
@LDLRegisterClient(name = "string", registry = "ldlib2:configurator_accessor")
public class StringAccessor extends TypesAccessor<String> {

    public StringAccessor() {
        super(String.class);
    }

    @Override
    public String defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return field.getAnnotation(DefaultValue.class).stringValue()[0];
        }
        return "";
    }

    @Override
    public Configurator create(String name, Supplier<String> supplier, Consumer<String> consumer, boolean forceUpdate, Field field, Object owner) {
        var defaultValue = defaultValue(field, String.class);
        if (field.isAnnotationPresent(ConfigSelector.class)) {
            var configSelector = field.getAnnotation(ConfigSelector.class);
            var maxItems = configSelector.max();
            SelectorConfigurator<String> selector;
            if (!configSelector.subConfiguratorBuilder().isEmpty()) {
                Method builderMethod;
                try {
                    builderMethod = field.getDeclaringClass().getDeclaredMethod(configSelector.subConfiguratorBuilder(), String.class, ConfiguratorGroup.class);
                    builderMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
                selector = new ConfiguratorSelectorConfigurator<>(name, supplier, consumer, defaultValue, forceUpdate,
                        Arrays.stream(configSelector.candidate()).toList(), s -> s, (value, group) -> {
                    try {
                        builderMethod.invoke(owner, value, group);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                selector = new SelectorConfigurator<>(name, supplier, consumer, defaultValue, forceUpdate,
                        Arrays.stream(configSelector.candidate()).toList(), s -> s);
            }
            selector.selector.selectorStyle(style -> style.maxItemCount(maxItems));
            return selector;
        }
        return new StringConfigurator(name, supplier, consumer, defaultValue, forceUpdate);
    }
}
