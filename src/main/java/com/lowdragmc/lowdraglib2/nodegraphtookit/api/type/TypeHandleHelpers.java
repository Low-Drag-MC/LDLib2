package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@UtilityClass
public final class TypeHandleHelpers {
    // customId -> Type binding
    private static final Map<String, Type> ID_TO_TYPE = new ConcurrentHashMap<>();
    // customId -> (TypeHandle + FriendlyName)
    private static final Map<String, TypeHandleDescriptor> CUSTOM_ID_TO_DESCRIPTOR = new ConcurrentHashMap<>();
    // customId -> ITypeConfigurable
    private static final Map<String, ITypeConfigurable> CUSTOM_ID_TO_CONFIGURABLE = new ConcurrentHashMap<>();
    // customId -> Color
    private static final Map<String, Integer> CUSTOM_ID_TO_COLOR = new ConcurrentHashMap<>();// customId -> Color
    // customId -> ICON
    private static final Map<String, IGuiTexture> CUSTOM_ID_TO_ICON = new ConcurrentHashMap<>();
    // customId -> Default Value Supplier
    private static final Map<String, Supplier<Object>> CUSTOM_ID_TO_DEFAULT_VALUE = new ConcurrentHashMap<>();

    /**
     * GenerateCustomTypeHandle(uniqueId, friendlyName)
     */
    public static TypeHandle customType(String uniqueId, @Nullable String friendlyName) {
        var res = getOrCreateCustomTypeHandle(uniqueId, friendlyName);
        if (!res.isNew) {
            LDLib2.LOGGER.error("{} is already registered in TypeSerializer", uniqueId);
        }
        return res.handle;
    }

    /**
     * GenerateCustomTypeHandle(Type t, uniqueId)
     */
    public static TypeHandle customType(Type t, String uniqueId) {
        return customType(t, uniqueId, null);
    }

    /**
     * GenerateCustomTypeHandle(Type t, uniqueId)
     */
    public static TypeHandle customType(Type t, String uniqueId, @Nullable String friendlyName) {
        var res = getOrCreateCustomTypeHandle(uniqueId, friendlyName);

        if (res.isNew) {
            ID_TO_TYPE.put(uniqueId, t);
        } else {
            Type existing = resolveType(res.handle);
            if (!typeEquals(existing, t)) {
                throw new IllegalArgumentException(
                        "TypeHandle " + uniqueId + " already refers to a different type. " +
                                "existing=" + safeTypeName(existing) + ", new=" + safeTypeName(t)
                );
            }
            LDLib2.LOGGER.error("{} is already registered in TypeSerializer", uniqueId);
        }
        return res.handle;
    }

