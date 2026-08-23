package com.lowdragmc.lowdraglib2.uitest.mp;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;

import java.util.function.Supplier;

/**
 * A multi-process automated test: a dedicated server plus one or more real clients connected over
 * localhost, driven in lockstep. Where a {@link com.lowdragmc.lowdraglib2.uitest.UIScenario} tests a
 * client against its integrated server, this tests the full wire: C2S packets, S2C sync, and what a
 * <em>second</em> client observes.
 *
 * <p>Register one with {@code @LDLRegister(registry = MPScenario.REGISTRY, environment =
 * RegistrationEnvironment.DEV_ONLY)} — the plain {@code @LDLRegister}, not the client variant,
 * because every process (the dedicated server included) discovers scenarios through this registry.
 * Run a selection with {@code gradlew runMpTest -PldMpTest=<name|all|group:x|tag:y>}.
 *
 * <p><b>Execution model.</b> Every process loads the same scenario class and calls {@link #define};
 * that produces an identical ordered list of {@linkplain MPSegment segments}. Each process then
 * executes only the segments it owns — {@code server(..)} segments on the dedicated server thread,
 * {@code client(role, ..)} segments on that client — and waits at a barrier for everyone else's.
 * Inside a client segment the full {@link com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder} DSL is
 * available unchanged.
 *
 * <p><b>Dist rule for authors.</b> Because {@code define} also runs in the dedicated-server process,
 * the scenario class must remain loadable there. Practically: lambdas whose <em>signature</em> would
 * mention a client-only type are the one thing to avoid — {@code Consumer<TestContext>},
 * {@code Predicate<TestContext>} and value getters returning boxed primitives are all fine, but a
 * factory like {@code Function<TestContext, Screen>} is not (its synthetic method's return type is
 * {@code Screen}), and neither is a lambda capturing a client-typed local. Class literals such as
 * {@code awaitScreen(ModularUIContainerScreen.class)} are fine: they only resolve when the owning
 * client expands the block. The {@code MPScenarioDistLoadingTest} game test enforces this by
 * instantiating and defining every registered scenario on a dedicated server.
 */
public interface MPScenario extends ILDLRegister<MPScenario, Supplier<MPScenario>> {

    /** Pass this to {@code @LDLRegister(registry = ...)}. */
    String REGISTRY = "ldlib2:mp_scenario";

    /**
     * Records the segments. Called once per run in <b>every process</b>, so it must be deterministic
     * and must not touch live game state — segment bodies are where that happens.
     */
    void define(MPScenarioBuilder s);

    /** Optional per-scenario configuration. Also called in every process. */
    default void configure(MPScenarioOptions options) {
    }
}
