package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

import java.util.function.Supplier;

/**
 * An automated UI test: a sequence of steps that drives a real client — creating a world, opening a
 * UI, clicking, typing, waiting for server round trips — and asserts on what happens.
 *
 * <p>Register one with {@code @LDLRegisterClient(registry = UIScenario.REGISTRY, ...)}. Discovery is
 * an annotation scan over every loaded mod, so a mod that depends on LDLib2 registers scenarios with
 * no changes on the LDLib2 side. Use
 * {@link com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment#DEV_ONLY} to keep them out of
 * production builds.
 *
 * <p>Run them with {@code gradlew runClient -PldTest=<name|all|group:x|tag:y|mod:z>}. The client
 * launches, runs the selection, writes {@code report.json} plus screenshots, and exits with a
 * non-zero code if anything failed.
 *
 * <pre>{@code
 * @LDLRegisterClient(name = "furnace_ui", group = "mymod", registry = UIScenario.REGISTRY,
 *                    environment = RegistrationEnvironment.DEV_ONLY)
 * public class FurnaceUiScenario implements UIScenario {
 *     private static final BlockPos POS = new BlockPos(8, 65, 8);
 *
 *     @Override
 *     public void define(ScenarioBuilder s) {
 *         s.setBlock(POS, MyBlocks.FURNACE.defaultBlockState())
 *          .awaitClientBlockEntity(POS)
 *          .useBlock(POS)                                  // real right-click, real open packet
 *          .awaitScreen(ModularUIContainerScreen.class)
 *          .click("#btn_start")
 *          .waitForSync("burn time reaches the client",
 *                  sc  -> sc.blockEntity(POS, FurnaceBlockEntity.class).getBurnTime(),
 *                  ctx -> ctx.clientBlockEntity(POS, FurnaceBlockEntity.class).getBurnTime())
 *          .checkTextContains("#burn_label", "burning")
 *          .screenshot("running");
 *     }
 * }
 * }</pre>
 */
public interface UIScenario extends ILDLRegisterClient<UIScenario, Supplier<UIScenario>> {

    /** Pass this to {@code @LDLRegisterClient(registry = ...)}. */
    String REGISTRY = "ldlib2:ui_scenario";

    /**
     * Records the steps. Called once per run on the client thread, before the first step executes.
     *
     * <p>This builds a plan; it does not touch the UI. Anything that needs to read live state must
     * happen inside a step body, because at definition time nothing has been opened yet.
     */
    void define(ScenarioBuilder s);

    /** Optional per-scenario configuration. */
    default void configure(ScenarioOptions options) {
    }
}
