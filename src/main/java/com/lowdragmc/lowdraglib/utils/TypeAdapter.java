package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.graphprocessor.data.UnknownType;
import com.lowdragmc.lowdraglib.graphprocessor.data.trigger.TriggerLink;
import org.joml.Vector3f;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

/**
 * Implement this interface to use the inside your class to define type convertions to use inside the graph.
 * <br>
 * Example:
 * @<code>
 *     public class CustomConvertions : ITypeAdapter {
 *         public Vector4 convertFloatToVector(float from){
 *             return new Vector4(from, from, from, from);
 *         }
 *     }
 * </code>
 */
public class TypeAdapter {
    public interface ITypeAdapter {
        void onRegister();
    }

    public record convertion(Class from, Class to) {};
    static Map<convertion, Function> adapters = Collections.synchronizedMap(new HashMap<>());
    static Set<convertion> incompatibleTypes = Collections.synchronizedSet(new HashSet<>());
    static Map<Class, Integer> typeColorMap = Collections.synchronizedMap(new HashMap<>());
    static Map<Class, String> typeDisplayNameMap = Collections.synchronizedMap(new HashMap<>());
    static Set<Class> castTypes = Collections.synchronizedSet(new HashSet<>());
    static boolean adaptersLoaded = false;

    public static <F, T> void registerAdapter(Class<F> from, Class<T> to, Function<F, T> adapter) {
        adapters.put(new convertion(from, to), adapter);
        registerCastType(from);
        registerCastType(to);
    }

    public static void registerTypeColor(Class<?> clazz, int color) {
        typeColorMap.put(clazz, color);
        registerCastType(clazz);
    }

    public static void registerTypeDisplayName(Class<?> clazz, String name) {
        typeDisplayNameMap.put(clazz, name);
        registerCastType(clazz);
    }

    public static void registerIncompatibleTypes(Class from, Class to) {
        incompatibleTypes.add(new convertion(from, to));
    }

    public static void registerCastType(Class<?> clazz) {
        castTypes.add(clazz);
    }

    public static void loadAllAdapters() {
        adaptersLoaded = true;
        AnnotationDetector.REGISTER_TYPE_ADAPTERS.forEach(instance -> {
            instance.onRegister();
            var clazz = instance.getClass();
            for (var method : clazz.getDeclaredMethods()) {
                // not a static method
                if (!Modifier.isStatic(method.getModifiers())){
                    if (method.getReturnType() != void.class && method.getParameterCount() == 1) {
                        var from = method.getParameterTypes()[0];
                        var to = method.getReturnType();
                        adapters.put(new convertion(from, to), obj -> {
                            try {
                                return method.invoke(instance, obj);
                            } catch (Exception e) {
                                return null;
                            }
                        });
                    }
                }
            }
        });
    }

    public static boolean areIncompatible(Class from, Class to) {
        if (!adaptersLoaded)
            loadAllAdapters();
        return incompatibleTypes.stream().anyMatch(t -> t.from == from && t.to == to);
    }

    public static boolean areConvertable(Class from, Class to) {
        if (!adaptersLoaded)
            loadAllAdapters();
        if (from == to || to == Object.class)
            return true;
        if (areIncompatible(from, to))
            return false;
        return adapters.containsKey(new convertion(from, to));
    }

    public static Object convert(Object from, Class targetType) {
        if (!adaptersLoaded)
            loadAllAdapters();
        var convertion = new convertion(from.getClass(), targetType);
        if (convertion.from == convertion.to || convertion.to == Object.class)
            return from;
        if (adapters.containsKey(convertion))
            return adapters.get(convertion).apply(from);
        return null;
    }

    public static String getTypeDisplayName(Class<?> type) {
        if (!adaptersLoaded)
            loadAllAdapters();
        if (type == TriggerLink.class) {
            return "Trigger";
        } else if (type == float.class || type == int.class || type == Float.class || type == Integer.class) {
            return "Number";
        } else if (type == Object.class) {
            return "Any";
        }
        return typeDisplayNameMap.getOrDefault(type, type.getSimpleName());
    }

    public static int getTypeColor(Class<?> type) {
        if (!adaptersLoaded)
            loadAllAdapters();
        if (type == UnknownType.class) {
            return ColorPattern.generateRainbowColor();
        } else if (type == TriggerLink.class) {
            return ColorPattern.YELLOW.color;
        }else if (type == boolean.class || type == Boolean.class) {
            return ColorPattern.PINK.color;
        } if (type.isPrimitive() || type == Integer.class || type == Float.class) {
            return ColorPattern.LIGHT_BLUE.color;
        } else if (type == String.class) {
            return ColorPattern.BROWN.color;
        } else if (type == Object.class) {
            return ColorPattern.WHITE.color;
        } else if (type == Vector3f.class) {
            return ColorPattern.ORANGE.color;
        }
        return typeColorMap.getOrDefault(type, ColorPattern.BLUE.color);
    }

}
