package com.lowdragmc.lowdraglib2.test.gametest.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

public final class UIElementRegistryGameTest {
    private static final String TEST_PATH = "ui_registry_test";

    private UIElementRegistryGameTest() {
    }

    static void registerFunctions() {
        UIGameTests.registerFunction(TEST_PATH, UIElementRegistryGameTest::run);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = UIGameTests.defaultTestData(environment, "empty");
        UIGameTests.registerFunctionTest(event, TEST_PATH, UIGameTests.functionKey(TEST_PATH), testData);
    }

    private static void run(GameTestHelper helper) {
        LDLib2.LOGGER.info("Start UI Registry Test");
        for (var holder : LDLib2Registries.UI_ELEMENTS.values()) {
            var element = holder.value().get();
            element.deserialize(TagValueInput.create(
                    ProblemReporter.Collector.DISCARDING,
                    helper.getLevel().registryAccess(),
                    new CompoundTag()
            ));
        }
        LDLib2.LOGGER.info("End UI Registry Test");
        helper.succeed();
    }
}
