package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigHeader;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.HeaderConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ConfiguratorParser
 */
@UtilityClass
public final class ConfiguratorParser {

    public static void createConfigurators(ConfiguratorGroup father, Object object) {
        createConfigurators(father, object, true);
    }

    public static void createConfigurators(ConfiguratorGroup father, Object object, boolean recursive) {
        createConfigurators(father, new HashMap<>(), object.getClass(), object, recursive);
    }

    public static void createConfigurators(ConfiguratorGroup father, Map<String, Method> setters, Class<?> clazz, Object object) {
        createConfigurators(father, setters, clazz, object, true);
    }

    /**
     * This method is used to create configurators for the given object fields with  {@link Configurable} annotation under the given father group.
     */
    public static void createConfigurators(ConfiguratorGroup father, Map<String, Method> setters, Class<?> clazz, Object object, boolean recursive) {
        if (clazz == Object.class || clazz == null) return;

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ConfigSetter.class)) {
                ConfigSetter configSetter = method.getAnnotation(ConfigSetter.class);
                String name = configSetter.field();
                if (!setters.containsKey(name)) {
                    method.setAccessible(true);
                    setters.put(name, method);
                }
            }
        }

        if (recursive) {
            createConfigurators(father, setters, clazz.getSuperclass(), object);
        }
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

    /**
     * This method is used to create configurators for the given field with {@link Configurable} annotation under the given father group.
     */
    public static void createFieldConfigurator(Field field, ConfiguratorGroup father, Class<?> clazz, Map<String, Method> setters, Object object) {
        if (Modifier.isStatic(field.getModifiers())) {
            return;
        }
        if (field.isAnnotationPresent(ConfigHeader.class)) {
            ConfigHeader configHeader = field.getAnnotation(ConfigHeader.class);
            father.addConfigurator(new HeaderConfigurator(configHeader.value(), configHeader.topMargin()));
        }
        if (field.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = field.getAnnotation(Configurable.class);
            // sub configurable
            if (configurable.subConfigurable()) {
                var rawClass = ReflectionUtils.getRawType(field.getGenericType());
                var flatten = configurable.subFlattenConfigurable();
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        ConfiguratorGroup group;

                        if (flatten) {
                            group = father;
                        } else {
                            String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
                            ConfiguratorGroup newGroup = new ConfiguratorGroup(name, configurable.collapse());
                            newGroup.setCanCollapse(configurable.canCollapse());
                            newGroup.setTips(configurable.tips());
                            group = newGroup;
                        }

                        if (value instanceof IConfigurable subConfigurable) {
                            subConfigurable.buildConfigurator(group);
                        } else {
                            createConfigurators(group, new HashMap<>(), rawClass, value);
                        }

                        if (!flatten) {
                            father.addConfigurators(group);
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                field.setAccessible(true);
                String name = configurable.showName() ? (configurable.name().isEmpty() ? field.getName() : configurable.name()) : "";
                Method setterMethod = setters.get(field.getName());
                Supplier getter = () -> {
                    try {
                        return field.get(object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                };
                Consumer setter = value -> {
                    try {
                        if (setterMethod == null) {
                            field.set(object, value);
                        } else {
                            setterMethod.invoke(object, value);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

                // if is annotated with @ConfigSearch
                if (field.isAnnotationPresent(ConfigSearch.class)) {
                    ConfigSearch configSearch = field.getAnnotation(ConfigSearch.class);
                    try {
                        var searchConfiguratorMethod = clazz.getDeclaredMethod(configSearch.searchConfiguratorMethod());
                        if (searchConfiguratorMethod.getReturnType() != SearchComponentConfigurator.ISearchConfigurator.class) {
                            LDLib2.LOGGER.error("The return type of the search configurator method {} for field {} with @ConfigSearch in class {} is not SearchComponentConfigurator.ISearchConfigurator", searchConfiguratorMethod.getName(), field.getName(), clazz.getName());
                            return;
                        }
                        searchConfiguratorMethod.setAccessible(true);
                        var searchConfigurator = (SearchComponentConfigurator.ISearchConfigurator) searchConfiguratorMethod.invoke(object);
                        father.addConfigurators(new SearchComponentConfigurator<>(name, getter, setter, searchConfigurator, configurable.forceUpdate()));
                    } catch (Exception e) {
                        LDLib2.LOGGER.error("Error while creating search component configurator for field {} with @ConfigSearch in class {}", field.getName(), clazz.getName(), e);
                    }
                    return;
                }

                // try to find accessor based on type
                IConfiguratorAccessor accessor = ConfiguratorAccessors.findByType(field.getGenericType());
                var configurator = accessor.create(name, getter, setter, configurable.forceUpdate(), field, object).setTips(configurable.tips());
                father.addConfigurators(configurator);
            }
        }
    }

}
