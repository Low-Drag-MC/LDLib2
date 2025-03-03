package com.lowdragmc.lowdraglib.gui.editor.runtime;

import com.lowdragmc.lowdraglib.gui.editor.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ConfiguratorParser
 */
public class ConfiguratorParser {

    public static void createConfigurators(ConfiguratorGroup father, Map<String, Method> setters, Class<?> clazz, Object object) {
        if (clazz == Object.class || clazz == null) return;

        for (Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(ConfigSetter.class)) {
                ConfigSetter configSetter = method.getAnnotation(ConfigSetter.class);
                String name = configSetter.field();
                if (!setters.containsKey(name)) {
                    setters.put(name, method);
                }
            }
        }

        createConfigurators(father, setters, clazz.getSuperclass(), object);
        if (clazz.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = clazz.getAnnotation(Configurable.class);
            String name = configurable.showName() ? (configurable.name().isEmpty() ? clazz.getSimpleName() : configurable.name()) : "";
            ConfiguratorGroup newGroup = new ConfiguratorGroup(name, configurable.collapse());
            newGroup.setCanCollapse(configurable.canCollapse());
            newGroup.setTips(configurable.tips());
            father.addConfigurators(newGroup);
            father = newGroup;
        }

        for (var field : clazz.getDeclaredFields()) {
            createFieldConfigurator(field, father, clazz, setters, object);
        }
    }

    public static void createFieldConfigurator(Field field, ConfiguratorGroup father, Class<?> clazz, Map<String, Method> setters, Object object) {
        if (Modifier.isStatic(field.getModifiers())) {
            return;
        }
        if (field.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = field.getAnnotation(Configurable.class);
            // sub configurable
            if (configurable.subConfigurable()) {
                var rawClass = ReflectionUtils.getRawType(field.getGenericType());
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
                        ConfiguratorGroup newGroup = new ConfiguratorGroup(name, configurable.collapse());
                        newGroup.setCanCollapse(configurable.canCollapse());
                        newGroup.setTips(configurable.tips());
                        if (value instanceof IConfigurable subConfigurable) {
                            subConfigurable.buildConfigurator(newGroup);
                        } else {
                            createConfigurators(newGroup, new HashMap<>(), rawClass, value);
                        }
                        father.addConfigurators(newGroup);
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                IConfiguratorAccessor accessor = ConfiguratorAccessors.findByType(field.getGenericType());
                field.setAccessible(true);
                String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
                Method setter = setters.get(field.getName());

                Configurator configurator = accessor.create(name, () -> {
                    try {
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }, value -> {
                    try {
                        if (setter == null) {
                            field.set(object, value);
                        } else {
                            setter.invoke(object, value);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, configurable.forceUpdate(), field);

                configurator.setTips(configurable.tips());
                father.addConfigurators(configurator);
            }
        }
    }

}
