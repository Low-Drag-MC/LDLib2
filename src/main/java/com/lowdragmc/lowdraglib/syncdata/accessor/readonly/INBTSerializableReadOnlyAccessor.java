package com.lowdragmc.lowdraglib.syncdata.accessor.readonly;

import com.lowdragmc.lowdraglib.Platform;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unchecked")
public class INBTSerializableReadOnlyAccessor implements IReadOnlyAccessor<INBTSerializable<?>> {

    @Override
    public boolean test(Class<?> type) {
        return INBTSerializable.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull INBTSerializable<?> value) {
        var tag = value.serializeNBT(Platform.getFrozenRegistry());
        return op == NbtOps.INSTANCE ? (T) tag : NbtOps.INSTANCE.convertTo(op, tag);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, INBTSerializable<?> value, T payload) {
        ((INBTSerializable)value).deserializeNBT(Platform.getFrozenRegistry(), op == NbtOps.INSTANCE ?
                (Tag) payload : op.convertTo(NbtOps.INSTANCE, payload));
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull INBTSerializable<?> value) {
        buffer.writeNbt(value.serializeNBT(buffer.registryAccess()));
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull INBTSerializable<?> value) {
        var nbt = buffer.readNbt();
        if (nbt != null) {
            ((INBTSerializable)value).deserializeNBT(buffer.registryAccess(), nbt);
        }
    }

}
