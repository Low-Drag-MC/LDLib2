package com.lowdragmc.lowdraglib2.test.gametest.mp;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.mp.MPSegment;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;

/**
 * The dist gate for the multi-process harness. The whole harness rests on one property: every
 * {@link MPScenario} class must instantiate and {@code define()} in the <b>dedicated-server</b>
 * process, even though its client blocks are full of client-only code. That holds only while the
 * client-only types stay out of the scenario's method <em>signatures</em> (lambda bodies are lazy;
 * signatures resolve when the class is linked) — a rule a single innocent-looking lambda can break.
 *
 * <p>This test runs on the game-test server, which is the dedicated dist, so a violation fails here
 * with the offending scenario's name instead of surfacing as a crashed {@code runMpTest} run.
 * Same idea as {@code UIElementRegistryTest} constructing every UI element server-side.
 */
public final class MPScenarioDistLoadingTest {
    private static final String SCENARIOS_DEFINE_ON_DEDICATED_SERVER = "mp_scenarios_define_on_dedicated_server";

    private MPScenarioDistLoadingTest() {
    }

    static void registerFunctions() {
        MPGameTests.registerFunction(SCENARIOS_DEFINE_ON_DEDICATED_SERVER, MPScenarioDistLoadingTest::mpScenariosDefineOnDedicatedServer);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = MPGameTests.defaultTestData(environment, "empty");
        MPGameTests.registerFunctionTest(event, SCENARIOS_DEFINE_ON_DEDICATED_SERVER, MPGameTests.functionKey(SCENARIOS_DEFINE_ON_DEDICATED_SERVER), testData);
    }

    // ------------------------------------------------------------------

    public static void mpScenariosDefineOnDedicatedServer(GameTestHelper helper) {
        var registry = LDLib2Registries.MP_SCENARIOS;
        if (registry == null) {
            helper.fail("The ldlib2:mp_scenario registry is unavailable (not a dev environment?)");
            return;
        }

        int scenarioCount = 0;
        for (var holder : registry) {
            var name = holder.annotation().name();
            try {
                // Instantiation is the moment the class links — this is where a client-only type in
                // a synthetic lambda signature explodes, before define() even runs.
                MPScenario scenario = holder.value().get();
                var options = new MPScenarioOptions();
                scenario.configure(options);
                var builder = new MPScenarioBuilder();
                scenario.define(builder);

                if (builder.segments().isEmpty()) {
                    helper.fail("MP scenario '" + name + "' defines no segments");
                }
                if (options.clients().isEmpty()) {
                    helper.fail("MP scenario '" + name + "' declares no client roles");
                }
                checkRolesDeclared(helper, name, options, builder.segments());
                checkRolesDeclared(helper, name, options, builder.teardownSegments());
            } catch (GameTestAssertException e) {
                throw e; // one of the structural checks above already failed with a precise message
            } catch (Throwable t) {
                LDLib2.LOGGER.error("[mptest] scenario '{}' failed to define on the dedicated dist", name, t);
                helper.fail("MP scenario '" + name + "' cannot load/define on a dedicated server - "
                        + "a client-only type leaked into a lambda signature? " + t);
            }
            scenarioCount++;
        }

        if (scenarioCount == 0) {
            helper.fail("No MP scenarios registered - the scan or the registry wiring is broken");
        }
        LDLib2.LOGGER.info("[mptest] {} MP scenario(s) define cleanly on the dedicated dist", scenarioCount);
        helper.succeed();
    }

    private static void checkRolesDeclared(GameTestHelper helper, String name,
                                           MPScenarioOptions options, List<MPSegment> segments) {
        for (var segment : segments) {
            if (segment.role != null && !options.clients().contains(segment.role)) {
                helper.fail("MP scenario '" + name + "' segment " + segment
                        + " uses role '" + segment.role + "' which is not in clients(" + options.clients() + ")");
            }
        }
    }
}
