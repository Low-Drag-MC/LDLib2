package com.lowdragmc.lowdraglib2.syncdata.holder;

import com.lowdragmc.lowdraglib2.syncdata.storage.IManagedStorage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public interface IManagedHolder {
    ServerLevel getServerLevel();

    ChunkPos getTrackingPos();

    IManagedStorage getRootStorage();
}
