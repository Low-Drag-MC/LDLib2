package com.lowdragmc.lowdraglib2.syncdata.accessor.readonly;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
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

import java.util.Objects;

/**
 * Compares values with the {@link MarkStrategies strategy} registered for their class, or else with what
 * {@link ValueIOSerializable#serialize} writes. Subclasses may save or sync more than the base class, so they keep
 * comparing what {@link #readReadOnlyValue} returns.
 */
public class ValueIOSerializableReadOnlyAccessor implements IReadOnlyAccessor<ValueIOSerializable>, IMarkFunction<ValueIOSerializable, Object> {

    private static final IMarkFunction<ValueIOSerializable, Object> MARKS = MarkStrategies.orElse(new SavedTagMark());

    private record SavedTagMark() implements IMarkFunction<ValueIOSerializable, Tag> {
        private static CompoundTag save(ValueIOSerializable value) {
            // marks are taken every period, so problems are left to the real save to report
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, Platform.getFrozenRegistry());
            value.serialize(output);
            return output.buildResult();
        }

        @Override
        public @NotNull Tag obtainManagedMark(@NotNull ValueIOSerializable value) {
            return save(value);
        }

        @Override
        public boolean areDifferent(@NotNull Tag managedMark, @NotNull ValueIOSerializable value) {
            return !save(value).equals(managedMark);
        }

        @Override
        public Tag updateMark(@NotNull Tag managedMark, @NotNull ValueIOSerializable value) {
            var tag = save(value);
            return tag.equals(managedMark) ? null : tag;
        }
    }

    private boolean subclassed() {
        return getClass() != ValueIOSerializableReadOnlyAccessor.class;
    }

    private Object readValue(ValueIOSerializable value) {
        return readReadOnlyValue(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), value);
    }

    @Override
    public boolean test(Class<?> type) {
        return ValueIOSerializable.class.isAssignableFrom(type);
    }

    @Override
    public @NotNull Object obtainManagedMark(@NotNull ValueIOSerializable value) {
        return subclassed() ? readValue(value) : MARKS.obtainManagedMark(value);
    }

    @Override
    public boolean areDifferent(@NotNull Object managedMark, @NotNull ValueIOSerializable value) {
        return subclassed() ? !Objects.equals(managedMark, readValue(value)) : MARKS.areDifferent(managedMark, value);
    }

    @Override
    public Object updateMark(@NotNull Object managedMark, @NotNull ValueIOSerializable value) {
        // a subclass may override the two methods above, which the default goes through
        return subclassed() ? IMarkFunction.super.updateMark(managedMark, value) : MARKS.updateMark(managedMark, value);
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
