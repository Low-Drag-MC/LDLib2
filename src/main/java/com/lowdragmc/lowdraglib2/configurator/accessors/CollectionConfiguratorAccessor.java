package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigList;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote ArrayConfiguratorAccessor
 */
@AllArgsConstructor
public class CollectionConfiguratorAccessor implements IConfiguratorAccessor<Collection> {
    private final Class<?> baseType;
    private final Class<?> childType;
    private final IConfiguratorAccessor childAccessor;

    @Override
    public boolean test(Class<?> type) {
        return Collection.class.isAssignableFrom(type);
    }

    @Override
    public Collection defaultValue(Field field, Class<?> type) {
        if (type == List.class) {
            return new ArrayList<>();
        } else if (type == Set.class) {
            return new HashSet<>();
        }
        return new ArrayList<>();
    }

    @Override
    public Configurator create(String name, Supplier<Collection> supplier, Consumer<Collection> consumer, boolean forceUpdate, Field field, Object owner) {
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
            var collection = supplier.get();
            if (collection == null) {
                collection = defaultValue(field, baseType);
            }
            ArrayList<Object> objectList = new ArrayList<>(collection);
            return objectList;
        }, provider, forceUpdate);

        arrayGroup.setAddDefault(addDefault);

        arrayGroup.setOnUpdate(list -> consumer.accept(updateCollection(supplier.get(), list)));
        arrayGroup.setCanCollapse(canCollapse);
        if (configList != null) {
            arrayGroup.setCanAdd(configList.canAdd());
            arrayGroup.setCanRemove(configList.canRemove());
            arrayGroup.setCanReorder(configList.canReorder());
        }
        return arrayGroup;
    }

    public Collection updateCollection(Collection base, List<Object> objectList) {
        base.clear();
        base.addAll(objectList);
        return base;
    }
}
