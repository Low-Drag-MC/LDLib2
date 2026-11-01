package com.lowdragmc.lowdraglib2.test.gametest.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench.SyncBenchmarkGameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class SyncDataGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, LDLib2.MOD_ID);
    private static boolean initialized = false;

    private SyncDataGameTests() {
    }

    public static void init(IEventBus eventBus) {
        if (initialized) {
            return;
        }
        initialized = true;
        MapSerializationGameTest.registerFunctions();
        ManagedCodecGameTest.registerFunctions();
        SyncTestBlockEntityGameTest.registerFunctions();
        PersistedCustomDataGameTest.registerFunctions();
        MarkStrategyGameTest.registerFunctions();
        SyncChangeDetectionGameTest.registerFunctions();
        SyncPacketBatchGameTest.registerFunctions();
        AsyncThreadDataGameTest.registerFunctions();
        AsyncPersistenceGameTest.registerFunctions();
        SyncBenchmarkGameTest.registerFunctions();
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(SyncDataGameTests::registerGameTests);
    }

    public static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> registerFunction(String path, Consumer<GameTestHelper> function) {
        return TEST_FUNCTIONS.register(path, () -> function);
    }

    public static ResourceKey<Consumer<GameTestHelper>> functionKey(String path) {
        return ResourceKey.create(Registries.TEST_FUNCTION, LDLib2.id(path));
    }

    public static TestData<Holder<TestEnvironmentDefinition<?>>> defaultTestData(Holder<TestEnvironmentDefinition<?>> environment, String structurePath) {
        return testData(environment, structurePath, 20);
    }

    public static TestData<Holder<TestEnvironmentDefinition<?>>> testData(Holder<TestEnvironmentDefinition<?>> environment, String structurePath, int maxTicks) {
        return new TestData<>(environment, LDLib2.id(structurePath), maxTicks, 0, true, Rotation.NONE);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                LDLib2.id("syncdata"),
                new TestEnvironmentDefinition.AllOf()
        );
        MapSerializationGameTest.register(event, environment);
        ManagedCodecGameTest.register(event, environment);
        SyncTestBlockEntityGameTest.register(event, environment);
        PersistedCustomDataGameTest.register(event, environment);
        MarkStrategyGameTest.register(event, environment);
        SyncChangeDetectionGameTest.register(event, environment);
        SyncPacketBatchGameTest.register(event, environment);
        AsyncThreadDataGameTest.register(event, environment);
        AsyncPersistenceGameTest.register(event, environment);
        SyncBenchmarkGameTest.register(event, environment);
    }

    public static void registerFunctionTest(
            RegisterGameTestsEvent event,
            String path,
            ResourceKey<Consumer<GameTestHelper>> functionKey,
            TestData<Holder<TestEnvironmentDefinition<?>>> testData
    ) {
        event.registerTest(
                LDLib2.id(path),
                new FunctionGameTestInstance(functionKey, testData)
        );
    }
}