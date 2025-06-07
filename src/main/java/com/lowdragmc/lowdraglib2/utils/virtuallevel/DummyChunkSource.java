package com.lowdragmc.lowdraglib2.utils.virtuallevel;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

public class DummyChunkSource extends ChunkSource {
    private final DummyWorld world;
    private final Long2ObjectMap<VirtualChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final LevelLightEngine lightEngine;

    public DummyChunkSource(DummyWorld world) {
        this.world = world;
        this.lightEngine = new LevelLightEngine(this, true, true);
    }

    @Override
    public void tick(BooleanSupplier booleanSupplier, boolean p_202163_) {

    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int pChunkX, int pChunkZ, ChunkStatus pRequiredStatus, boolean pLoad) {
        return getChunk(pChunkX, pChunkZ);
    }

    public ChunkAccess getChunk(int x, int z) {
        long pos = ChunkPos.asLong(x, z);
        return chunks.computeIfAbsent(pos, $ -> new VirtualChunk(world, x, z));
    }

    @Override
    @Nonnull
    public String gatherStats() {
        return "Dummy";
    }

    @Override
    public int getLoadedChunksCount() {
        return 0;
    }

    @Override
    @Nonnull
    public LevelLightEngine getLightEngine() {
        return lightEngine;
    }

    @Override
    @Nonnull
    public BlockGetter getLevel() {
        return world;
    }

}
