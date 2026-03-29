package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.var.ReadOnlyVar;
import com.mojang.serialization.JavaOps;

import org.jetbrains.annotations.Nullable;
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
                        accessor.readReadOnlyValue(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), value);
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
            var newValueMark = accessor.readReadOnlyValue(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), value);
            if (!oldValueMark.equals(newValueMark)) {
                oldValueMark = newValueMark;
                markAsDirty();
            }
        }
    }
}
