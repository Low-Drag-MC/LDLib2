package com.lowdragmc.lowdraglib.syncdata.ref;

import com.lowdragmc.lowdraglib.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib.syncdata.var.ReadOnlyVar;
import com.mojang.serialization.JavaOps;

import javax.annotation.Nullable;

/**
 * ReadonlyRef represents a reference to a nonnull value, the value is readonly and the instance won't change.
 *  <br>
 *  It will store the old value mark to compare with the new value mark every update.
 *  Please implement {@link IMarkFunction} for the accessor.
 *  If the {@link IMarkFunction} is not implemented, it will use codec to store the mark in a type of {@link com.mojang.serialization.JavaOps}
 */
@SuppressWarnings("unchecked")
public class ReadOnlyRef<TYPE> extends ReadOnlyManagedRef<TYPE> {
    private @Nullable Object oldValueMark;

    public ReadOnlyRef(ReadOnlyVar<TYPE> field, ManagedKey key, IReadOnlyAccessor<TYPE> accessor) {
        super(field, key, accessor);
        var value = field.value();
        if (value != null) {
            if (!isReadOnlyManaged()) {
                this.oldValueMark = accessor instanceof IMarkFunction markFunction ?
                        markFunction.obtainManagedMark(value) :
                        accessor.readReadOnlyValue(JavaOps.INSTANCE, value);
            }
            if (isReadOnlyManaged()) {
                assert field.getManagedVar() != null;
                oldUid = field.getManagedVar().serializeUid(value);
            }
        } else {
            if (!isReadOnlyManaged()) {
                throw new IllegalStateException("The read only value is null, it should not be null!");
            } else {
                oldValueMark = null;
                oldUid = null;
            }
        }
    }

    public IReadOnlyAccessor<TYPE> getAccessor() {
        return (IReadOnlyAccessor<TYPE>) super.getAccessor();
    }

    public void readOnlyUpdate() {
        var value = readRaw();
        if (value == null) {
            throw new IllegalStateException("The read only value is null, it should not be null!");
        }
        var accessor = getAccessor();
        if (accessor instanceof IMarkFunction markFunction) {
            if (markFunction.areDifferent(oldValueMark, value)) {
                oldValueMark = markFunction.obtainManagedMark(value);
                markAsDirty();
            }
        } else {
            var newValueMark = accessor.readReadOnlyValue(JavaOps.INSTANCE, value);
            if (!oldValueMark.equals(newValueMark)) {
                oldValueMark = newValueMark;
                markAsDirty();
            }
        }
    }
}
