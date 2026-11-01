package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;

import java.util.function.Supplier;

/**
 * A scripted video of a UI, for documentation: the same {@link ScenarioBuilder} steps as a
 * {@link UIScenario}, run in a real client, with the parts wrapped in
 * {@link ScenarioBuilder#take(String, java.util.function.Consumer) take(..)} encoded to video.
 *
 * <p>Recordings live in a registry of their own, so {@code -PldTest} never selects them and a test run
 * never pays for them. Run them with {@code gradlew runClient -PldRecord=<name|all|group:x|tag:y|mod:z>};
 * only those runs record, and the videos land in {@code build/ldlib2-recordings/<name>/}, which test runs
 * leave alone. Checks still count: a recording whose checks fail fails the build, so a video of a UI
 * that did not do what the script says is never mistaken for a good one.
 *
 * <p>Register one with {@code @LDLRegisterClient(registry = UIRecording.REGISTRY, ...)}, usually
 * {@link com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment#DEV_ONLY}:
 *
 * <pre>{@code
 * @LDLRegisterClient(name = "furnace_tour", group = "mymod", registry = UIRecording.REGISTRY,
 *                    environment = RegistrationEnvironment.DEV_ONLY)
 * public class FurnaceTour implements UIRecording {
 *     @Override
 *     public void configure(ScenarioOptions options) {
 *         options.guiScale(4);                                    // made for a 3840x2160 window
 *     }
 *
 *     @Override
 *     public void define(ScenarioBuilder s) {
 *         s.openModularUI("furnace", FurnaceTour::ui).awaitModularUI()
 *          .take("start", o -> o.region("#furnace").size(1280, 720), t -> t
 *              .caption("Press start to light the furnace")
 *              .caption("zh", "点击开始点燃熔炉")
 *              .click("#btn_start")
 *              .hold(1000))
 *          .checkTextContains("#status", "Burning");             // the video only counts if this holds
 *     }
 * }
 * }</pre>
 *
 * <p>A recording's steps can be shared with a test: both {@link #define} methods may call the same
 * static method. In the test the takes, captions and holds simply do nothing.
 */
public interface UIRecording extends ILDLRegisterClient<UIRecording, Supplier<UIRecording>> {

    /** Pass this to {@code @LDLRegisterClient(registry = ...)}. */
    String REGISTRY = "ldlib2:ui_recording";

    /** Records the steps. See {@link UIScenario#define}. */
    void define(ScenarioBuilder s);

    /** Optional configuration: GUI scale, tags, world, settle. See {@link UIScenario#configure}. */
    default void configure(ScenarioOptions options) {
    }
}
