package com.lowdragmc.lowdraglib.syncdata.accessor.direct;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.var.FieldVar;
import com.lowdragmc.lowdraglib.syncdata.ref.DirectRef;
import com.lowdragmc.lowdraglib.syncdata.var.IVar;
import com.lowdragmc.lowdraglib.syncdata.ref.UniqueDirectRef;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.WeakHashMap;

public class EnumAccessor implements IDirectAccessor<Enum<?>> {
    private static final WeakHashMap<Class<? extends Enum<?>>, Enum<?>[]> enumCache = new WeakHashMap<>();
    private static final WeakHashMap<Class<? extends Enum<?>>, Map<String, Enum<?>>> enumNameCache = new WeakHashMap<>();

    public static Enum<?> getEnum(Class<Enum<?>> type, String name) {
        var values = enumNameCache.computeIfAbsent(type, t -> {
            var map = new WeakHashMap<String, Enum<?>>();
            for (var value : t.getEnumConstants()) {
                String enumName = getEnumName(value);

                map.put(enumName, value);
            }
            return map;
        });
        var value = values.get(name);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    public static Enum<?> getEnum(Class<Enum<?>> type, int ordinal) {
        var values = enumCache.computeIfAbsent(type, Class::getEnumConstants);
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Invalid ordinal for enum type " + type.getName() + ": " + ordinal);
        }
        return type.cast(values[ordinal]);
    }

    public static String getEnumName(Enum<?> enumValue) {
        if (enumValue instanceof StringRepresentable provider) {
            return provider.getSerializedName();
        } else {
            return enumValue.name();
        }
    }

    @Override
    public boolean test(Class<?> type) {
        return type.isEnum();
    }

    @Override
    public <T> T readDirectVar(DynamicOps<T> op, IVar<Enum<?>> var) {
        return op.createString(getEnumName(var.value()));
    }

    @Override
    public <T> void writeDirectVar(DynamicOps<T> op, IVar<Enum<?>> var, T payload) {
        var.set(getEnum(var.getType(), op.getStringValue(payload).getOrThrow()));
    }

    @Override
    public void readDirectVarToStream(RegistryFriendlyByteBuf buffer, IVar<Enum<?>> var) {
        buffer.writeVarInt(var.value().ordinal());
    }

    @Override
    public void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, IVar<Enum<?>> var) {
        var.set(var.getType().getEnumConstants()[buffer.readVarInt()]);
    }

    @Override
    public DirectRef<Enum<?>> createDirectRef(ManagedKey managedKey, IVar<Enum<?>> var) {
        return new UniqueDirectRef<>(var, managedKey, this);
    }

    @Override
    public IVar<Enum<?>> createDirectVar(ManagedKey managedKey, @NotNull Object holder) {
        return FieldVar.of(managedKey.getRawField(), holder);
    }

}
