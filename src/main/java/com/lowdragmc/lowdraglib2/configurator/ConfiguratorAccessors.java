package com.lowdragmc.lowdraglib2.configurator;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;
import com.lowdragmc.lowdraglib2.configurator.accessors.ArrayConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.CollectionConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.utils.ReflectionUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author KilaBash
 * @date 2022/12/1
 * @implNote ConfiguratorAccessors
 */
public class ConfiguratorAccessors {
    private static final Map<Class<?>, IConfiguratorAccessor<?>> ACCESSOR_MAP = new ConcurrentHashMap<>();

    public static IConfiguratorAccessor<?> findByType(Type clazz) {
        if (clazz instanceof GenericArrayType array) {
            var componentType = array.getGenericComponentType();
            var childAccessor = findByType(componentType);
            var rawType = ReflectionUtils.getRawType(componentType);

            return new ArrayConfiguratorAccessor(rawType == null ? Object.class : rawType, childAccessor);
        }

        var rawType = ReflectionUtils.getRawType(clazz);

        if (rawType != null) {
            var accessor = findByClass(rawType);

            if (accessor != IConfiguratorAccessor.DEFAULT) {
                return accessor;
            }

            if (rawType.isArray()) {
                var componentType = rawType.getComponentType();
                var childAccessor = findByType(componentType);
                return new ArrayConfiguratorAccessor(componentType, childAccessor);
            }

            if (Collection.class.isAssignableFrom(rawType)) {
                // A raw collection type — List.class rather than List<String> — carries no element
                // type. That is an ordinary Type and arrives here from anything that describes a
                // field or a port by its class alone, so it cannot be an unchecked cast: it used to
                // throw ClassCastException all the way out of the screen's tick and take the editor
                // down. Object is the only element type a raw collection can honestly claim.
                var componentType = elementTypeOf(clazz);
                var childAccessor = findByType(componentType);
                var rawComponentType = ReflectionUtils.getRawType(componentType);

                return new CollectionConfiguratorAccessor(rawType, rawComponentType == null ? Object.class : rawComponentType, childAccessor);
            }
        }
        return IConfiguratorAccessor.DEFAULT;
    }

    /**
     * The element type of a collection type, or {@link Object} when it does not name one — a raw
     * {@code List.class}, or the pathological parameterised type with no arguments.
     */
    private static Type elementTypeOf(Type collectionType) {
        if (collectionType instanceof ParameterizedType parameterized) {
            var arguments = parameterized.getActualTypeArguments();
            if (arguments.length > 0) return arguments[0];
        }
        return Object.class;
    }

    public static IConfiguratorAccessor<?> findByClass(Class<?> clazz) {
        return ACCESSOR_MAP.computeIfAbsent(clazz, c -> {
            for (var holder : LDLib2ClientRegistries.CONFIGURATOR_ACCESSORS) {
                if (holder.value().test(c)) {
                    return holder.value();
                }
            }
            return IConfiguratorAccessor.DEFAULT;
        });
    }

}
