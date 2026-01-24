package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.YogaUnit;
import org.appliedenergistics.yoga.YogaValue;
import org.appliedenergistics.yoga.style.StyleLength;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StyleLengthConfigurator extends ValueConfigurator<StyleLength> {
    public final TextField textField;
    public final Selector<YogaUnit> unitSelector;

    public StyleLengthConfigurator(String name, Supplier<StyleLength> supplier, Consumer<StyleLength> onUpdate, @Nonnull StyleLength defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }

        inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
        });
        inlineContainer.addChildren(textField = new TextField(), unitSelector = new Selector<>());

        unitSelector.buttonIcon.setDisplay(false);
        unitSelector.layout(layout -> {
            layout.flex(1);
        });
        unitSelector.setCandidates(List.of(YogaUnit.POINT, YogaUnit.PERCENT, YogaUnit.AUTO, YogaUnit.UNDEFINED));
        updateSelector();

        unitSelector.setOnValueChanged(unit -> {
            switch (unit) {
                case UNDEFINED -> updateValueActively(StyleLength.undefined());
                case AUTO -> updateValueActively(StyleLength.ofAuto());
                case POINT -> updateValueActively(StyleLength.points(0));
                case PERCENT -> updateValueActively(StyleLength.percent(0));
                default -> throw new IllegalArgumentException("Invalid unit: " + unit);
            }
            updateTextFieldValue();
        });
        unitSelector.setCandidateUIProvider(UIElementProvider.text(value -> switch (value) {
            case AUTO -> Component.translatable("auto");
            case POINT -> Component.literal("px");
            case PERCENT -> Component.literal("%");
            case UNDEFINED -> Component.translatable("initial");
            case MAX_CONTENT -> Component.translatable("max-content");
            case FIT_CONTENT -> Component.translatable("fit-content");
            case STRETCH -> Component.translatable("stretch");
        }));

        textField.layout(layout -> {
            layout.flex(2);
        });
        textField.setNumbersOnlyFloat(-Float.MAX_VALUE, Float.MAX_VALUE);
        textField.setWheelDur(1f);
        textField.setTextResponder(this::onNumberUpdate);
        updateTextFieldValue();
    }

    protected void updateTextFieldValue() {
        assert value != null;
        if (value.isPercent() || value.isPoints()) {
            textField.setText(String.valueOf(value.asYogaValue().value), false);
            textField.setDisplay(true);
        } else {
            textField.setDisplay(false);
        }
    }

    protected void updateSelector() {
        assert value != null;
        unitSelector.setValue(
                value.isAuto() ? YogaUnit.AUTO :
                        value.isPoints() ? YogaUnit.POINT :
                                value.isPercent() ? YogaUnit.PERCENT :
                                        YogaUnit.UNDEFINED, false);
    }

    @Override
    protected void onValueUpdatePassively(StyleLength newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        assert value != null;
        if (value.isPoints() || value.isPercent()) {
            updateValueActively(value.isPercent() ?
                    StyleLength.fromYogaValue(YogaValue.percent(number)) :
                    value.isPoints() ? StyleLength.fromYogaValue(YogaValue.point(number)) : value);
        }
    }
}
