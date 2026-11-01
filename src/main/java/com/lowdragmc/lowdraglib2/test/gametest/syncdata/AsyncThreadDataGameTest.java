package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.async.AsyncThreadData;
import com.lowdragmc.lowdraglib2.async.IAsyncLogic;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AsyncThreadDataGameTest {
    private static final String LOGIC_RUNS_ONCE_PER_ADD_UNTIL_REMOVED_AS_OFTEN_PATH = "async_thread_data_logic_runs_once_per_add_until_removed_as_often";
    private static final String FAILING_PERIOD_TASK_LEAVES_THE_OTHERS_AND_THE_NEXT_PERIODS_PATH = "async_thread_data_failing_period_task_leaves_the_others_and_the_next_periods";

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(LOGIC_RUNS_ONCE_PER_ADD_UNTIL_REMOVED_AS_OFTEN_PATH, AsyncThreadDataGameTest::logicRunsOncePerAddUntilRemovedAsOften);
        SyncDataGameTests.registerFunction(FAILING_PERIOD_TASK_LEAVES_THE_OTHERS_AND_THE_NEXT_PERIODS_PATH, AsyncThreadDataGameTest::failingPeriodTaskLeavesTheOthersAndTheNextPeriods);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        // its own batch: these pace the server in real time, which would slow every test sharing it
        environment = event.registerEnvironment(LDLib2.id("syncdata_async_thread"), new TestEnvironmentDefinition.AllOf());
        SyncDataGameTests.registerFunctionTest(event, LOGIC_RUNS_ONCE_PER_ADD_UNTIL_REMOVED_AS_OFTEN_PATH, SyncDataGameTests.functionKey(LOGIC_RUNS_ONCE_PER_ADD_UNTIL_REMOVED_AS_OFTEN_PATH), SyncDataGameTests.testData(environment, "empty", 12000));
        SyncDataGameTests.registerFunctionTest(event, FAILING_PERIOD_TASK_LEAVES_THE_OTHERS_AND_THE_NEXT_PERIODS_PATH, SyncDataGameTests.functionKey(FAILING_PERIOD_TASK_LEAVES_THE_OTHERS_AND_THE_NEXT_PERIODS_PATH), SyncDataGameTests.testData(environment, "empty", 12000));
    }

    private static final long PERIODS_NANOS = 250_000_000L;
    private static final long GIVE_UP_NANOS = 5_000_000_000L;
    private static final long TICK_NANOS = 2_000_000L;

    private static final class CountingLogic implements IAsyncLogic {
        private final List<Long> periods = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void asyncTick(long periodID) {
            periods.add(periodID);
        }

        private List<Long> snapshot() {
            synchronized (periods) {
                return List.copyOf(periods);
            }
        }
    }

    /**
     * Runs {@code steps} one after another in real time, each once its condition holds, pacing the gametest server so the
     * async thread gets periods in between.
     */
    private static void inRealTime(GameTestHelper helper, List<BooleanSupplier> steps) {
        var step = new int[1];
        var since = new long[]{System.nanoTime()};
        helper.onEachTick(() -> {
            long now = System.nanoTime();
            if (now - since[0] >= GIVE_UP_NANOS) {
                helper.fail("step " + step[0] + " never got done");
            } else if (steps.get(step[0]).getAsBoolean()) {
                since[0] = now;
                if (++step[0] == steps.size()) {
                    helper.succeed();
                    return;
                }
            }
            long wait;
            while ((wait = now + TICK_NANOS - System.nanoTime()) > 0) {
                LockSupport.parkNanos(wait);
            }
        });
    }

    /**
     * How often the logic ran in each period, leaving out the first and last periods seen, which an add, a removal or the
     * snapshot itself may have cut short.
     */
    private static Map<Long, Long> runsPerWholePeriod(List<Long> periods) {
        var runs = periods.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (!runs.isEmpty()) {
            runs.remove(Collections.min(runs.keySet()));
            runs.remove(Collections.max(runs.keySet()));
        }
        return runs;
    }

    static void logicRunsOncePerAddUntilRemovedAsOften(GameTestHelper helper) {
        var data = AsyncThreadData.getOrCreate(helper.getLevel());
        var logic = new CountingLogic();
        data.addAsyncLogic(logic);
        data.addAsyncLogic(logic);
        var mark = new int[1];
        var waitStart = new long[1];
        inRealTime(helper, List.of(
                () -> logic.snapshot().size() >= 10,
                () -> {
                    var runs = runsPerWholePeriod(logic.snapshot());
                    if (runs.isEmpty() || runs.values().stream().anyMatch(count -> count != 2)) {
                        helper.fail("a logic added twice did not run twice per period: " + runs);
                    }
                    data.removeAsyncLogic(logic);
                    mark[0] = logic.snapshot().size();
                    return true;
                },
                () -> logic.snapshot().size() >= mark[0] + 6,
                () -> {
                    var all = logic.snapshot();
                    var runs = runsPerWholePeriod(all.subList(mark[0], all.size()));
                    if (runs.isEmpty() || runs.values().stream().anyMatch(count -> count != 1)) {
                        helper.fail("removed once, the logic should run once per period: " + runs);
                    }
                    data.removeAsyncLogic(logic);
                    mark[0] = logic.snapshot().size();
                    waitStart[0] = System.nanoTime();
                    return true;
                },
                () -> System.nanoTime() - waitStart[0] >= PERIODS_NANOS,
                () -> {
                    // a period already running when it was removed may still run it once
                    if (logic.snapshot().size() > mark[0] + 1) {
                        helper.fail("the logic kept running after its last removal: " + logic.snapshot().size() + " runs, " + mark[0] + " before");
                    }
                    return true;
                }));
    }

    static void failingPeriodTaskLeavesTheOthersAndTheNextPeriods(GameTestHelper helper) {
        var data = AsyncThreadData.getOrCreate(helper.getLevel());
        var ranAfterFailure = new AtomicInteger();
        IAsyncLogic logic = periodID -> {
            AsyncThreadData.periodTask("failing", () -> server -> {
                throw new IllegalStateException("expected by the test");
            });
            AsyncThreadData.periodTask("counting", () -> server -> ranAfterFailure.incrementAndGet());
        };
        data.addAsyncLogic(logic);
        inRealTime(helper, List.of(
                () -> ranAfterFailure.get() >= 5,
                () -> {
                    data.removeAsyncLogic(logic);
                    return true;
                }));
    }
}
