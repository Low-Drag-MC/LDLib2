package com.lowdragmc.lowdraglib2.test.registry;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Supplier;

/**
 * Tests that {@link com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment} filtering
 * works correctly during auto-registration.
 * These tests run in a dev environment.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class RegistrationEnvironmentTest {

    public static final AutoRegistry.LDLibRegister<ITestRegistryEntry, Supplier<ITestRegistryEntry>> TEST_ENV_REGISTRY =
            AutoRegistry.LDLibRegister.create(LDLib2.id("test_env_registry"), ITestRegistryEntry.class, AutoRegistry::noArgsCreator);

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void alwaysEntryIsRegistered(GameTestHelper helper) {
        var holder = TEST_ENV_REGISTRY.get("test_always");
        if (holder == null) {
            helper.fail("ALWAYS entry should be registered but was not found");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void devOnlyEntryIsRegisteredInDev(GameTestHelper helper) {
        var holder = TEST_ENV_REGISTRY.get("test_dev_only");
        if (holder == null) {
            helper.fail("DEV_ONLY entry should be registered in dev environment but was not found");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void productionOnlyEntryIsNotRegisteredInDev(GameTestHelper helper) {
        var holder = TEST_ENV_REGISTRY.get("test_production_only");
        if (holder != null) {
            helper.fail("PRODUCTION_ONLY entry should NOT be registered in dev environment");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void manualEntryIsNotAutoRegistered(GameTestHelper helper) {
        var holder = TEST_ENV_REGISTRY.get("test_manual");
        if (holder != null) {
            helper.fail("MANUAL entry should NOT be auto-registered");
            return;
        }
        helper.succeed();
    }
}
