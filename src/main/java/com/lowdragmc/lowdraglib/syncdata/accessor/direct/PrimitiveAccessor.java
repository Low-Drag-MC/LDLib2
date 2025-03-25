package com.lowdragmc.lowdraglib.syncdata.accessor.direct;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.var.FieldVar;
import com.lowdragmc.lowdraglib.syncdata.ref.DirectRef;
import com.lowdragmc.lowdraglib.syncdata.var.IVar;
import com.lowdragmc.lowdraglib.syncdata.ref.UniqueDirectRef;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

@Getter
public final class PrimitiveAccessor<TYPE> implements IDirectAccessor<TYPE> {

    private final Class<?>[] operandTypes;
    private final PrimitiveCodec<TYPE> codec;
    private final StreamCodec<ByteBuf, TYPE> streamCodec;

    private PrimitiveAccessor(PrimitiveCodec<TYPE> codec, StreamCodec<ByteBuf, TYPE> streamCodec, Class<?> ...operandTypes) {
        this.operandTypes = operandTypes;
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    public static <T> PrimitiveAccessor<T> of(PrimitiveCodec<T> codec, StreamCodec<ByteBuf, T> streamCodec, Class<?> ...operandTypes) {
        return new PrimitiveAccessor<>(codec, streamCodec, operandTypes);
    }

    /**
     * Test if the given type is supported by this accessor.
     *
     * @param type The type to test.
     * @return True if the type is supported.
     */
    public boolean test(Class<?> type) {
        for (Class<?> aClass : operandTypes) {
            if (aClass == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T> T readDirectVar(DynamicOps<T> op, IVar<TYPE> var) {
        return codec.write(op, var.value());
    }

    @Override
    public <T> void writeDirectVar(DynamicOps<T> op, IVar<TYPE> var, T payload) {
        codec.read(op, payload).result().ifPresent(var::set);
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
