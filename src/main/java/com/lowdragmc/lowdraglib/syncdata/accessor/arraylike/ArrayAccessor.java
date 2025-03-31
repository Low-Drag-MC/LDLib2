package com.lowdragmc.lowdraglib.syncdata.accessor.arraylike;

import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.ref.DirectArrayRef;
import com.lowdragmc.lowdraglib.syncdata.ref.IArrayRef;
import com.lowdragmc.lowdraglib.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib.syncdata.ref.ReadOnlyArrayRef;
import com.lowdragmc.lowdraglib.syncdata.var.FieldVar;
import com.lowdragmc.lowdraglib.syncdata.var.ReadOnlyVar;
import com.lowdragmc.lowdraglib.utils.LDLibExtraCodecs;
import com.mojang.serialization.DynamicOps;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.List;

public class ArrayAccessor<TYPE, TYPE_ARRAY> implements IArrayLikeAccessor<TYPE, TYPE_ARRAY> {
    @Getter
    private final IAccessor<TYPE> childAccessor;
    @Getter
    private final Class<TYPE> childType;

    public ArrayAccessor(IAccessor<TYPE> childAccessor, Class<TYPE> childType) {
        this.childAccessor = childAccessor;
        this.childType = childType;
    }

    @Override
    public boolean test(Class<?> type) {
        return type.isArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public IArrayRef<TYPE, TYPE_ARRAY> createRef(ManagedKey managedKey, @NotNull Object holder) {
        if (isReadOnly()) {
            return (IArrayRef<TYPE, TYPE_ARRAY>) ReadOnlyArrayRef.of(ReadOnlyVar.of(managedKey, holder), managedKey,this);
        } else {
            return DirectArrayRef.of(FieldVar.of(managedKey.getRawField(), holder), managedKey, this);
        }
    }

    @Override
    public <T> T readField(DynamicOps<T> op, IRef<TYPE_ARRAY> ref) {
        var arrayRef = (IArrayRef<TYPE, TYPE_ARRAY>) ref;
        if (isReadOnly()) {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
            }
            return readListField(op, refs);
        } else {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                return LDLibExtraCodecs.createStringNull(op);
            }
            return readListField(op, refs);
        }
    }

    private <T> T readListField(DynamicOps<T> op, IRef<TYPE>[] refs) {
        var listBuilder = op.listBuilder();
        for (IRef<TYPE> ref : refs) {
            listBuilder.add(childAccessor.readField(op, ref));
        }
        return listBuilder.build(op.empty()).getOrThrow();
    }

    @Override
    public <T> void writeField(DynamicOps<T> op, IRef<TYPE_ARRAY> ref, T payload) {
        var arrayRef = (IArrayRef<TYPE, TYPE_ARRAY>) ref;
        if (isReadOnly()) {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
            }
            writeListField(op, ref, op.getStream(payload).getOrThrow().toList(), refs);
        } else {
            var refs = arrayRef.getRefs();
            if (LDLibExtraCodecs.isEmptyOrStringNull(op, payload)) {
                ((DirectArrayRef<TYPE, TYPE_ARRAY>)ref).getField().set(null);
                return;
            }
            var payloads = op.getStream(payload).getOrThrow().toList();
            if (refs == null || refs.length != payloads.size()) {
                var newValues = (TYPE_ARRAY) Array.newInstance(getChildType(), payloads.size());
                ((DirectArrayRef<TYPE, TYPE_ARRAY>)ref).getField().set(newValues);
                arrayRef.updateRefs(newValues);
                refs = arrayRef.getRefs();
            }
            writeListField(op, ref, payloads, refs);
        }
    }

    private <T> void writeListField(DynamicOps<T> op, IRef<TYPE_ARRAY> ref, List<T> payloads, IRef<TYPE>[] refs) {
        if (payloads.size() != refs.length) {
            throw new IllegalArgumentException("readonly field %s has a different length of payload".formatted(ref.getKey()));
        }
        for (int i = 0; i < refs.length; i++) {
            childAccessor.writeField(op, refs[i], payloads.get(i));
        }
    }

    @Override
    public void readFieldToStream(RegistryFriendlyByteBuf buffer, IRef<TYPE_ARRAY> ref) {
        var arrayRef = (IArrayRef<TYPE, TYPE_ARRAY>) ref;
        if (isReadOnly()) {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
            }
            writeListField(buffer, refs);
        } else {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                buffer.writeBoolean(true);
                return;
            }
            buffer.writeBoolean(false);
            writeListField(buffer, refs);
        }
    }

    private void writeListField(RegistryFriendlyByteBuf buffer, IRef<TYPE>[] refs) {
        buffer.writeVarInt(refs.length);
        for (IRef<TYPE> typeiRef : refs) {
            typeiRef.readSyncToStream(buffer);
        }
    }

    @Override
    public void writeFieldFromStream(RegistryFriendlyByteBuf buffer, IRef<TYPE_ARRAY> ref) {
        var arrayRef = (IArrayRef<TYPE, TYPE_ARRAY>) ref;
        if (isReadOnly()) {
            var refs = arrayRef.getRefs();
            if (refs == null) {
                throw new IllegalArgumentException("readonly field %s has a null reference".formatted(ref.getKey()));
            }
            var length = buffer.readVarInt();
            if (length != refs.length) {
                throw new IllegalArgumentException("readonly field %s has a different length of refs".formatted(refs[0].getKey()));
            }
            readListField(buffer, refs);
        } else {
            var refs = arrayRef.getRefs();
            if (buffer.readBoolean()) {
                ((DirectArrayRef<TYPE, TYPE_ARRAY>)ref).getField().set(null);
                return;
            }
            var length = buffer.readVarInt();
            if (refs == null || length != refs.length) {
                var newValues = (TYPE_ARRAY) Array.newInstance(getChildType(), length);
                ((DirectArrayRef<TYPE, TYPE_ARRAY>)ref).getField().set(newValues);
                arrayRef.updateRefs(newValues);
                refs = arrayRef.getRefs();
            }
            readListField(buffer, refs);
        }
    }

    private void readListField(RegistryFriendlyByteBuf buffer, IRef<TYPE>[] refs) {
        for (IRef<TYPE> typeiRef : refs) {
            typeiRef.writeSyncFromStream(buffer);
        }
    }
}
