package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.arraylike.ArrayAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.var.ArrayVar;
import com.lowdragmc.lowdraglib2.syncdata.var.ReadOnlyVar;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;

public final class ReadOnlyArrayRef<TYPE> extends ReadOnlyManagedRef<TYPE[]> implements IArrayRef<TYPE, TYPE[]> {
    @Getter
    @Nullable
    private ReadOnlyRef<TYPE>[] refs;

    private ReadOnlyArrayRef(ReadOnlyVar<TYPE[]> var, ManagedKey key, ArrayAccessor<TYPE, TYPE[]> accessor) {
        super(var, key, accessor);
        updateRefs(var.value());
    }

    @SuppressWarnings("unchecked")
    public static <TYPE, TYPE_ARRAY> ReadOnlyArrayRef<TYPE> of(ReadOnlyVar<TYPE_ARRAY> var, ManagedKey key, ArrayAccessor<TYPE, TYPE_ARRAY> accessor) {
        return new ReadOnlyArrayRef<>((ReadOnlyVar<TYPE[]>)var, key, (ArrayAccessor<TYPE, TYPE[]>)accessor);
    }

    @Override
    public ArrayAccessor<TYPE, TYPE[]> getAccessor() {
        return (ArrayAccessor<TYPE, TYPE[]>) super.getAccessor();
    }

    @Override
    public void readOnlyManagedUpdate() {
        var newValue = readRaw();
        if ((oldUid == null && newValue != null) || (oldUid != null && newValue == null)) {
            markAsDirty();
        }
        if (newValue != null) {
            var field = getReadOnlyVar();
            assert field.getManagedVar() != null;
            var newUid = field.getManagedVar().serializeUid(newValue);
            if (newUid.equals(oldUid)) {
                readOnlyUpdate();
            } else {
                markAsDirty();
                oldUid = newUid;
                updateRefs(newValue);
            }
        } else {
            oldUid = null;
            updateRefs(null);
        }
    }

    @Override
    public void readOnlyUpdate() {
        assert refs != null;
        for (ReadOnlyRef<TYPE> ref : refs) {
            ref.update();
            if (ref.isSyncDirty() || ref.isPersistedDirty()) {
                markAsDirty();
            }
        }
    }

    public void updateRefs(@Nullable TYPE[] values) {
        if (values == null) {
            this.refs = null;
            return;
        }
        this.refs = new ReadOnlyRef[values.length];
        for (int i = 0; i < values.length; i++) {
            if (getAccessor().getChildAccessor() instanceof IReadOnlyAccessor<TYPE> childAccessor) {
                refs[i] = childAccessor.createReadOnlyRef(key, ReadOnlyVar.of(ArrayVar.of(values, i, getAccessor().getChildType()), null));
            } else {
                throw new IllegalArgumentException("Child accessor is not an instance of IDirectAccessor when creating DirectArrayRef");
            }
        }
    }

    @Override
    public void readReadOnlySyncToStream(RegistryFriendlyByteBuf buffer) {
        IArrayRef.super.readSyncToStream(buffer);
    }

    @Override
    public void writeReadOnlySyncFromStream(RegistryFriendlyByteBuf buffer) {
        IArrayRef.super.writeSyncFromStream(buffer);
    }
}
