package com.lowdragmc.lowdraglib.syncdata.accessor.arraylike;
import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib.syncdata.var.ManagedHolderVar;
import com.lowdragmc.lowdraglib.utils.LDLibExtraCodecs;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CollectionAccessor<TYPE> implements
        IReadOnlyAccessor<Collection<TYPE>>,
        IArrayLikeAccessor<TYPE, Collection<TYPE>>,
        IMarkFunction<Collection<TYPE>, Object[]> {
    @Getter
    private final IAccessor<TYPE> childAccessor;
    @Getter
    private final Class<TYPE> childType;

    public CollectionAccessor(IAccessor<TYPE> childAccessor, Class<TYPE> childType) {
        this.childAccessor = childAccessor;
        this.childType = childType;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return Collection.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull Collection<TYPE> value) {
        return op.createList(value.stream().map(v -> {
            if (childAccessor instanceof IDirectAccessor<TYPE> directAccessor) {
                if (v == null) {
                    return LDLibExtraCodecs.createStringNull(op);
                }
                return directAccessor.readDirectVar(op, ManagedHolderVar.of(v));
            } else if (childAccessor instanceof IReadOnlyAccessor<TYPE> readOnlyAccessor) {
                if (v == null) {
                    throw new IllegalArgumentException("Null value of a read-only accessor in collection");
                }
                return readOnlyAccessor.readReadOnlyValue(op, v);
            } else {
                throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
            }
        }));
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, Collection<TYPE> value, T payload) {
        var stream = op.getStream(payload).getOrThrow();
        if (childAccessor instanceof IDirectAccessor<TYPE> directAccessor) {
            value.clear();
            var holder = ManagedHolderVar.ofNull(childType);
            stream.map(p -> {
                if (p == LDLibExtraCodecs.createStringNull(op)) {
                    return null;
                }
                directAccessor.writeDirectVar(op, holder, p);
                return holder.value();
            }).forEach(value::add);
        } else if (childAccessor instanceof IReadOnlyAccessor<TYPE> readOnlyAccessor) {
            var list = stream.toList();
            if (list.size() != value.size()) {
                throw new IllegalArgumentException("Stream size of a read-only accessor is not equal to the collection size");
            }
            var iterValue = value.iterator();
            var iterPayload = list.iterator();
            while (iterValue.hasNext()) {
                var v = iterValue.next();
                var p = iterPayload.next();
                if (LDLibExtraCodecs.isEmptyOrStringNull(op, p)) {
                    throw new IllegalArgumentException("Empty value in the stream of a read-only accessor");
                }
                readOnlyAccessor.writeReadOnlyValue(op, v, p);
            }
        } else {
            throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
        }
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull Collection<TYPE> value) {
        buffer.writeVarInt(value.size());
        for (var v : value) {
            if (childAccessor instanceof IDirectAccessor<TYPE> directAccessor) {
                if (v == null) {
                    buffer.writeBoolean(true);
                    continue;
                }
                buffer.writeBoolean(false);
                directAccessor.readDirectVarToStream(buffer, ManagedHolderVar.of(v));
            } else if (childAccessor instanceof IReadOnlyAccessor<TYPE> readOnlyAccessor) {
                if (v == null) {
                    throw new IllegalArgumentException("Null value of a read-only accessor in collection");
                }
                readOnlyAccessor.readReadOnlyValueToStream(buffer, v);
            } else {
                throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
            }
        }
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull Collection<TYPE> value) {
        var size = buffer.readVarInt();
        if (childAccessor instanceof IDirectAccessor<TYPE> directAccessor) {
            value.clear();
            var holder = ManagedHolderVar.ofNull(childType);
            for (int i = 0; i < size; i++) {
                if (buffer.readBoolean()) {
                    value.add(null);
                } else {
                    directAccessor.readDirectVarToStream(buffer, holder);
                    value.add(holder.value());
                }
            }
        } else if (childAccessor instanceof IReadOnlyAccessor<TYPE> readOnlyAccessor) {
            if (size != value.size()) {
                throw new IllegalArgumentException("Stream size of a read-only accessor is not equal to the collection size");
            }
            for (int i = 0; i < size; i++) {
                if (buffer.readBoolean()) {
                    throw new IllegalArgumentException("Empty value in the stream of a read-only accessor");
                }
                var v = value.iterator().next();
                readOnlyAccessor.writeReadOnlyValueFromStream(buffer, v);
            }
        } else {
            throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
        }
    }

    @Override
    public Object[] obtainManagedMark(@NotNull Collection<TYPE> value) {
        return value.stream().map(v -> {
            if (v == null) {
                return null;
            }
            return switch (childAccessor) {
                case IMarkFunction markFunction -> markFunction.obtainManagedMark(v);
                case IDirectAccessor<TYPE> directAccessor ->
                        directAccessor.readDirectVar(JavaOps.INSTANCE, ManagedHolderVar.of(v));
                case IReadOnlyAccessor<TYPE> readOnlyAccessor ->
                        readOnlyAccessor.readReadOnlyValue(JavaOps.INSTANCE, v);
                case null, default ->
                        throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
            };
        }).toArray();
    }

    @Override
    public boolean areDifferent(Object[] managedMark, @NotNull Collection<TYPE> value) {
        if (managedMark.length != value.size()) {
            return true;
        }
        var iterValue = value.iterator();
        for (var mark : managedMark) {
            var v = iterValue.next();
            if (v == null) {
                if (mark != null) {
                    return true;
                }
                continue;
            }
            switch (childAccessor) {
                case IMarkFunction markFunction -> {
                    if (markFunction.areDifferent(mark, v)) {
                        return true;
                    }
                }
                case IDirectAccessor<TYPE> directAccessor -> {
                    if (!directAccessor.readDirectVar(JavaOps.INSTANCE, ManagedHolderVar.of(v)).equals(mark)) {
                        return true;
                    }
                }
                case IReadOnlyAccessor<TYPE> readOnlyAccessor -> {
                    if (!readOnlyAccessor.readReadOnlyValue(JavaOps.INSTANCE, v).equals(mark)) {
                        return true;
                    }
                }
                case null, default ->
                        throw new IllegalArgumentException("Child accessor %s is not managed for collection accessor".formatted(childAccessor));
            }
        }
        return false;
    }
}
