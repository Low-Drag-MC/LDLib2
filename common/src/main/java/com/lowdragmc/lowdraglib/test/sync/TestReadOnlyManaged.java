package com.lowdragmc.lowdraglib2.test.sync;

import com.lowdragmc.lowdraglib2.syncdata.IManaged;
import com.lowdragmc.lowdraglib2.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib2.test.TestBlockEntity;
import lombok.Getter;
import lombok.Setter;

public class TestReadOnlyManaged implements IManaged {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(TestReadOnlyManaged.class);
    private final TestBlockEntity blockEntity;
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Getter @Setter
    private int value = 10;

    public TestReadOnlyManaged(TestBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        this.blockEntity.markAsDirty();
    }
}
