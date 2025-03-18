package com.lowdragmc.lowdraglib.syncdata.managed;

import net.minecraft.nbt.CompoundTag;

/**
 * @author KilaBash
 * @date 2023/2/19
 * @implNote ReadOnlyManagedRef
 */
public class ReadOnlyManagedRef extends DirectRef {

    private boolean wasNull;
    private CompoundTag lastUid;

    ReadOnlyManagedRef(ReadOnlyDirectField field) {
        super(field);
        var current = getField().value();
        wasNull = current == null;
        if (current != null) {
            lastUid = getReadOnlyField().serializeUid(current);
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
            var newUid = getReadOnlyField().serializeUid(newValue);
            if (!newUid.equals(lastUid) || getReadOnlyField().isDirty(newValue)) {
                markAsDirty();
            }
            lastUid = newUid;
        }
    }
}
