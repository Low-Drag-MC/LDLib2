package com.lowdragmc.lowdraglib2.test.gametest.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Slider} is a registered UI element, so it has to be reachable by the names XML and the editor
 * use, and its value model has to behave like {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller}'s.
 */
public final class SliderGameTest {
    private static final String REGISTERED_UNDER_BOTH_NAMES = "slider_registered_under_both_names";
    private static final String VALUE_IS_CLAMPED_TO_RANGE = "slider_value_is_clamped_to_range";
    private static final String NORMALIZED_VALUE_ROUND_TRIPS = "slider_normalized_value_round_trips";
    private static final String LISTENERS_ONLY_FIRE_ON_REAL_CHANGES = "slider_listeners_only_fire_on_real_changes";
    private static final String STEP_MOVES_BY_A_FRACTION_OF_THE_RANGE = "slider_step_moves_by_a_fraction_of_the_range";

    private SliderGameTest() {
    }

    static void registerFunctions() {
        UIGameTests.registerFunction(REGISTERED_UNDER_BOTH_NAMES, SliderGameTest::registeredUnderBothNames);
        UIGameTests.registerFunction(VALUE_IS_CLAMPED_TO_RANGE, SliderGameTest::valueIsClampedToRange);
        UIGameTests.registerFunction(NORMALIZED_VALUE_ROUND_TRIPS, SliderGameTest::normalizedValueRoundTrips);
        UIGameTests.registerFunction(LISTENERS_ONLY_FIRE_ON_REAL_CHANGES, SliderGameTest::listenersOnlyFireOnRealChanges);
        UIGameTests.registerFunction(STEP_MOVES_BY_A_FRACTION_OF_THE_RANGE, SliderGameTest::stepMovesByAFractionOfTheRange);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = UIGameTests.defaultTestData(environment, "empty");
        UIGameTests.registerFunctionTest(event, REGISTERED_UNDER_BOTH_NAMES, UIGameTests.functionKey(REGISTERED_UNDER_BOTH_NAMES), testData);
        UIGameTests.registerFunctionTest(event, VALUE_IS_CLAMPED_TO_RANGE, UIGameTests.functionKey(VALUE_IS_CLAMPED_TO_RANGE), testData);
        UIGameTests.registerFunctionTest(event, NORMALIZED_VALUE_ROUND_TRIPS, UIGameTests.functionKey(NORMALIZED_VALUE_ROUND_TRIPS), testData);
        UIGameTests.registerFunctionTest(event, LISTENERS_ONLY_FIRE_ON_REAL_CHANGES, UIGameTests.functionKey(LISTENERS_ONLY_FIRE_ON_REAL_CHANGES), testData);
        UIGameTests.registerFunctionTest(event, STEP_MOVES_BY_A_FRACTION_OF_THE_RANGE, UIGameTests.functionKey(STEP_MOVES_BY_A_FRACTION_OF_THE_RANGE), testData);
    }

    public static void registeredUnderBothNames(GameTestHelper helper) {
        for (var name : new String[]{"slider-horizontal", "slider-vertical"}) {
            var holder = LDLib2Registries.UI_ELEMENTS.get(name);
            if (holder == null) {
                helper.fail("No UI element registered as " + name);
                return;
            }
            var element = holder.value().get();
            if (!(element instanceof Slider)) {
                helper.fail(name + " resolved to " + element.getClass().getSimpleName() + ", not a Slider");
                return;
            }
        }
        helper.succeed();
    }

    public static void valueIsClampedToRange(GameTestHelper helper) {
        var slider = new Slider.Horizontal().setRange(10, 100);
        slider.setValue(500f);
        if (slider.getValue() != 100f) {
            helper.fail("Expected the value to clamp to 100, got " + slider.getValue());
            return;
        }
        slider.setValue(-5f);
        if (slider.getValue() != 10f) {
            helper.fail("Expected the value to clamp to 10, got " + slider.getValue());
            return;
        }
        // shrinking the range has to pull the value back in with it
        slider.setValue(80f);
        slider.setRange(10, 50);
        if (slider.getValue() != 50f) {
            helper.fail("Expected the value to follow the range down to 50, got " + slider.getValue());
            return;
        }
        helper.succeed();
    }

    public static void normalizedValueRoundTrips(GameTestHelper helper) {
        var slider = new Slider.Vertical().setRange(10, 100);
        slider.setNormalizedValue(0.5f);
        if (slider.getValue() != 55f) {
            helper.fail("Expected 55 at the midpoint of 10..100, got " + slider.getValue());
            return;
        }
        if (Math.abs(slider.getNormalizedValue() - 0.5f) > 1e-6) {
            helper.fail("Normalized value did not round trip: " + slider.getNormalizedValue());
            return;
        }
        // an empty range must not divide by zero
        slider.setRange(1, 1);
        if (!Float.isFinite(slider.getNormalizedValue())) {
            helper.fail("An empty range produced " + slider.getNormalizedValue());
            return;
        }
        helper.succeed();
    }

    public static void listenersOnlyFireOnRealChanges(GameTestHelper helper) {
        var slider = new Slider.Horizontal().setRange(0, 10);
        var calls = new AtomicInteger();
        slider.setOnValueChanged(value -> calls.incrementAndGet());

        slider.setValue(5f);
        slider.setValue(5f);          // same value, no notification
        slider.setValue(7f, false);   // explicitly silent
        if (calls.get() != 1) {
            helper.fail("Expected exactly one notification, got " + calls.get());
            return;
        }
        if (slider.getValue() != 7f) {
            helper.fail("The silent set did not apply, value is " + slider.getValue());
            return;
        }
        helper.succeed();
    }

    public static void stepMovesByAFractionOfTheRange(GameTestHelper helper) {
        var slider = new Slider.Horizontal().setRange(0, 100);
        slider.sliderStyle(style -> style.sliderStep(0.25f));
        slider.setValue(0f);
        slider.slideValue(slider.getSliderStyle().sliderStep());
        if (slider.getValue() != 25f) {
            helper.fail("Expected a quarter step to land on 25, got " + slider.getValue());
            return;
        }
        helper.succeed();
    }
}
