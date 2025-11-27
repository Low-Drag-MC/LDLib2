package com.lowdragmc.lowdraglib2.syncdata.blockentity;

import com.lowdragmc.lowdraglib2.syncdata.IBlockEntityManaged;
import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;

public interface IAsyncAutoSyncPersistBlockEntity extends IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity, IBlockEntityManaged {
    @Override
    default IManagedStorage getRootStorage() {
        return getSyncStorage();
    }
}
