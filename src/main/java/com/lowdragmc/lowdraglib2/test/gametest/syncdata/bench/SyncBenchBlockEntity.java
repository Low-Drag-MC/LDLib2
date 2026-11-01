package com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench;

import com.lowdragmc.lowdraglib2.CommonProxy;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncProbeHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * The {@link SyncBenchProfiles#MIXED} field mix as a real block entity, with counters for the work the sync framework
 * causes on the server thread.
 */
public class SyncBenchBlockEntity extends BlockEntity implements ISyncPersistRPCBlockEntity {
    public static final AtomicLong SET_CHANGED_CALLS = new AtomicLong();
    public static final AtomicLong SET_CHANGED_NANOS = new AtomicLong();
    @Nullable
    public static volatile Consumer<SPacketAutoSyncBlockEntity> packetListener;

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    public volatile boolean syncEnabled = true;
    public int setChangedCalls;

    @Persisted
    @DescSynced
    public boolean working;
    @Persisted
    @DescSynced
    public boolean active;
    @Persisted
    @DescSynced
    public boolean allowOutput = true;
    @Persisted
    @DescSynced
    public int tier = 1;
    @Persisted
    @DescSynced
    public long energy = 100_000;
    @Persisted
    @DescSynced
    public Direction facing = Direction.NORTH;
    @Persisted
    @DescSynced
    public Direction outputFacing = Direction.SOUTH;
    @Persisted
    @DescSynced
    public SyncBenchProfiles.Mode mode = SyncBenchProfiles.Mode.AUTO;
    @Persisted
    @DescSynced
    public final ItemStacksResourceHandler importItems = new ItemStacksResourceHandler(9);
    @Persisted
    @DescSynced
    public final ItemStacksResourceHandler exportItems = new ItemStacksResourceHandler(9);
    @Persisted
    @DescSynced
    public final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, 16000);
    @Persisted
    @DescSynced
    public final SyncProbeHolder.Part logic = new SyncProbeHolder.Part(this::notifyPersistence);

    public SyncBenchBlockEntity(BlockPos pos, BlockState state) {
        super(CommonProxy.SYNC_BENCH_BE_TYPE.get(), pos, state);
        for (int i = 0; i < importItems.size(); i++) {
            importItems.set(i, ItemResource.of(Items.IRON_INGOT), 16 + i);
        }
        tank.set(0, FluidResource.of(Fluids.WATER), 8000);
    }

    public void work(long tick) {
        logic.progress = (int) (tick % logic.duration);
        working = true;
        energy -= 32;
        if (tick % 20 == 0) {
            exportItems.set(0, ItemResource.of(Items.IRON_INGOT), (int) (tick / 20 % 64) + 1);
        }
    }

    @Override
    public boolean isAsyncValid() {
        return syncEnabled && ISyncPersistRPCBlockEntity.super.isAsyncValid();
    }

    @Override
    public void setChanged() {
        long start = System.nanoTime();
        super.setChanged();
        SET_CHANGED_NANOS.addAndGet(System.nanoTime() - start);
        SET_CHANGED_CALLS.incrementAndGet();
        setChangedCalls++;
    }

    @Override
    public SPacketAutoSyncBlockEntity createSyncPacket(BitSet changed, byte[] data, CompoundTag extra) {
        var packet = ISyncPersistRPCBlockEntity.super.createSyncPacket(changed, data, extra);
        var listener = packetListener;
        if (listener != null) listener.accept(packet);
        return packet;
    }
}
