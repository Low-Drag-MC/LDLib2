package com.lowdragmc.lowdraglib.test.sync;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.test.TestBlockEntity;
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
