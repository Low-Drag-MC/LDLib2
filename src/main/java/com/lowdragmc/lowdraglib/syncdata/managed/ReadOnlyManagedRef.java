package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.accessor.IDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import net.minecraft.nbt.CompoundTag;

/**
 * @author KilaBash
 * @date 2023/2/19
 * @implNote ReadOnlyManagedRef
 */
public class ReadOnlyManagedRef extends DirectRef<ReadOnlyDirectField<?>> {

    private boolean wasNull;
    private CompoundTag lastUid;

    public ReadOnlyManagedRef(ReadOnlyDirectField<?> field, ManagedKey key, IDirectAccessor<ReadOnlyDirectField<?>> accessor) {
        super(field, key, accessor);
        var current = getField().value();
        wasNull = current == null;
        if (current != null) {
            lastUid = getField().serializeUid(current);
        }
    }

    public ReadOnlyDirectField getReadOnlyField() {
        return ((ReadOnlyDirectField)field);
    }

    @Override
    public void update() {
        Object newValue = getField().value();
        if ((wasNull && newValue != null) || (!wasNull && newValue == null)) {
            markAsDirty();
        }
        wasNull = newValue == null;
        if (newValue != null) {
            var newUid = getField().serializeUid(newValue);
            if (!newUid.equals(lastUid) || getField().isDirty(newValue)) {
                markAsDirty();
            }
            lastUid = newUid;
        }
    }
}
