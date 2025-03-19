package com.lowdragmc.lowdraglib.gui.editor.runtime;

import com.google.common.base.Strings;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.syncdata.ManagedFieldUtils;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import com.lowdragmc.lowdraglib.utils.TagUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/12/6
 * @implNote PersistedParser
 */
public final class PersistedParser {

    public static CompoundTag serializeNBT(Object object, HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        serializeNBT(tag, object.getClass(), object, provider);
        return tag;
    }

    /**
     * This method is used to serialize the object fields with {@link Persisted} or {@link Configurable} annotation to the NBT data.
     */
    public static void serializeNBT(CompoundTag tag, Class<?> clazz, Object object, HolderLookup.Provider provider) {
        if (clazz == Object.class || clazz == null) return;

        serializeNBT(tag, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();

            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
            } else {
                continue;
            }

            Tag nbt = null;
            // sub configurable
            if ((field.isAnnotationPresent(Configurable.class) && field.getAnnotation(Configurable.class).subConfigurable()) || (field.isAnnotationPresent(Persisted.class) && field.getAnnotation(Persisted.class).subPersisted())) {
                try {
                    field.setAccessible(true);
                    var value = field.get(object);
                    if (value != null) {
                        if (value instanceof INBTSerializable<?> serializable) {
                            nbt = serializable.serializeNBT(provider);
                        } else {
                            nbt = new CompoundTag();
                            serializeNBT((CompoundTag)nbt, ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            } else {
                nbt = ManagedFieldUtils.createKey(field).createRef(object).readPersisted(NbtOps.INSTANCE);
            }
            if (nbt != null) {
                TagUtils.setTagExtended(tag, key, nbt);
            }

        }
    }

    /**
     * This method is used to deserialize the NBT data to the object fields with {@link Persisted} or {@link Configurable} annotation.
     */
    public static void deserializeNBT(CompoundTag tag, Map<String, Method> setters, Class<?> clazz, Object object, HolderLookup.Provider provider) {
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

        deserializeNBT(tag, setters, clazz.getSuperclass(), object, provider);

        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String key = field.getName();

            if (field.isAnnotationPresent(Configurable.class)) {
                Configurable configurable = field.getAnnotation(Configurable.class);
                if (!configurable.persisted()) {
                    continue;
                } else if (!Strings.isNullOrEmpty(configurable.key())) {
                    key = configurable.key();
                }
            } else if (field.isAnnotationPresent(Persisted.class)) {
                Persisted persisted = field.getAnnotation(Persisted.class);
                if (!Strings.isNullOrEmpty(persisted.key())) {
                    key = persisted.key();
                }
            } else {
                continue;
            }

            Tag nbt = TagUtils.getTagExtended(tag, key);
            // sub configurable
            if (nbt != null) {
                if ((field.isAnnotationPresent(Configurable.class) && field.getAnnotation(Configurable.class).subConfigurable()) || (field.isAnnotationPresent(Persisted.class) && field.getAnnotation(Persisted.class).subPersisted())) {
                    try {
                        field.setAccessible(true);
                        var value = field.get(object);
                        if (value != null) {
                            if (value instanceof INBTSerializable serializable) {
                                serializable.deserializeNBT(provider, nbt);
                            } else if (nbt instanceof CompoundTag compoundTag) {
                                deserializeNBT(compoundTag, new HashMap<>(), ReflectionUtils.getRawType(field.getGenericType()), value, provider);
                            }
                        }
                    } catch (IllegalAccessException ignored) {}
                } else {
                    ManagedFieldUtils.createKey(field).createRef(object).writePersisted(NbtOps.INSTANCE, nbt);
                    Method setter = setters.get(field.getName());

                    if (setter != null) {
                        field.setAccessible(true);
                        try {
                            setter.invoke(object, field.get(object));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

}
