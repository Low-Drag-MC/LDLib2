package com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.async.IAsyncLogic;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntities;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncDataGameTests;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.SyncProbeHolder;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchProfiles.Profile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;

/**
 * Measures what the auto sync costs today. Off unless the game runs with {@code -Dldlib2.syncBench=true}
 * ({@code gradlew runGameTestServer -PldSyncBench}); the report lands in {@code <game dir>/ldlib2-syncbench/report.md}.
 */
public final class SyncBenchmarkGameTest {
    private static final String CHANGE_DETECTION_AND_SENDING_PATH = "sync_bench_change_detection_and_sending";
    private static final String LIVE_BLOCK_ENTITIES_PATH = "sync_bench_live_block_entities";
    private static final String WIRE_SIZE_PATH = "sync_bench_wire_size";
    private static final String ASYNC_ATTACH_AND_DETACH_PATH = "sync_bench_async_attach_and_detach";

    public static void registerFunctions() {
        if (!ENABLED) return;
        SyncDataGameTests.registerFunction(CHANGE_DETECTION_AND_SENDING_PATH, SyncBenchmarkGameTest::changeDetectionAndSending);
        SyncDataGameTests.registerFunction(LIVE_BLOCK_ENTITIES_PATH, SyncBenchmarkGameTest::liveBlockEntities);
        SyncDataGameTests.registerFunction(WIRE_SIZE_PATH, SyncBenchmarkGameTest::wireSize);
        SyncDataGameTests.registerFunction(ASYNC_ATTACH_AND_DETACH_PATH, SyncBenchmarkGameTest::asyncAttachAndDetach);
    }

