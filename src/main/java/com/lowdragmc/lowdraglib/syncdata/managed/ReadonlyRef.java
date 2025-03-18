package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IReadOnlyAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.mojang.serialization.JavaOps;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;

/**
 * ReadonlyRef represents a reference to a nonnull value, the value is readonly and the instance won't change.
 *  <br>
 *  It will store the old value mark to compare with the new value mark every update.
 *  Please implement {@link IMarkFunction} for the accessor.
 *  If the {@link IMarkFunction} is not implemented, it will use codec to store the mark in a type of {@link com.mojang.serialization.JavaOps}
 */
@SuppressWarnings("unchecked")
public class ReadonlyRef<TYPE> extends Ref {
    @Getter
    protected final WeakReference<TYPE> reference;
    private @Nonnull Object oldValueMark;

    public ReadonlyRef(TYPE value, ManagedKey key, IReadOnlyAccessor<TYPE> accessor) {
        super(key, accessor);
        this.reference = new WeakReference<>(value);
        this.oldValueMark = accessor instanceof IMarkFunction markFunction ?
                markFunction.obtainManagedMark(value) :
                accessor.readReadOnlyValue(JavaOps.INSTANCE, value);
    }

    @Override
    public IReadOnlyAccessor<TYPE> getAccessor() {
        return (IReadOnlyAccessor<TYPE>) super.getAccessor();
    }

    @Override
    public TYPE readRaw() {
        return this.reference.get();
    }

    @Override
    public void update() {
        var value = reference.get();
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
