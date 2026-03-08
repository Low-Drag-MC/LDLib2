package com.lowdragmc.lowdraglib2.nodegraphtookit.model;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class Model implements IPersistedSerializable {
    @Persisted
    protected @Nullable UUID uid;

    public Model() {
        uid = UUID.randomUUID();
    }

    public Model(@Nullable UUID uid) {
        this.uid = uid;
    }

    public UUID getUid() {
        if (uid == null) uid = UUID.randomUUID();
        return uid;
    }

    public void setUid(@Nullable UUID uuid) {
        this.uid = uuid;
    }

}
