package com.lowdragmc.lowdraglib2.nodegraphtookit.api.type;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
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

    /**
     * Whether a literal of this type can be authored — i.e. whether a constant node of it would hold
     * a value, rather than spawn, show nothing and emit {@code null}.
     *
     * <h2>Why this is a predicate and not a hand-written list</h2>
     * "A port can carry it" and "a user can type one in" are different questions, and only the first
     * is answerable from the graph's node set — so a graph cannot derive the second from its nodes,
     * and used to answer it by listing types by hand instead. That list is only ever as correct as
     * the last person who remembered to edit it: it omits a type that was minted after it was
     * written, and it keeps offering a broken node for a type that lost what made it authorable.
     *
     * <h2>Why the test is the default value and not the configurator registry</h2>
     * The question one <em>wants</em> to ask is "would {@link ITypeConfigurable#DEFAULT} find a
     * widget for this type", and it cannot be asked from here. It bottoms out in
     * {@link com.lowdragmc.lowdraglib2.configurator.ConfiguratorAccessors}, whose registry field is
     * {@code @OnlyIn(Dist.CLIENT)}; off the client that is not a graceful miss but a
     * {@code NoSuchFieldError}, and a graph's supported types are read on both sides.
     *
     * <p>A registered default value is the common-side stand-in, and it is a real signal rather than
     * a coincidence: every path that builds a constant editor seeds it from
     * {@link TypeHandle#getDefaultValue()}, so a handle without one either dereferences null while
     * building the row (accessor-backed types) or shows a plausible first value it never writes back
     * (enums through {@code SelectorConfigurator}). Registering a default is the act by which an
     * author says "a constant of this is a thing a user can hold", which is the question.</p>
     *
     * <p>It is also what keeps {@code List}/{@code Map} out, where a widget-based test would let them
     * in: a raw collection does resolve to a {@code CollectionConfiguratorAccessor}, but a handle
     * registered without a default has nothing to put in it and serialising the empty constant
     * fails.</p>
     *
     * <p>The gap this leaves is a type that has a default but no widget — it renders an empty
     * inspector row, and only a client can tell. {@code ChunkPos} is the one live instance: syncdata
     * accessor and default value, no configurator accessor. Until one is written, a graph that would
     * otherwise offer it has to cut it by hand.</p>
     *
     * @param typeHandle the handle to test; {@code null} is not authorable
     * @return {@code true} if a constant of this type would carry a value
     */
    public static boolean canAuthorLiteral(@Nullable TypeHandle typeHandle) {
        return typeHandle != null && typeHandle.getDefaultValue() != null;
    }

    /**
     * {@code types} narrowed to the ones a user can author a literal of, order preserved.
     *
     * @see #canAuthorLiteral(TypeHandle)
     */
    public static List<TypeHandle> authorableTypes(@Nullable Collection<TypeHandle> types) {
        if (types == null) return List.of();
        return types.stream().filter(TypeHandleHelpers::canAuthorLiteral).toList();
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
