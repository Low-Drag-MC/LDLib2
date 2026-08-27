package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.ComposeBenchmark;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;

/**
 * Runs {@link ComposeBenchmark} inside a real client and logs the result.
 *
 * <p>Not an assertion of any particular speed — those would be assertions about the machine. It
 * exists so the numbers can be reproduced on demand, and so a change that makes reconciliation
 * quadratic shows up as a number somebody can see rather than as a UI that feels sluggish.
 */
@LDLRegisterClient(name = "compose_perf", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class ComposePerfScenario implements UIScenario {

    @Override
    public void configure(ScenarioOptions options) {
        options.tags("compose", "perf").requiresWorld(true);
    }

    @Override
    public void define(ScenarioBuilder s) {
        s.step("benchmark the declarative layer against a real element tree", ctx -> {
            for (var line : ComposeBenchmark.run(500)) {
                LDLib2.LOGGER.info("[compose-perf] {}", line);
            }
        });
    }
}
