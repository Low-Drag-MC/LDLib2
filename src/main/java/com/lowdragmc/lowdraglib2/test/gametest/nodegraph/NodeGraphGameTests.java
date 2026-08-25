package com.lowdragmc.lowdraglib2.test.gametest.nodegraph;

import com.lowdragmc.lowdraglib2.LDLib2;
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

public final class NodeGraphGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, LDLib2.MOD_ID);
    private static boolean initialized = false;

    private NodeGraphGameTests() {
    }

    public static void init(IEventBus eventBus) {
        if (initialized) {
            return;
        }
        initialized = true;
        GraphSerializationGameTest.registerFunctions();
        GraphHierarchySerializationGameTest.registerFunctions();
        GraphAnnotationRegistrationGameTest.registerFunctions();
        GraphCopyPasteGameTest.registerFunctions();
        GraphFuzzGameTest.registerFunctions();
        GraphRenameColorTest.registerFunctions();
        GraphSubgraphTest.registerFunctions();
        ContextBlockTest.registerFunctions();
        GraphCommandPolicyTest.registerFunctions();
        GraphCrossTypeSubgraphTest.registerFunctions();
        GraphNodePreviewTest.registerFunctions();
        GraphWireReroutePointTest.registerFunctions();
        TEST_FUNCTIONS.register(eventBus);
        eventBus.addListener(NodeGraphGameTests::registerGameTests);
    }

    static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> registerFunction(String path, Consumer<GameTestHelper> function) {
        return TEST_FUNCTIONS.register(path, () -> function);
    }

    static ResourceKey<Consumer<GameTestHelper>> functionKey(String path) {
        return ResourceKey.create(Registries.TEST_FUNCTION, LDLib2.id(path));
    }

    static TestData<Holder<TestEnvironmentDefinition<?>>> defaultTestData(Holder<TestEnvironmentDefinition<?>> environment, String structurePath) {
        return new TestData<>(
                environment,
                LDLib2.id(structurePath),
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0
        );
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                LDLib2.id("nodegraph"),
                new TestEnvironmentDefinition.AllOf()
        );
        GraphSerializationGameTest.register(event, environment);
        GraphHierarchySerializationGameTest.register(event, environment);
        GraphAnnotationRegistrationGameTest.register(event, environment);
        GraphCopyPasteGameTest.register(event, environment);
        GraphFuzzGameTest.register(event, environment);
        GraphRenameColorTest.register(event, environment);
        GraphSubgraphTest.register(event, environment);
        ContextBlockTest.register(event, environment);
        GraphCommandPolicyTest.register(event, environment);
        GraphCrossTypeSubgraphTest.register(event, environment);
        GraphNodePreviewTest.register(event, environment);
        GraphWireReroutePointTest.register(event, environment);
    }

    static void registerFunctionTest(
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