    /**
     * Registered only with {@code -Dldlib2.syncBench=true}, each in an environment of its own so it runs as a batch of
     * its own: the macro run fills a 16x8x16 grid next to its template and paces the server in real time, which would
     * disturb any test sharing its batch.
     */
    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        if (!ENABLED) return;
        registerAlone(event, CHANGE_DETECTION_AND_SENDING_PATH, "syncbench_1_micro", 20);
        registerAlone(event, LIVE_BLOCK_ENTITIES_PATH, "syncbench_2_macro", 1200);
        registerAlone(event, ASYNC_ATTACH_AND_DETACH_PATH, "syncbench_3_lifecycle", 20);
        registerAlone(event, WIRE_SIZE_PATH, "syncbench_4_wire", 20);
    }

    private static void registerAlone(RegisterGameTestsEvent event, String path, String batch, int maxTicks) {
        var environment = event.registerEnvironment(LDLib2.id(batch), new TestEnvironmentDefinition.AllOf());
        SyncDataGameTests.registerFunctionTest(event, path, SyncDataGameTests.functionKey(path), SyncDataGameTests.testData(environment, "empty", maxTicks));
    }

    private static final boolean ENABLED = Boolean.getBoolean("ldlib2.syncBench");
    private static final int HOLDERS = Integer.getInteger("ldlib2.syncBench.holders", 2000);
    private static final long WARMUP_NANOS = 1_500_000_000L;
    private static final long ROUND_NANOS = 250_000_000L;
    private static final int ROUNDS = 7;

    private static final int GRID_X = 16, GRID_Y = 8, GRID_Z = 16;
    private static final int ACTIVE_EVERY = 4;
    private static final long TICK_NANOS = 50_000_000L;

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    static void changeDetectionAndSending(GameTestHelper helper) {
        if (!ENABLED) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var rows = new ArrayList<SyncBenchReport.MicroRow>();
        for (var profile : SyncBenchProfiles.ALL) {
            rows.add(measure(profile, level, false));
            rows.add(measure(profile, level, true));
            SyncBenchReport.micro(HOLDERS, rows);
        }
        helper.succeed();
    }

    private static <H extends SyncProbeHolder> SyncBenchReport.MicroRow measure(Profile<H> profile, ServerLevel level, boolean active) {
        // class loading and first-use caches stay out of the init timing
        for (int i = 0; i < 50; i++) {
            profile.create(level).getRootStorage().getSyncFields();
        }
        var holders = new ArrayList<H>(HOLDERS);
        long initStart = System.nanoTime();
        for (int i = 0; i < HOLDERS; i++) {
            var holder = profile.create(level);
            holder.getRootStorage().getSyncFields();
            holders.add(holder);
        }
        long initNanos = System.nanoTime() - initStart;
        holders.forEach(SyncProbeHolder::settle);

        var period = new int[1];
        Runnable runPeriod = () -> {
            int p = period[0]++;
            for (var holder : holders) {
                if (active) profile.work().apply(holder, p);
                holder.sync(false);
            }
        };
        long warmupEnd = System.nanoTime() + WARMUP_NANOS;
        do {
            runPeriod.run();
        } while (System.nanoTime() < warmupEnd);

        for (var holder : holders) {
            holder.packets = 0;
            holder.payloadBytes = 0;
            holder.persistenceNotices = 0;
        }
        var nanos = new double[ROUNDS];
        var allocs = new double[ROUNDS];
        long periods = 0;
        for (int round = 0; round < ROUNDS; round++) {
            long alloc = allocatedBytes();
            long start = System.nanoTime();
            int roundPeriods = 0;
            do {
                runPeriod.run();
                roundPeriods++;
            } while (System.nanoTime() - start < ROUND_NANOS);
            double holderPeriods = (double) roundPeriods * HOLDERS;
            nanos[round] = (System.nanoTime() - start) / holderPeriods;
            allocs[round] = (allocatedBytes() - alloc) / holderPeriods;
            periods += roundPeriods;
        }
        long packets = 0, bytes = 0, notices = 0;
        for (var holder : holders) {
            packets += holder.packets;
            bytes += holder.payloadBytes;
            notices += holder.persistenceNotices;
        }
        double holderPeriods = (double) periods * HOLDERS;
        Arrays.sort(nanos);
        return new SyncBenchReport.MicroRow(profile.name(), profile.fields(), active,
                median(nanos), nanos[0], nanos[ROUNDS - 1], median(allocs),
                packets / holderPeriods, packets == 0 ? 0 : (double) bytes / packets, notices / holderPeriods,
                initNanos / 1000.0 / HOLDERS);
    }

    static void liveBlockEntities(GameTestHelper helper) {
        if (!ENABLED) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var positions = new ArrayList<BlockPos>();
        for (int x = 0; x < GRID_X; x++) {
            for (int y = 0; y < GRID_Y; y++) {
                for (int z = 0; z < GRID_Z; z++) {
                    positions.add(helper.absolutePos(new BlockPos(x + 2, y + 2, z + 2)));
                }
            }
        }

        long placeStart = System.nanoTime();
        for (var pos : positions) {
            level.setBlock(pos, SyncBenchBlock.BLOCK.defaultBlockState(), 3);
        }
        long placeNanos = System.nanoTime() - placeStart;
        var all = new ArrayList<SyncBenchBlockEntity>();
        var active = new ArrayList<SyncBenchBlockEntity>();
        for (int i = 0; i < positions.size(); i++) {
            if (level.getBlockEntity(positions.get(i)) instanceof SyncBenchBlockEntity be) {
                all.add(be);
                if (i % ACTIVE_EVERY == 0) active.add(be);
            }
        }

        var run = new MacroRun(AsyncThreadData.getOrCreate(level), all);
        var nextTick = new long[]{System.nanoTime()};
        helper.onEachTick(() -> {
            for (var be : active) {
                be.work(run.tick);
            }
            if (run.onTick()) {
                var lines = run.report(positions.size(), active.size(), placeNanos);
                long removeStart = System.nanoTime();
                for (var pos : positions) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
                lines.add("removing all " + positions.size() + " block entities: " + ms(System.nanoTime() - removeStart) + " ms");
                SyncBenchReport.section("macro", "Macro: live block entities with the async thread", lines);
                helper.succeed();
                return;
            }
            // the gametest server ticks as fast as it can; hold it to 20 ticks per second like a real one.
            // Loop because every task the async thread hands over unparks the server thread
            nextTick[0] += TICK_NANOS;
            if (nextTick[0] < System.nanoTime()) {
                nextTick[0] = System.nanoTime();
            }
            long wait;
            while ((wait = nextTick[0] - System.nanoTime()) > 0) {
                LockSupport.parkNanos(wait);
            }
        });
    }

    /**
     * Alternates sync off and on under the same workload, so the server thread's cost of syncing is the difference
     * between the two, averaged over both halves of the run.
     */
    private static final class MacroRun {
        // sync stays on through the warmup so its code is compiled before anything is measured
        private static final int[] PHASE_TICKS = {60, 20, 100, 20, 100, 20, 100, 20, 100};
        private static final boolean[] PHASE_SYNC = {true, false, false, true, true, false, false, true, true};
        private static final boolean[] PHASE_MEASURED = {false, false, true, false, true, false, true, false, true};

        private final AsyncThreadData asyncData;
        private final List<SyncBenchBlockEntity> blockEntities;
        private long tick;
        private int phase = -1;
        private long phaseEnd;

        private long cpuStart, timeStart, periodStart, setChangedStart, setChangedNanosStart;
        private Map<Long, Long> asyncStart = Map.of();
        private Map<Long, Long> asyncAllocStart = Map.of();
        private double onSeconds, offSeconds;
        private long onCpu, offCpu, asyncNanos, asyncAlloc, periods, setChanged, setChangedNanos;

        private MacroRun(AsyncThreadData asyncData, List<SyncBenchBlockEntity> blockEntities) {
            this.asyncData = asyncData;
            this.blockEntities = blockEntities;
        }

        /**
         * @return whether every phase has run
         */
        private boolean onTick() {
            if (tick == phaseEnd) {
                if (phase >= 0 && PHASE_MEASURED[phase]) finishPhase();
                phase++;
                if (phase == PHASE_TICKS.length) return true;
                for (var be : blockEntities) {
                    be.syncEnabled = PHASE_SYNC[phase];
                }
                phaseEnd = tick + PHASE_TICKS[phase];
                if (PHASE_MEASURED[phase]) startPhase();
            }
            tick++;
            return false;
        }

        private void startPhase() {
            asyncStart = asyncThreadCpu();
            asyncAllocStart = asyncThreadAllocation();
            periodStart = asyncData.getPeriodID();
            setChangedStart = SyncBenchBlockEntity.SET_CHANGED_CALLS.get();
            setChangedNanosStart = SyncBenchBlockEntity.SET_CHANGED_NANOS.get();
            timeStart = System.nanoTime();
            cpuStart = THREADS.getCurrentThreadCpuTime();
        }

        private void finishPhase() {
            long cpu = THREADS.getCurrentThreadCpuTime() - cpuStart;
            double seconds = (System.nanoTime() - timeStart) / 1e9;
            if (!PHASE_SYNC[phase]) {
                offCpu += cpu;
                offSeconds += seconds;
                return;
            }
            onCpu += cpu;
            onSeconds += seconds;
            for (var entry : asyncThreadAllocation().entrySet()) {
                asyncAlloc += entry.getValue() - asyncAllocStart.getOrDefault(entry.getKey(), 0L);
            }
            for (var entry : asyncThreadCpu().entrySet()) {
                asyncNanos += entry.getValue() - asyncStart.getOrDefault(entry.getKey(), 0L);
            }
            periods += asyncData.getPeriodID() - periodStart;
            setChanged += SyncBenchBlockEntity.SET_CHANGED_CALLS.get() - setChangedStart;
            setChangedNanos += SyncBenchBlockEntity.SET_CHANGED_NANOS.get() - setChangedNanosStart;
        }

        private List<String> report(int total, int active, long placeNanos) {
            var lines = new ArrayList<String>();
            double syncNanosPerSecond = onCpu / onSeconds - offCpu / offSeconds;

            lines.add(total + " block entities in a " + GRID_X + "x" + GRID_Y + "x" + GRID_Z + " block, every " + ACTIVE_EVERY
                    + "th one working (" + active + "): progress and energy change every tick, an output slot every 20 ticks. "
                    + "No player is watching, so this is what loaded but unwatched machines cost");
            lines.add("placing them: " + ms(placeNanos) + " ms (" + SyncBenchReport.fmt(placeNanos / 1000.0 / total)
                    + " us each, includes storage init and attaching to the async thread)");
            lines.add("measured " + SyncBenchReport.fmt(onSeconds) + " s with sync on and " + SyncBenchReport.fmt(offSeconds)
                    + " s with it off, alternating, at 20 ticks per second");
            lines.add("async thread: " + SyncBenchReport.fmt(asyncNanos / 1e7 / onSeconds) + "% of a core; "
                    + SyncBenchReport.fmt(periods / onSeconds) + " periods per second (target 20); "
                    + SyncBenchReport.fmt(asyncNanos / 1e3 / Math.max(1, periods) / total) + " us per block entity per period; allocates "
                    + SyncBenchReport.fmt(asyncAlloc / 1e6 / onSeconds) + " MB per second");
            lines.add("server thread: " + SyncBenchReport.fmt(onCpu / 1e6 / onSeconds) + " ms CPU per second with sync, "
                    + SyncBenchReport.fmt(offCpu / 1e6 / offSeconds) + " without = " + SyncBenchReport.fmt(syncNanosPerSecond / 1e6)
                    + " ms per second spent on sync (" + SyncBenchReport.fmt(syncNanosPerSecond / 1e7) + "% of the tick budget)");
            lines.add("setChanged from persistence notices: " + SyncBenchReport.fmt(setChanged / onSeconds) + " per second, "
                    + SyncBenchReport.fmt(setChanged == 0 ? 0 : setChangedNanos / 1000.0 / setChanged) + " us each, "
                    + SyncBenchReport.fmt(setChangedNanos / 1e6 / onSeconds) + " ms per second; "
                    + SyncBenchReport.fmt(setChanged / onSeconds / active) + " per working block entity per second");
            return lines;
        }
    }

    static void wireSize(GameTestHelper helper) {
        if (!ENABLED) {
            helper.succeed();
            return;
        }
        var level = helper.getLevel();
        var lines = new ArrayList<String>();
        lines.add("bytes per block entity update before compression, which the game applies to packets of 256 bytes and more; "
                + "framing counted as packet id, channel id, frame length and compression marker");
        for (int perChunk : new int[]{1, 8, 64}) {
            for (boolean container : new boolean[]{false, true}) {
                var updates = captureUpdates(helper, perChunk, container);
                long single = 0;
                for (var packet : updates) {
                    single += wireBytes(level, SPacketAutoSyncBlockEntity.CODEC, packet, SPacketAutoSyncBlockEntity.ID);
                }
                long batched = 0;
                var entries = updates.stream().map(SPacketAutoSyncBlockEntities.Entry::of).toList();
                for (var packet : SPacketAutoSyncBlockEntities.pack(entries, 256 * 1024)) {
                    batched += wireBytes(level, SPacketAutoSyncBlockEntities.CODEC, packet, SPacketAutoSyncBlockEntities.ID);
                }
                lines.add(perChunk + " updates in a chunk, " + (container ? "progress, energy and an output slot" : "progress and energy")
                        + ": " + SyncBenchReport.fmt(single / (double) updates.size()) + " B each as single packets, "
                        + SyncBenchReport.fmt(batched / (double) updates.size()) + " B batched ("
                        + SyncBenchReport.fmt(single / (double) batched) + "x smaller)");
            }
        }
        SyncBenchReport.section("wire", "Wire size of sync packets", lines);
        helper.succeed();
    }

    private static List<SPacketAutoSyncBlockEntity> captureUpdates(GameTestHelper helper, int count, boolean container) {
        var level = helper.getLevel();
        var updates = new ArrayList<SPacketAutoSyncBlockEntity>();
        SyncBenchBlockEntity.packetListener = updates::add;
        try {
            for (int i = 0; i < count; i++) {
                var be = new SyncBenchBlockEntity(helper.absolutePos(new BlockPos(i % 16, 10 + i / 16, 0)), SyncBenchBlock.BLOCK.defaultBlockState());
                be.setLevel(level);
                for (var ref : be.getSyncStorage().getSyncFields()) {
                    ref.clearSyncDirty();
                }
                be.work(container ? 20 : 1);
                // off the async thread, so the update is built and handed over right away
                be.sync(false);
            }
        } finally {
            SyncBenchBlockEntity.packetListener = null;
        }
        return updates;
    }

    private static <T> long wireBytes(ServerLevel level, StreamCodec<RegistryFriendlyByteBuf, T> codec, T payload, Identifier channel) {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            codec.encode(buf, payload);
            int id = channel.toString().length();
            int packet = 1 + VarInt.getByteSize(id) + id + buf.readableBytes();
            return VarInt.getByteSize(packet) + 1 + packet;
        } finally {
            buf.release();
        }
    }

    static void asyncAttachAndDetach(GameTestHelper helper) {
        if (!ENABLED) {
            helper.succeed();
            return;
        }
        var data = AsyncThreadData.getOrCreate(helper.getLevel());
        var lines = new ArrayList<String>();
        for (int count : new int[]{2_000, 20_000}) {
            var logics = new ArrayList<IAsyncLogic>(count);
            for (int i = 0; i < count; i++) {
                logics.add(new NoopLogic(i));
            }
            long add = timed(() -> logics.forEach(data::addAsyncLogic));
            Collections.shuffle(logics, new Random(42));
            long remove = timed(() -> logics.forEach(data::removeAsyncLogic));
            lines.add(count + " holders: attach " + ms(add) + " ms (" + SyncBenchReport.fmt(add / 1000.0 / count)
                    + " us each), detach in random order " + ms(remove) + " ms (" + SyncBenchReport.fmt(remove / 1000.0 / count) + " us each)");
        }
        SyncBenchReport.section("lifecycle", "Lifecycle: attaching to and detaching from the async thread", lines);
        helper.succeed();
    }

    private record NoopLogic(int id) implements IAsyncLogic {
        @Override
        public void asyncTick(long periodID) {
        }
    }

    private static long timed(Runnable task) {
        long start = System.nanoTime();
        task.run();
        return System.nanoTime() - start;
    }

    private static Map<Long, Long> asyncThreadAllocation() {
        var result = new HashMap<Long, Long>();
        if (!(THREADS instanceof com.sun.management.ThreadMXBean sun)) return result;
        for (var thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().startsWith("LDLib Async Thread")) {
                long bytes = sun.getThreadAllocatedBytes(thread.threadId());
                if (bytes >= 0) result.put(thread.threadId(), bytes);
            }
        }
        return result;
    }

    private static Map<Long, Long> asyncThreadCpu() {
        var result = new HashMap<Long, Long>();
        for (var thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().startsWith("LDLib Async Thread")) {
                long cpu = THREADS.getThreadCpuTime(thread.threadId());
                if (cpu >= 0) result.put(thread.threadId(), cpu);
            }
        }
        return result;
    }

    private static long allocatedBytes() {
        return THREADS instanceof com.sun.management.ThreadMXBean sun ? sun.getCurrentThreadAllocatedBytes() : 0;
    }

    private static double median(double[] values) {
        var sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static String ms(long nanos) {
        return SyncBenchReport.fmt(nanos / 1e6);
    }
}
