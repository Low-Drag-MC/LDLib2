package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import lombok.Getter;
import org.appliedenergistics.yoga.YogaDisplay;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatOptionalConfigurator extends ValueConfigurator<FloatOptional> {
    public final Toggle toggle;
    public final TextField textField;
    @Getter
    protected Float min, max, wheel;

    public FloatOptionalConfigurator(String name, Supplier<FloatOptional> supplier, Consumer<FloatOptional> onUpdate, @Nonnull FloatOptional defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
        });
        inlineContainer.addChildren(toggle = new Toggle(), textField = new TextField());

        toggle.noText().setOn(value.isDefined(), false).setOnToggleChanged(isOn -> {
            updateValueActively(isOn ? FloatOptional.of(min == null ? 0 : min) : FloatOptional.of());
            updateTextFieldValue();
        });

        textField.layout(layout -> {
            layout.setFlex(1);
        }).setDisplay(value.isDefined() ? YogaDisplay.FLEX : YogaDisplay.NONE);
        textField.setTextResponder(this::onNumberUpdate);
        updateTextField();
    }

    @Override
    protected void onPaste(FloatOptional pasted) {
        if ((max != null && pasted.getValue() <= max) && (min != null && pasted.getValue() >= min)) {
            super.onPaste(pasted);
        }
    }

    @Override
    protected void onDropObject(@Nonnull Object object) {
        if (object instanceof FloatOptional floatOptional && (max != null && floatOptional.getValue() <= max) && (min != null && floatOptional.getValue() >= min)) {
            updateValueActively(floatOptional);
            updateTextFieldValue();
        }
    }

    public FloatOptionalConfigurator setRange(Float min, Float max) {
        this.min = min;
        this.max = max;
        updateTextField();
        return this;
    }

    public FloatOptionalConfigurator setWheel(Float wheel) {
        if (wheel.doubleValue() == 0) return this;
        this.wheel = wheel;
        updateTextField();
        return this;
    }

    protected void updateTextField() {
        textField.setNumbersOnlyFloat(min == null ? -Float.MAX_VALUE : min, max == null ? Float.MAX_VALUE : max);
        var wheelValue = 0.1f;
        if (wheel != null) wheelValue = wheel;
        textField.setWheelDur(wheelValue);
        updateTextFieldValue();
    }

    protected void updateTextFieldValue() {
        assert value != null;
        textField.setDisplay(value.isDefined() ? YogaDisplay.FLEX : YogaDisplay.NONE);
        textField.setText(String.valueOf(value.getValue()), false);
    }

    @Override
    protected void onValueUpdatePassively(FloatOptional newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        toggle.setOn(newValue.isDefined(), false);
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        updateValueActively(toggle.isOn() ? FloatOptional.of(number) : FloatOptional.of());
    }
}
