package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link Slider} is a registered UI element, so it has to be reachable by the names XML and the editor
 * use, and its value model has to behave like {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller}'s.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class SliderTest {

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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
