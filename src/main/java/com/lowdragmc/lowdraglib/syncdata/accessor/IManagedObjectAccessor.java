package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.IManagedReadOnlyRef;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class IManagedObjectAccessor implements IReadOnlyAccessor<IManaged> {

    @Override
    public boolean test(Class<?> type) {
        return IManaged.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull IManaged value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, IManaged value, T payload) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull IManaged value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull IManaged value) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public IManagedReadOnlyRef createReadOnlyRef(ManagedKey managedKey, IManaged value) {
        return new IManagedReadOnlyRef(value, managedKey, this);
    }
}
