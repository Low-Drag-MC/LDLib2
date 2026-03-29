package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;
import com.mojang.serialization.JavaOps;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
@Getter
@SuppressWarnings("unchecked")
public final class MutableDirectRef<TYPE> extends DirectRef<TYPE> {
    private @Nullable Object oldValueMark;

    public MutableDirectRef(IVar<TYPE> field, ManagedKey key, IDirectAccessor<TYPE> accessor) {
        super(field, key, accessor);
        var value = field.value();
        oldValueMark = value == null ? null :
                accessor instanceof IMarkFunction markFunction ?
                markFunction.obtainManagedMark(getField().value()) :
                        accessor.readDirectVar(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), field);
    }

    @Override
    public IDirectAccessor<TYPE> getAccessor() {
        return (IDirectAccessor<TYPE>)super.getAccessor();
    }

    @Override
    protected void updateSync() {
        TYPE newValue = getField().value();
        if (newValue == null) {
            if (oldValueMark != null) {
                oldValueMark = null;
                markAsDirty();
            }
            return;
        }
        var accessor = getAccessor();
        if (accessor instanceof IMarkFunction markFunction) {
            if (oldValueMark == null || markFunction.areDifferent(oldValueMark, newValue)) {
                oldValueMark = markFunction.obtainManagedMark(newValue);
                markAsDirty();
            }
        } else if (accessor instanceof IDirectAccessor) {
            if (oldValueMark == null) {
                oldValueMark = accessor.readDirectVar(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), field);
                markAsDirty();
            } else {
                var newValueMark = accessor.readDirectVar(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), field);
                if (!oldValueMark.equals(newValueMark)) {
                    oldValueMark = newValueMark;
                    markAsDirty();
                }
            }
        }
    }
}
