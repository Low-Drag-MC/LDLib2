package com.lowdragmc.lowdraglib.utils.virtuallevel;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.function.BooleanSupplier;

public class DummyChunkSource extends ChunkSource {

    private final DummyWorld world;
    public final HashMap<Long, VirtualChunk> chunks = new HashMap<>();

    public DummyChunkSource(DummyWorld world) {
        this.world = world;
    }

    @Override
    public void tick(BooleanSupplier p_202162_, boolean p_202163_) {

    }

    @Override
    public LightChunk getChunkForLighting(int x, int z) {
        return getChunk(x, z);
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
        return world.getLightEngine();
    }

    @Override
    @Nonnull
    public BlockGetter getLevel() {
        return world;
    }

}
