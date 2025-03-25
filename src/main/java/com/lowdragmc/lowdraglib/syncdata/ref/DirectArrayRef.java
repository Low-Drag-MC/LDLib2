package com.lowdragmc.lowdraglib.syncdata.ref;

import com.lowdragmc.lowdraglib.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.arraylike.IArrayLikeAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.var.ArrayVar;
import com.lowdragmc.lowdraglib.syncdata.var.IVar;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Objects;

public final class DirectArrayRef<TYPE, TYPE_ARRAY> extends UniqueDirectRef<TYPE_ARRAY> implements IArrayRef<TYPE, TYPE_ARRAY> {
    @Getter
    @Nullable
    private DirectRef<TYPE>[] refs;

    private DirectArrayRef(IVar<TYPE_ARRAY> field, ManagedKey key, IArrayLikeAccessor<TYPE, TYPE_ARRAY> accessor) {
        super(field, key, accessor);
        updateRefs(field.value());
    }

    public static <TYPE, TYPE_ARRAY> DirectArrayRef<TYPE, TYPE_ARRAY> of(IVar<TYPE_ARRAY> field, ManagedKey key, IArrayLikeAccessor<TYPE, TYPE_ARRAY> accessor) {
        return new DirectArrayRef<>(field, key, accessor);
    }

    @Override
    public IArrayLikeAccessor<TYPE, TYPE_ARRAY> getAccessor() {
        return (IArrayLikeAccessor<TYPE, TYPE_ARRAY>) super.getAccessor();
    }

    @Override
    public void clearSyncDirty() {
        super.clearSyncDirty();
        if (refs == null) {
            return;
        }
        for (DirectRef<TYPE> ref : refs) {
            ref.clearSyncDirty();
        }
    }

    @Override
    public void clearPersistedDirty() {
        super.clearPersistedDirty();
        if (refs == null) {
            return;
        }
        for (var ref : refs) {
            ref.clearPersistedDirty();
        }
    }

    @Override
    public void update() {
        var newValue = readRaw();
        if (!Objects.equals(oldValue, newValue)) {
            oldValue = newValue;
            updateRefs(newValue);
            if (refs != null) {
                for (var ref : refs) {
                    ref.markAsDirty();
                }
            }
        } else if (refs != null) {
            for (var ref : refs) {
                ref.update();
                if (ref.isSyncDirty() || ref.isPersistedDirty()) {
                    markAsDirty();
                }
            }
        }
    }

    public void updateRefs(@Nullable TYPE_ARRAY values) {
        if (values == null) {
            refs = null;
            return;
        }
        var length = length(values);
        refs = new DirectRef[length];
        for (int i = 0; i < length; i++) {
            if (getAccessor().getChildAccessor() instanceof IDirectAccessor<TYPE> childAccessor) {
                refs[i] = childAccessor.createDirectRef(key, ArrayVar.of(values, i, getAccessor().getChildType()));
            } else {
                throw new IllegalArgumentException("Child accessor is not an instance of IDirectAccessor when creating DirectArrayRef");
            }
        }
    }

}
