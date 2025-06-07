package com.lowdragmc.lowdraglib2.configurator.accessors;

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
    public Configurator create(String name, Supplier<Collection> supplier, Consumer<Collection> consumer, boolean forceUpdate, Field field) {
        boolean isCollapse = true;
        boolean canCollapse = true;

        if (field.isAnnotationPresent(Configurable.class)) {
            isCollapse = field.getAnnotation(Configurable.class).collapse();
            canCollapse = field.getAnnotation(Configurable.class).canCollapse();
        }

        var arrayGroup = new ArrayConfiguratorGroup<>(name, isCollapse, () -> {
            var collection = supplier.get();
            if (collection == null) {
                collection = defaultValue(field, baseType);
            }
            ArrayList<Object> objectList = new ArrayList<>(collection);
            return objectList;
        }, (getter, setter) -> childAccessor.create("", getter, setter, forceUpdate, field), forceUpdate);

        arrayGroup.setAddDefault(() -> childAccessor.defaultValue(field, childType));

        arrayGroup.setOnUpdate(list -> consumer.accept(updateCollection(supplier.get(), list)));
        arrayGroup.setCanCollapse(canCollapse);
        return arrayGroup;
    }

    public Collection updateCollection(Collection base, List<Object> objectList) {
        base.clear();
        base.addAll(objectList);
        return base;
    }
}
