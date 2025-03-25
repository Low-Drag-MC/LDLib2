package com.lowdragmc.lowdraglib.syncdata.accessor.direct;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.var.FieldVar;
import com.lowdragmc.lowdraglib.syncdata.ref.DirectRef;
import com.lowdragmc.lowdraglib.syncdata.var.IVar;
import com.lowdragmc.lowdraglib.syncdata.ref.UniqueDirectRef;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

@Getter
public final class RegistryAccessor<TYPE> implements IDirectAccessor<TYPE> {
    private final Class<TYPE> typeClass;
    private final Registry<TYPE> registry;
    private final Codec<TYPE> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, TYPE> streamCodec;

    private RegistryAccessor(Class<TYPE> typeClass, Registry<TYPE> registry) {
        this.typeClass = typeClass;
        this.registry = registry;
        this.codec = registry.byNameCodec();
        this.streamCodec = ByteBufCodecs.registry(registry.key());
    }

    public static <TYPE> RegistryAccessor<TYPE> of(Class<TYPE> typeClass, Registry<TYPE> registry) {
        return new RegistryAccessor<>(typeClass, registry);
    }

    @Override
    public boolean test(Class<?> type) {
        return typeClass == type;
    }

    @Override
    public <T> T readDirectVar(DynamicOps<T> op, IVar<TYPE> var) {
        return codec.encodeStart(op, var.value()).getOrThrow();
    }

    @Override
    public <T> void writeDirectVar(DynamicOps<T> op, IVar<TYPE> var, T payload) {
        var type = codec.parse(op, payload).result();
        if (type.isPresent()) {
            var.set(type.get());
        } else if (registry instanceof DefaultedRegistry<TYPE> defaultedRegistry) {
            var.set(defaultedRegistry.get(defaultedRegistry.getDefaultKey()));
        } else {
            LDLib.LOGGER.error("Cannot parse the payload {} to the registry type {}.", payload, typeClass);
            throw new IllegalArgumentException("Cannot parse the payload to the registry type.");
        }
    }

    @Override
    public void readDirectVarToStream(RegistryFriendlyByteBuf buffer, IVar<TYPE> var) {
        streamCodec.encode(buffer, var.value());
    }

    @Override
    public void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, IVar<TYPE> var) {
        var.set(streamCodec.decode(buffer));
    }

    @Override
    public DirectRef<TYPE> createDirectRef(ManagedKey managedKey, IVar<TYPE> var) {
        return new UniqueDirectRef<>(var, managedKey, this);
    }

    @Override
    public IVar<TYPE> createDirectVar(ManagedKey managedKey, @NotNull Object holder) {
        return FieldVar.of(managedKey.getRawField(), holder);
    }

}
