package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.editor.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurable;
import lombok.experimental.UtilityClass;

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
@UtilityClass
public final class ConfiguratorParser {

    public static void createConfigurators(ConfiguratorGroup father, Object object) {
//        createConfigurators(father, new HashMap<>(), object.getClass(), object);
    }

    /**
     * This method is used to create configurators for the given object fields with  {@link Configurable} annotation under the given father group.
     */
    public static void createConfigurators(ConfiguratorGroup father, Map<String, Method> setters, Class<?> clazz, Object object) {
//        if (clazz == Object.class || clazz == null) return;
//
//        for (Method method : clazz.getMethods()) {
//            if (method.isAnnotationPresent(ConfigSetter.class)) {
//                ConfigSetter configSetter = method.getAnnotation(ConfigSetter.class);
//                String name = configSetter.field();
//                if (!setters.containsKey(name)) {
//                    setters.put(name, method);
//                }
//            }
//        }
//
//        createConfigurators(father, setters, clazz.getSuperclass(), object);
//        if (clazz.isAnnotationPresent(Configurable.class)) {
//            Configurable configurable = clazz.getAnnotation(Configurable.class);
//            String name = configurable.showName() ? (configurable.name().isEmpty() ? clazz.getSimpleName() : configurable.name()) : "";
//            ConfiguratorGroup newGroup = new ConfiguratorGroup(name, configurable.collapse());
//            newGroup.setCanCollapse(configurable.canCollapse());
//            newGroup.setTips(configurable.tips());
//            father.addConfigurators(newGroup);
//            father = newGroup;
//        }
//
//        for (var field : clazz.getDeclaredFields()) {
//            createFieldConfigurator(field, father, clazz, setters, object);
//        }
    }

    /**
     * This method is used to create configurators for the given field with {@link Configurable} annotation under the given father group.
     */
    public static void createFieldConfigurator(Field field, ConfiguratorGroup father, Class<?> clazz, Map<String, Method> setters, Object object) {
//        if (Modifier.isStatic(field.getModifiers())) {
//            return;
//        }
//        if (field.isAnnotationPresent(Configurable.class)) {
//            Configurable configurable = field.getAnnotation(Configurable.class);
//            // sub configurable
//            if (configurable.subConfigurable()) {
//                var rawClass = ReflectionUtils.getRawType(field.getGenericType());
//                try {
//                    field.setAccessible(true);
//                    var value = field.get(object);
//                    if (value != null) {
//                        String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
//                        ConfiguratorGroup newGroup = new ConfiguratorGroup(name, configurable.collapse());
//                        newGroup.setCanCollapse(configurable.canCollapse());
//                        newGroup.setTips(configurable.tips());
//                        if (value instanceof IConfigurable subConfigurable) {
//                            subConfigurable.buildConfigurator(newGroup);
//                        } else {
//                            createConfigurators(newGroup, new HashMap<>(), rawClass, value);
//                        }
//                        father.addConfigurators(newGroup);
//                    }
//                } catch (IllegalAccessException ignored) {}
//            } else {
//                IConfiguratorAccessor accessor = ConfiguratorAccessors.findByType(field.getGenericType());
//                field.setAccessible(true);
//                String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
//                Method setter = setters.get(field.getName());
//
//                Configurator configurator = accessor.create(name, () -> {
//                    try {
//                        return field.get(object);
//                    } catch (IllegalAccessException e) {
//                        throw new RuntimeException(e);
//                    }
//                }, value -> {
//                    try {
//                        if (setter == null) {
//                            field.set(object, value);
//                        } else {
//                            setter.invoke(object, value);
//                        }
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }, configurable.forceUpdate(), field);
//
//                configurator.setTips(configurable.tips());
//                father.addConfigurators(configurator);
//            }
//        }
    }

}
