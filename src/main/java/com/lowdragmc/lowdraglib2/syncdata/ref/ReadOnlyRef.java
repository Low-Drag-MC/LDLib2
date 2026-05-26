package com.lowdragmc.lowdraglib2.syncdata.ref;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.var.ReadOnlyVar;
import com.mojang.serialization.JavaOps;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

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
            // Always compute oldValueMark — readOnlyManagedUpdate() falls back to readOnlyUpdate()
            // when the UID is unchanged and the managedVar has no dirty checker, and that path
            // dereferences oldValueMark.
            this.oldValueMark = obtainValueMark(accessor, value);
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

    private static <T> Object obtainValueMark(IReadOnlyAccessor<T> accessor, T value) {
        return accessor instanceof IMarkFunction markFunction ?
                markFunction.obtainManagedMark(value) :
                accessor.readReadOnlyValue(Platform.getFrozenRegistry().createSerializationContext(JavaOps.INSTANCE), value);
    }

    public IReadOnlyAccessor<TYPE> getAccessor() {
        return (IReadOnlyAccessor<TYPE>) super.getAccessor();
    }

    @Override
    public void readOnlyManagedUpdate() {
        var prevUid = oldUid;
        super.readOnlyManagedUpdate();
        // Keep oldValueMark in sync with the current value when the UID changed; otherwise the
        // next readOnlyUpdate() would compare against a snapshot from before the structure change.
        if (!Objects.equals(prevUid, oldUid)) {
            var value = readRaw();
            oldValueMark = value == null ? null : obtainValueMark(getAccessor(), value);
        }
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
