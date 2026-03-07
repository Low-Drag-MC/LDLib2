package com.lowdragmc.lowdraglib2.syncdata.accessor.readonly;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jetbrains.annotations.NotNull;

public class ValueIOSerializableReadOnlyAccessor implements IReadOnlyAccessor<ValueIOSerializable> {

    @Override
    public boolean test(Class<?> type) {
        return ValueIOSerializable.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull ValueIOSerializable value) {
        HolderLookup.Provider registry = Platform.getFrozenRegistry();
        if (op instanceof RegistryOps<T> registryOps) {
            registry = CommonHooks.extractLookupProvider(registryOps);
        }
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var output = TagValueOutput.createWithContext(reporter, registry);
            value.serialize(output);
            var tag = output.buildResult();
            return (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) ? (T) tag : NbtOps.INSTANCE.convertTo(op, tag);
        }
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, ValueIOSerializable value, T payload) {
        HolderLookup.Provider registry = Platform.getFrozenRegistry();
        if (op instanceof RegistryOps<T> registryOps) {
            registry = CommonHooks.extractLookupProvider(registryOps);
        }
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var data = (op == NbtOps.INSTANCE || op instanceof DelegatingOpsAccessor<?> accessor && accessor.getDelegate() == NbtOps.INSTANCE) ?
                    (Tag) payload : op.convertTo(NbtOps.INSTANCE, payload);
            if (data instanceof CompoundTag tag) {
                value.deserialize(TagValueInput.create(reporter, registry, tag));
            }
        }
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull ValueIOSerializable value) {
        try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
            var output = TagValueOutput.createWithContext(reporter, buffer.registryAccess());
            value.serialize(output);
            buffer.writeNbt(output.buildResult());
        }
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull ValueIOSerializable value) {
        var nbt = buffer.readNbt();
        if (nbt != null) {
            try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                value.deserialize(TagValueInput.create(reporter, buffer.registryAccess(), nbt));
            }
        }
    }

}
