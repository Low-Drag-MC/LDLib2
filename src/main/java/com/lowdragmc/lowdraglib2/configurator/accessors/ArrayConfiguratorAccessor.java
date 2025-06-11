package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import lombok.AllArgsConstructor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ArrayConfiguratorAccessor
 */
@AllArgsConstructor
public class ArrayConfiguratorAccessor implements IConfiguratorAccessor<Object> {
    private final Class<?> childType;
    private final IConfiguratorAccessor childAccessor;

    @Override
    public boolean test(Class<?> type) {
        return type.isArray();
    }

    @Override
    public Object defaultValue(Field field, Class<?> type) {
        return Array.newInstance(childType, 0);
    }

    @Override
    public Configurator create(String name, Supplier supplier, Consumer consumer, boolean forceUpdate, Field field, Object owner) {
        boolean isCollapse = true;
        boolean canCollapse = true;
        if (field.isAnnotationPresent(Configurable.class)) {
            isCollapse = field.getAnnotation(Configurable.class).collapse();
            canCollapse = field.getAnnotation(Configurable.class).canCollapse();
        }

        ArrayConfiguratorGroup.IConfiguratorProvider<Object> provider = (getter, setter) -> childAccessor.create("", getter, setter, forceUpdate, field, owner);
        ArrayConfiguratorGroup.IAddDefault<Object> addDefault = () -> childAccessor.defaultValue(field, childType);

        ConfigList configList = field.isAnnotationPresent(ConfigList.class) ? field.getAnnotation(ConfigList.class) : null;
        if (configList != null) {
            if (!configList.configuratorMethod().isEmpty()) {
                var declaringClass = field.getDeclaringClass();
                try {
                    var customCreator = declaringClass.getDeclaredMethod(configList.configuratorMethod(), Supplier.class, Consumer.class);
                    customCreator.setAccessible(true);
                    provider = (getter, setter) -> {
                        try {
                            return (Configurator) customCreator.invoke(owner, getter, setter);
                        } catch (Exception e) {
                            return childAccessor.create("", getter, setter, forceUpdate, field, owner);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    LDLib2.LOGGER.error("Could not find method {} in class {} while using @ConfigList for a field {}", configList.configuratorMethod(), declaringClass.getName(), field);
                }
            }
            if (!configList.addDefaultMethod().isEmpty()) {
                var declaringClass = field.getDeclaringClass();
                try {
                    var customAddDefault = declaringClass.getDeclaredMethod(configList.addDefaultMethod());
                    customAddDefault.setAccessible(true);
                    addDefault = () -> {
                        try {
                            return customAddDefault.invoke(owner);
                        } catch (Exception e) {
                            return childAccessor.defaultValue(field, childType);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    LDLib2.LOGGER.error("Could not find method {} in class {} while using @ConfigList for a field {}", configList.addDefaultMethod(), declaringClass.getName(), field);
                }
            }
        }

        var arrayGroup = new ArrayConfiguratorGroup<>(name, isCollapse, () -> {
            Object array = supplier.get();
            if (array == null) {
                array = defaultValue(field, Object.class);
            }
            int length = Array.getLength(array);

            List<Object> objectList = new ArrayList<>();

            for (int i = 0; i < length; i++) {
                objectList.add(Array.get(array, i));
            }

            return objectList;
        }, provider, forceUpdate);

        arrayGroup.setAddDefault(addDefault);
        arrayGroup.setOnUpdate(list -> consumer.accept(toArray(list)));
        arrayGroup.setCanCollapse(canCollapse);
        if (configList != null) {
            arrayGroup.setCanAdd(configList.canAdd());
            arrayGroup.setCanRemove(configList.canRemove());
            arrayGroup.setCanReorder(configList.canReorder());
        }
        return arrayGroup;
    }

    public Object toArray(List<Object> objectList) {
        Object array = Array.newInstance(childType, objectList.size());
        for (int i = 0; i < objectList.size(); i++) {
            Array.set(array, i, objectList.get(i));
        }
        return array;
    }
}
