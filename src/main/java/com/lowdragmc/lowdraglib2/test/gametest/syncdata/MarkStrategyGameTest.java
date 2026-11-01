package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.ValueIOSerializableReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ManagedCodec;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.mark.IMarkStrategy;
import com.lowdragmc.lowdraglib2.syncdata.mark.MarkStrategies;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class MarkStrategyGameTest {
    private static final String BUILT_IN_STRATEGIES_TAKE_ONLY_CONTAINERS_SAVED_THE_BASE_WAY_PATH = "mark_strategy_built_in_strategies_take_only_containers_saved_the_base_way";
    private static final String CONTENT_VERSION_DECIDES_WHETHER_A_VALUE_CHANGED_PATH = "mark_strategy_content_version_decides_whether_a_value_changed";
    private static final String REGISTERED_STRATEGY_DECIDES_FOR_ITS_CLASS_PATH = "mark_strategy_registered_strategy_decides_for_its_class";
    private static final String REGISTERING_REPLACES_EARLIER_LOOKUPS_AND_LOWER_PRIORITIES_WIN_PATH = "mark_strategy_registering_replaces_earlier_lookups_and_lower_priorities_win";
    private static final String A_VALUE_TURNING_INTO_ANOTHER_CLASS_COUNTS_AS_CHANGED_PATH = "mark_strategy_a_value_turning_into_another_class_counts_as_changed";

    static void registerFunctions() {
        SyncDataGameTests.registerFunction(BUILT_IN_STRATEGIES_TAKE_ONLY_CONTAINERS_SAVED_THE_BASE_WAY_PATH, MarkStrategyGameTest::builtInStrategiesTakeOnlyContainersSavedTheBaseWay);
        SyncDataGameTests.registerFunction(CONTENT_VERSION_DECIDES_WHETHER_A_VALUE_CHANGED_PATH, MarkStrategyGameTest::contentVersionDecidesWhetherAValueChanged);
        SyncDataGameTests.registerFunction(REGISTERED_STRATEGY_DECIDES_FOR_ITS_CLASS_PATH, MarkStrategyGameTest::registeredStrategyDecidesForItsClass);
        SyncDataGameTests.registerFunction(REGISTERING_REPLACES_EARLIER_LOOKUPS_AND_LOWER_PRIORITIES_WIN_PATH, MarkStrategyGameTest::registeringReplacesEarlierLookupsAndLowerPrioritiesWin);
        SyncDataGameTests.registerFunction(A_VALUE_TURNING_INTO_ANOTHER_CLASS_COUNTS_AS_CHANGED_PATH, MarkStrategyGameTest::aValueTurningIntoAnotherClassCountsAsChanged);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = SyncDataGameTests.defaultTestData(environment, "empty");
        SyncDataGameTests.registerFunctionTest(event, BUILT_IN_STRATEGIES_TAKE_ONLY_CONTAINERS_SAVED_THE_BASE_WAY_PATH, SyncDataGameTests.functionKey(BUILT_IN_STRATEGIES_TAKE_ONLY_CONTAINERS_SAVED_THE_BASE_WAY_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, CONTENT_VERSION_DECIDES_WHETHER_A_VALUE_CHANGED_PATH, SyncDataGameTests.functionKey(CONTENT_VERSION_DECIDES_WHETHER_A_VALUE_CHANGED_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, REGISTERED_STRATEGY_DECIDES_FOR_ITS_CLASS_PATH, SyncDataGameTests.functionKey(REGISTERED_STRATEGY_DECIDES_FOR_ITS_CLASS_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, REGISTERING_REPLACES_EARLIER_LOOKUPS_AND_LOWER_PRIORITIES_WIN_PATH, SyncDataGameTests.functionKey(REGISTERING_REPLACES_EARLIER_LOOKUPS_AND_LOWER_PRIORITIES_WIN_PATH), testData);
        SyncDataGameTests.registerFunctionTest(event, A_VALUE_TURNING_INTO_ANOTHER_CLASS_COUNTS_AS_CHANGED_PATH, SyncDataGameTests.functionKey(A_VALUE_TURNING_INTO_ANOTHER_CLASS_COUNTS_AS_CHANGED_PATH), testData);
    }


    public static class VersionedHolder extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final SyncProbeHolder.VersionedItems items = new SyncProbeHolder.VersionedItems(2);
        @Persisted
        @DescSynced
        public final SyncProbeHolder.VersionedTank tank = new SyncProbeHolder.VersionedTank(1000);
        @Persisted
        @DescSynced
        @ManagedCodec("thingCodec")
        public VersionedThing thing = new VersionedThing(1);

        public VersionedHolder(ServerLevel level) {
            super(level);
            items.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        }

        private static Codec<VersionedThing> thingCodec() {
            return Codec.INT.xmap(VersionedThing::new, thing -> thing.value);
        }
    }

    public static class VersionedThing implements IContentVersion {
        public int value;
        public long version;

        public VersionedThing(int value) {
            this.value = value;
        }

        @Override
        public long getContentVersion() {
            return version;
        }
    }

    /**
     * Saves and syncs only its counter.
     */
    public static class Counted implements ValueIOSerializable {
        public int counter;
        public String note = "";

        @Override
        public void serialize(ValueOutput output) {
            output.putInt("counter", counter);
        }

        @Override
        public void deserialize(ValueInput input) {
            counter = input.getIntOr("counter", 0);
        }
    }

    public static class Late extends Counted {
    }

    public static class Muted extends Counted {
    }

    public static class Contested extends Counted {
    }

    public static class CountedHolder extends SyncProbeHolder {
        @Persisted
        @DescSynced
        public final Counted counted = new Counted();
        @Persisted
        @DescSynced
        public final Muted muted = new Muted();

        public CountedHolder(ServerLevel level) {
            super(level);
        }
    }

    private record CounterStrategy(Class<?> type, boolean everChanges) implements IMarkStrategy<Counted, Integer> {
        @Override
        public boolean test(Class<?> type) {
            return type == this.type;
        }

        @Override
        public @NotNull Integer obtainManagedMark(@NotNull Counted value) {
            return value.counter;
        }

        @Override
        public boolean areDifferent(@NotNull Integer managedMark, @NotNull Counted value) {
            return everChanges && managedMark != value.counter;
        }
    }

    // the registry lives as long as the game, so the test strategies go in once
    private static boolean registered;
    private static final CounterStrategy LATE = new CounterStrategy(Late.class, true);
    private static final CounterStrategy PREFERRED = new CounterStrategy(Contested.class, true);
    private static boolean lateRegistered;

    private static synchronized void registerCounterStrategy() {
        if (registered) return;
        registered = true;
        MarkStrategies.register(new CounterStrategy(Counted.class, true), 1000);
        // tells the sync a Muted never changes, which only holds if the sync asks the registry rather than the save
        MarkStrategies.register(new CounterStrategy(Muted.class, false), 1000);
    }

    static void builtInStrategiesTakeOnlyContainersSavedTheBaseWay(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        var items = MarkStrategies.find(ItemStackTransfer.class);
        var tank = MarkStrategies.find(FluidStorage.class);
        var version = MarkStrategies.find(SyncProbeHolder.VersionedItems.class);
        if (items == null || tank == null || items == tank) failures.add("plain containers got " + items + " and " + tank);
        if (MarkStrategies.find(SyncProbeHolder.LockableItems.class) != null) failures.add("a handler saving extra state got a slot strategy");
        if (MarkStrategies.find(SyncProbeHolder.LockableTank.class) != null) failures.add("a tank saving extra state got a fluid strategy");
        if (MarkStrategies.find(SyncChangeDetectionGameTest.PassThroughItems.class) != null) failures.add("a handler reading slots its own way got a slot strategy");
        if (version == null || version == items || version != MarkStrategies.find(SyncProbeHolder.VersionedTank.class)) {
            failures.add("versioned containers got " + version);
        }
        if (MarkStrategies.find(String.class) != null) failures.add("a String got a strategy");
        report(helper, failures);
    }

    static void contentVersionDecidesWhetherAValueChanged(GameTestHelper helper) {
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        expect(failures, level, "slot set", "items", true, h -> h.items.setStackInSlot(1, new ItemStack(Items.DIAMOND)));
        expect(failures, level, "extra state changed through its versioned setter", "items", true, h -> h.items.setLocked(true));
        expect(failures, level, "stack grown in place, which the version doesn't count", "items", false, h -> h.items.getStackInSlot(0).grow(1));
        expect(failures, level, "tank filled", "tank", true, h -> h.tank.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE));
        expect(failures, level, "codec field changed with a new version", "thing", true, h -> {
            h.thing.value = 7;
            h.thing.version++;
        });
        expect(failures, level, "codec field changed without a new version", "thing", false, h -> h.thing.value = 7);
        report(helper, failures);
    }

    private static void expect(List<String> failures, ServerLevel level, String name, String field, boolean synced, Consumer<VersionedHolder> change) {
        var server = new VersionedHolder(level);
        server.settle();
        var client = new VersionedHolder(level);
        server.initClient(client);
        change.accept(server);
        boolean sent = server.syncTo(client);
        if (sent != synced) {
            failures.add(name + ": " + (sent ? "synced " + server.syncFieldNames(server.sentChanged) : "not synced"));
        } else if (sent && !client.syncedValue(field).equals(server.syncedValue(field))) {
            failures.add(name + ": client has " + client.syncedValue(field) + ", server " + server.syncedValue(field));
        }
    }

    static void registeredStrategyDecidesForItsClass(GameTestHelper helper) {
        registerCounterStrategy();
        var level = helper.getLevel();
        var failures = new ArrayList<String>();
        var server = new CountedHolder(level);
        server.settle();
        var client = new CountedHolder(level);
        server.initClient(client);

        server.counted.note = "only in memory";
        if (server.syncTo(client)) failures.add("a change the strategy leaves out was synced");
        server.counted.counter = 5;
        if (!server.syncTo(client) || client.counted.counter != 5) failures.add("the counter was not synced, client has " + client.counted.counter);
        server.muted.counter = 5;
        if (server.syncTo(client)) failures.add("the registered strategy was passed over for what the value saves");
        report(helper, failures);
    }

    static void registeringReplacesEarlierLookupsAndLowerPrioritiesWin(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        synchronized (MarkStrategyGameTest.class) {
            if (!lateRegistered) {
                lateRegistered = true;
                if (MarkStrategies.find(Late.class) != null) failures.add("a class nobody registered for got a strategy");
                MarkStrategies.register(LATE, 1000);
                MarkStrategies.register(new CounterStrategy(Contested.class, false), 500);
                MarkStrategies.register(PREFERRED, 100);
            }
        }
        if ((Object) MarkStrategies.find(Late.class) != LATE) failures.add("a lookup made before the registration stayed: " + MarkStrategies.find(Late.class));
        if ((Object) MarkStrategies.find(Contested.class) != PREFERRED) failures.add("the higher priority number won: " + MarkStrategies.find(Contested.class));
        report(helper, failures);
    }

    static void aValueTurningIntoAnotherClassCountsAsChanged(GameTestHelper helper) {
        var failures = new ArrayList<String>();
        var accessor = new ValueIOSerializableReadOnlyAccessor();
        var mark = accessor.obtainManagedMark(new ItemStackTransfer(1));
        var other = new SyncProbeHolder.LockableItems(1);
        if (!accessor.areDifferent(mark, other)) failures.add("a value compared another way looked unchanged");
        var next = accessor.updateMark(mark, other);
        if (next == null) failures.add("a value compared another way got no new mark");
        else if (accessor.updateMark(next, other) != null) failures.add("an unchanged value got a new mark after switching");
        report(helper, failures);
    }

    private static void report(GameTestHelper helper, List<String> failures) {
        if (failures.isEmpty()) {
            helper.succeed();
        } else {
            helper.fail(failures.size() + " failed:\n" + String.join("\n", failures));
        }
    }
}
