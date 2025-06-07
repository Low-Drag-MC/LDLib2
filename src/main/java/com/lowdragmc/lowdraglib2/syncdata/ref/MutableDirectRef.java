package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;
import com.mojang.serialization.JavaOps;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * MutableDirectRef represents a reference to a mutable value,
 * which is updated only when the value changes (no the same instance or have internal changes).
 * <br>
 * It will store the old value mark to compare with the new value mark every update.
 * Please implement {@link IMarkFunction} for the accessor.
 * If the {@link IMarkFunction} is not implemented, it will use codec to store the mark in a type of {@link com.mojang.serialization.JavaOps}
 */
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
                        accessor.readDirectVar(JavaOps.INSTANCE, field);
    }

    @Override
    public IDirectAccessor<TYPE> getAccessor() {
        return (IDirectAccessor<TYPE>)super.getAccessor();
    }

    @Override
    public void update() {
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
                oldValueMark = accessor.readDirectVar(JavaOps.INSTANCE, field);
                markAsDirty();
            } else {
                var newValueMark = accessor.readDirectVar(JavaOps.INSTANCE, field);
                if (!oldValueMark.equals(newValueMark)) {
                    oldValueMark = newValueMark;
                    markAsDirty();
                }
            }
        }
    }
}
