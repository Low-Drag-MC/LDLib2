package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.DirectField;
import com.lowdragmc.lowdraglib.syncdata.managed.DirectRef;
import com.lowdragmc.lowdraglib.syncdata.managed.IDirectVar;
import com.lowdragmc.lowdraglib.syncdata.managed.UniqueDirectRef;
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
public final class RegistryAccessor<TYPE> implements IDirectAccessor<IDirectVar<TYPE>> {
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
    public <T> T readDirectVar(DynamicOps<T> op, IDirectVar<TYPE> var) {
        return codec.encodeStart(op, var.value()).getOrThrow();
    }

    @Override
    public <T> void writeDirectVar(DynamicOps<T> op, IDirectVar<TYPE> var, T payload) {
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
    public void readDirectVarToStream(RegistryFriendlyByteBuf buffer, IDirectVar<TYPE> var) {
        streamCodec.encode(buffer, var.value());
    }

    @Override
    public void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, IDirectVar<TYPE> var) {
        var.set(streamCodec.decode(buffer));
    }

    @Override
    public DirectRef<IDirectVar<TYPE>> createDirectRef(ManagedKey managedKey, IDirectVar<TYPE> var) {
        return new UniqueDirectRef<>(var, managedKey, this);
    }

    @Override
    public IDirectVar<TYPE> createDirectVar(ManagedKey managedKey, @NotNull Object holder) {
        return DirectField.of(managedKey.getRawField(), holder);
    }

}
