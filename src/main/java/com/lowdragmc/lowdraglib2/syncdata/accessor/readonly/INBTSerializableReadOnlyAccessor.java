package com.lowdragmc.lowdraglib2.syncdata.accessor.readonly;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.CommonHooks;
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
        HolderLookup.Provider registry = Platform.getFrozenRegistry();
        if (op instanceof RegistryOps<T> registryOps) {
            registry = CommonHooks.extractLookupProvider(registryOps);
        }
        var tag = value.serializeNBT(registry);
        return (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) ? (T) tag : NbtOps.INSTANCE.convertTo(op, tag);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, INBTSerializable<?> value, T payload) {
        HolderLookup.Provider registry = Platform.getFrozenRegistry();
        if (op instanceof RegistryOps<T> registryOps) {
            registry = CommonHooks.extractLookupProvider(registryOps);
        }
        ((INBTSerializable)value).deserializeNBT(registry,
                (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) ?
                (Tag) payload : op.convertTo(NbtOps.INSTANCE, payload));
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull INBTSerializable<?> value) {
        buffer.writeNbt(value.serializeNBT(buffer.registryAccess()));
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull INBTSerializable<?> value) {
        var nbt = buffer.readNbt(NbtAccounter.unlimitedHeap());
        if (nbt != null) {
            ((INBTSerializable)value).deserializeNBT(buffer.registryAccess(), nbt);
        }
    }

}
