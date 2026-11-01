package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchBlock;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.locks.LockSupport;

/**
 * Persistence of a block entity synced by the async thread, which runs on real time rather than on ticks.
 */
public final class AsyncPersistenceGameTest {
    private static final String CHANGES_FOUND_IN_ONE_PERIOD_MARK_THE_BLOCK_ENTITY_CHANGED_ONCE_PATH = "async_persistence_changes_found_in_one_period_mark_the_block_entity_changed_once";
    private static final String UNWATCHED_CHANGES_WAIT_UNSENT_BUT_ARE_PERSISTED_PATH = "async_persistence_unwatched_changes_wait_unsent_but_are_persisted";

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(CHANGES_FOUND_IN_ONE_PERIOD_MARK_THE_BLOCK_ENTITY_CHANGED_ONCE_PATH, AsyncPersistenceGameTest::changesFoundInOnePeriodMarkTheBlockEntityChangedOnce);
        SyncDataGameTests.registerFunction(UNWATCHED_CHANGES_WAIT_UNSENT_BUT_ARE_PERSISTED_PATH, AsyncPersistenceGameTest::unwatchedChangesWaitUnsentButArePersisted);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        // its own batch: these pace the server in real time, which would slow every test sharing it
        environment = event.registerEnvironment(LDLib2.id("syncdata_async_persistence"), new TestEnvironmentDefinition.AllOf());
        SyncDataGameTests.registerFunctionTest(event, CHANGES_FOUND_IN_ONE_PERIOD_MARK_THE_BLOCK_ENTITY_CHANGED_ONCE_PATH, SyncDataGameTests.functionKey(CHANGES_FOUND_IN_ONE_PERIOD_MARK_THE_BLOCK_ENTITY_CHANGED_ONCE_PATH), SyncDataGameTests.testData(environment, "empty", 6000));
        SyncDataGameTests.registerFunctionTest(event, UNWATCHED_CHANGES_WAIT_UNSENT_BUT_ARE_PERSISTED_PATH, SyncDataGameTests.functionKey(UNWATCHED_CHANGES_WAIT_UNSENT_BUT_ARE_PERSISTED_PATH), SyncDataGameTests.testData(environment, "empty", 6000));
    }

    private static final long PERIODS_NANOS = 250_000_000L;
    private static final long GIVE_UP_NANOS = 5_000_000_000L;
    private static final long TICK_NANOS = 2_000_000L;

    static void changesFoundInOnePeriodMarkTheBlockEntityChangedOnce(GameTestHelper helper) {
        // the "empty" template is a single block, keep the block inside it so the test area cleans it up
        var pos = BlockPos.ZERO;
        helper.setBlock(pos, SyncBenchBlock.BLOCK);
        var be = helper.getBlockEntity(pos, SyncBenchBlockEntity.class);
        var step = new int[1];
        var since = new long[]{System.nanoTime()};
        helper.onEachTick(() -> {
            long now = System.nanoTime();
            if (step[0] == 0 && now - since[0] >= PERIODS_NANOS) {
                be.setChangedCalls = 0;
                be.tier++;
                be.energy--;
                be.logic.progress++;
                be.logic.working = !be.logic.working;
                step[0] = 1;
                since[0] = now;
            } else if (step[0] == 1 && (be.setChangedCalls > 0 || now - since[0] >= GIVE_UP_NANOS)) {
                step[0] = 2;
                since[0] = now;
            } else if (step[0] == 2 && now - since[0] >= PERIODS_NANOS) {
                // normally found in one period, in two if a period starts between the writes
                if (be.setChangedCalls < 1 || be.setChangedCalls > 2) {
                    helper.fail("four persisted changes found by the async thread caused " + be.setChangedCalls + " setChanged calls");
                }
                be.setChangedCalls = 0;
                be.markDirty("tier");
                if (be.setChangedCalls != 1) {
                    helper.fail("markDirty on the server thread caused " + be.setChangedCalls + " setChanged calls right away");
                }
                helper.setBlock(pos, Blocks.AIR);
                helper.succeed();
                return;
            }
            long wait;
            while ((wait = now + TICK_NANOS - System.nanoTime()) > 0) {
                LockSupport.parkNanos(wait);
            }
        });
    }

    static void unwatchedChangesWaitUnsentButArePersisted(GameTestHelper helper) {
        var pos = BlockPos.ZERO;
        helper.setBlock(pos, SyncBenchBlock.BLOCK);
        var be = helper.getBlockEntity(pos, SyncBenchBlockEntity.class);
        var step = new int[1];
        var since = new long[]{System.nanoTime()};
        // a first change gets the chunk looked at, and found unwatched, since no player is in a gametest
        be.energy--;
        helper.onEachTick(() -> {
            long now = System.nanoTime();
            if (step[0] == 0 && now - since[0] >= PERIODS_NANOS) {
                be.setChangedCalls = 0;
                be.tier++;
                be.logic.progress++;
                step[0] = 1;
                since[0] = now;
            } else if (step[0] == 1 && now - since[0] >= 2 * PERIODS_NANOS) {
                // Persisted means setChanged reached the chunk: the counter, not chunk.isUnsaved(). Since 1.21.2 the
                // server saves a chunk eagerly the tick after it is marked (ChunkMap#saveChunksEagerly, once ten
                // seconds have passed since its last save), so the flag can already be clear again by now.
                if (be.setChangedCalls < 1) helper.fail("changes nobody watched were not persisted");
                if (be.setChangedCalls > 2) {
                    helper.fail("two changes, waiting unsent for ten periods, caused " + be.setChangedCalls + " setChanged calls");
                }
                for (var name : new String[]{"tier", "logic"}) {
                    if (!syncRef(be, name).isSyncDirty()) helper.fail(name + " was sent or dropped instead of waiting for a watcher");
                }
                helper.setBlock(pos, Blocks.AIR);
                helper.succeed();
                return;
            }
            long wait;
            while ((wait = now + TICK_NANOS - System.nanoTime()) > 0) {
                LockSupport.parkNanos(wait);
            }
        });
    }

    private static IRef<?> syncRef(SyncBenchBlockEntity be, String name) {
        for (var ref : be.getSyncStorage().getSyncFields()) {
            if (ref.getKey().getName().equals(name)) return ref;
        }
        throw new IllegalArgumentException(name);
    }
}
