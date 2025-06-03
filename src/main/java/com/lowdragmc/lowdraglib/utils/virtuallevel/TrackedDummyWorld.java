package com.lowdragmc.lowdraglib.utils.virtuallevel;

import com.lowdragmc.lowdraglib.client.scene.ParticleManager;
import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Author: KilaBash
 * Date: 2021/08/25
 * Description: TrackedDummyWorld. Used to build a Fake World.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TrackedDummyWorld extends DummyWorld {
    @Setter
    private Predicate<BlockPos> blockFilter;
    public final WeakReference<Level> proxyWorld;

    public TrackedDummyWorld() {
        proxyWorld = new WeakReference<>(null);
    }

    public TrackedDummyWorld(Level world) {
        super(world.registryAccess());
        proxyWorld = new WeakReference<>(world);
    }

    public void clear() {
        for (var pos : new ArrayList<>(filledBlocks)) {
            removeBlock(BlockPos.of(pos));
        }
        for (Entity entity : super.getEntities().getAll()) {
            removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    public void addBlocks(Map<BlockPos, BlockInfo> renderedBlocks) {
        renderedBlocks.forEach(this::addBlock);
    }

    public void addBlock(BlockPos pos, BlockInfo blockInfo) {
        if (blockInfo.getBlockState().getBlock() == Blocks.AIR)
            return;
        setBlockAndUpdate(pos, blockInfo.getBlockState());
        if (blockInfo.hasBlockEntity()) {
            blockInfo.postEntity(getBlockEntity(pos));
        }
    }

    public void removeBlock(BlockPos pos) {
        if (isFilledBlock(pos)) {
            setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(@Nonnull BlockPos pos) {
        if (blockFilter != null && !blockFilter.test(pos))
            return null;
        Level proxy = proxyWorld.get();
        return proxy != null ? proxy.getBlockEntity(pos) : super.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(@Nonnull BlockPos pos) {
        if (blockFilter != null && !blockFilter.test(pos))
            return Blocks.AIR.defaultBlockState(); //return air if not rendering this
        Level proxy = proxyWorld.get();
        return proxy != null ? proxy.getBlockState(pos) : super.getBlockState(pos);
    }

    @Override
    protected LevelEntityGetter<Entity> getEntities() {
        return proxyWorld.get() instanceof DummyWorld dummyWorld ? dummyWorld.getEntities() : super.getEntities();
    }

    @Override
    public ChunkSource getChunkSource() {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getChunkSource() : proxy.getChunkSource();
    }

    @Override
    public FluidState getFluidState(BlockPos pPos) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getFluidState(pPos) : proxy.getFluidState(pPos);
    }

    @Override
    public int getBlockTint(@Nonnull BlockPos blockPos, @Nonnull ColorResolver colorResolver) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getBlockTint(blockPos, colorResolver) : proxy.getBlockTint(blockPos, colorResolver);
    }

    @Nonnull
    @Override
    public Holder<Biome> getBiome(@Nonnull BlockPos pos) {
        Level proxy = proxyWorld.get();
        return proxy == null ? super.getBiome(pos) : proxy.getBiome(pos);
    }

    @Override
    public void setParticleManager(ParticleManager particleManager) {
        super.setParticleManager(particleManager);
        if (proxyWorld.get() instanceof DummyWorld dummyWorld) {
            dummyWorld.setParticleManager(particleManager);
        }
    }

    @Nullable
    @Override
    public ParticleManager getParticleManager() {
        ParticleManager particleManager = super.getParticleManager();
        if (particleManager == null && proxyWorld.get() instanceof DummyWorld dummyWorld) {
            return dummyWorld.getParticleManager();
        }
        return particleManager;
    }

    public Iterable<Entity> getAllRenderedEntities() {
        if (proxyWorld.get() instanceof TrackedDummyWorld dummyWorld) {
            return dummyWorld.getAllRenderedEntities();
        } else if (proxyWorld.get() != null) {
            // TODO entity box?
        }
        return getEntities().getAll();
    }
}