    /**
     * RebindCustomTypeHandle(typeHandle, newType)
     */
    public static void setCustomTypeHandle(TypeHandle typeHandle, Type t) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            ID_TO_TYPE.put(id, t);
        } else {
            throw new IllegalArgumentException("TypeHandle is not a custom type handle.");
        }
    }

    public static void setCustomConfigurable(TypeHandle typeHandle, ITypeConfigurable typeConfigurable) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            CUSTOM_ID_TO_CONFIGURABLE.put(id, typeConfigurable);
        } else {
            throw new IllegalArgumentException("TypeHandle is not a custom type handle.");
        }
    }

    public static void setCustomColorAndIcon(TypeHandle typeHandle, int color, IGuiTexture icon) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            CUSTOM_ID_TO_COLOR.put(id, color);
            CUSTOM_ID_TO_ICON.put(id, icon);
        }
    }

    public static void setCustomColor(TypeHandle typeHandle, int color) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            CUSTOM_ID_TO_COLOR.put(id, color);
        }
    }

    public static void setCustomIcon(TypeHandle typeHandle, IGuiTexture icon) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            CUSTOM_ID_TO_ICON.put(id, icon);
        }
    }

    public static void setCustomDefaultValue(TypeHandle typeHandle, Supplier<Object> defaultValue) {
        var id = typeHandle.getIdentification();
        if (id != null) {
            CUSTOM_ID_TO_DEFAULT_VALUE.put(id, defaultValue);
        }
    }

    /**
     * GenerateTypeHandle(Type t, friendlyName)
     */
    public static TypeHandle fromType(Type t, @Nullable String friendlyName) {
        t = convertType(t);
        Objects.requireNonNull(t, "t");
        var identification = identificationOf(t);

        if (friendlyName != null && !friendlyName.isEmpty()) {
            TypeHandleDescriptor existing = CUSTOM_ID_TO_DESCRIPTOR.get(identification);
            if (existing != null && existing.friendlyName() != null
                    && !Objects.equals(existing.friendlyName(), friendlyName)) {
                throw new IllegalStateException(
                        "A type with same identification but a different friendly name exists " +
                                existing.friendlyName() + " != " + friendlyName
                );
            }
        }

        TypeHandle th = TypeHandle.create(identification);

        if (friendlyName != null && !friendlyName.isEmpty()) {
            CUSTOM_ID_TO_DESCRIPTOR.put(identification, new TypeHandleDescriptor(th, friendlyName));
        }

        if (!ID_TO_TYPE.containsKey(identification)) {
            ID_TO_TYPE.put(identification, t);
        }
        return th;
    }

    public static TypeHandle fromType(Type type) {
        return fromType(type, null);
    }

    static Type resolveType(TypeHandle th) {
        String id = (th != null) ? th.getIdentification() : null;
        if (id == null) return TypeHandles.Unknown.class;
        return ID_TO_TYPE.computeIfAbsent(id, key -> {
            try {
                return Class.forName(key, false, TypeHandleHelpers.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                return TypeHandles.Unknown.class;
            }
        });
    }

    static ITypeConfigurable resolveConfigurable(TypeHandle th) {
        String id = (th != null) ? th.getIdentification() : null;
        return CUSTOM_ID_TO_CONFIGURABLE.getOrDefault(id, ITypeConfigurable.DEFAULT);
    }

    static int resolveColor(TypeHandle typeHandle) {
        String id = (typeHandle != null) ? typeHandle.getIdentification() : null;
        var color = CUSTOM_ID_TO_COLOR.get(id);
        if (color != null) return color;
        if (id == null) return -1;
        // hash color
        var t = (fnv1a32(id) & 0xffffffffL) / (double) 0x1_0000_0000L;
        var rgb = ColorUtils.hslToRGB(new double[]{t, 0.75, 0.68});
        return ColorUtils.color(1, rgb[0], rgb[1], rgb[2]);
    }

    /**
     * To make sure hash results are consistent across JVMs.
     */
    private static int fnv1a32(String s) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < s.length(); i++) {
            hash ^= s.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }

    static IGuiTexture resolveIcon(TypeHandle typeHandle) {
        String id = (typeHandle != null) ? typeHandle.getIdentification() : null;
        return CUSTOM_ID_TO_ICON.getOrDefault(id, IGuiTexture.EMPTY);
    }

    static Supplier<Object> resolveDefaultValue(TypeHandle typeHandle) {
        String id = (typeHandle != null) ? typeHandle.getIdentification() : null;
        return CUSTOM_ID_TO_DEFAULT_VALUE.getOrDefault(id, () -> null);
    }

    static boolean isCustomTypeHandle(TypeHandle typeHandle) {
        String id = (typeHandle != null) ? typeHandle.getIdentification() : null;
        return id != null && CUSTOM_ID_TO_DESCRIPTOR.containsKey(id);
    }

    static String getFriendlyNameInternal(TypeHandle typeHandle) {
        String id = (typeHandle != null) ? typeHandle.getIdentification() : null;
        if (id == null) return null;
        TypeHandleDescriptor d = CUSTOM_ID_TO_DESCRIPTOR.get(id);
        return (d != null) ? d.friendlyName() : null;
    }

    /* --------- private plumbing --------- */
    private record HandleResult(TypeHandle handle, boolean isNew) {
    }

    private static HandleResult getOrCreateCustomTypeHandle(String uniqueId, @Nullable String friendlyName) {
        Objects.requireNonNull(uniqueId, "uniqueId");

        TypeHandleDescriptor existing = CUSTOM_ID_TO_DESCRIPTOR.get(uniqueId);
        if (existing == null) {
            TypeHandle th = TypeHandle.create(uniqueId);
            CUSTOM_ID_TO_DESCRIPTOR.put(uniqueId, new TypeHandleDescriptor(th, friendlyName));
            return new HandleResult(th, true);
        }

        // Unity logic: if friendlyName differs, throw
        if (!Objects.equals(existing.friendlyName(), friendlyName)) {
            throw new IllegalStateException(
                    "A custom TypeHandle with same friendly name '" + friendlyName + "' already exists"
            );
        }

        return new HandleResult(existing.typeHandle(), false);
    }

    public static String identificationOf(Type t) {
        if (t instanceof Class<?> c) return c.getName();
        return t.getTypeName();
    }

    static String convertTypeName(String identification) {
        return identification;
    }

    static String friendlyNameOf(Type t) {
        if (t == null) return "Unknown";
        if (t instanceof Class<?> c) {
            // small primitive/wrapper friendly mapping
            if (c == int.class || c == Integer.class) return "int";
            if (c == long.class || c == Long.class) return "long";
            if (c == float.class || c == Float.class) return "float";
            if (c == double.class || c == Double.class) return "double";
            if (c == boolean.class || c == Boolean.class) return "bool";
            if (c == char.class || c == Character.class) return "char";
            if (c == byte.class || c == Byte.class) return "byte";
            if (c == String.class) return "string";
            return c.getSimpleName();
        }
        // For ParameterizedType, show like List<String>
        return t.getTypeName();
    }
    
    public static Type convertType(Type t) {
        if (t instanceof Class<?> clazz && clazz.isPrimitive()) {
            if (clazz == int.class) return Integer.class;
            if (clazz == long.class) return Long.class;
            if (clazz == float.class) return Float.class;
            if (clazz == double.class) return Double.class;
            if (clazz == boolean.class) return Boolean.class;
            if (clazz == char.class) return Character.class;
            if (clazz == byte.class) return Byte.class;
            if (clazz == short.class) return Short.class;
            if (clazz == void.class) return Void.class;
        }
        return t;
    }

    private static boolean typeEquals(Type a, Type b) {
        return Objects.equals(a, b); // TypeToken's Type supports equals()
    }

    private static String safeTypeName(Type t) {
        return (t == null) ? "null" : t.getTypeName();
    }
}
