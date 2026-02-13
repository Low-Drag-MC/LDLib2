package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.appliedenergistics.yoga.numeric.FloatOptional;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatOptionalConfigurator extends ValueConfigurator<FloatOptional> {
    public final TextField textField;
    public final Selector<Boolean> definedSelector;
    @Getter
    protected Float min, max, wheel;

    public FloatOptionalConfigurator(String name, Supplier<FloatOptional> supplier, Consumer<FloatOptional> onUpdate, @Nonnull FloatOptional defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }
        inlineContainer.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
        });
        inlineContainer.addChildren(textField = new TextField(), definedSelector = new Selector<>());

        definedSelector.buttonIcon.setDisplay(false);
        definedSelector.layout(layout -> {
            layout.flex(1);
        });
        definedSelector.setCandidates(List.of(false, true));
        updateSelector();

        definedSelector.setOnValueChanged(defined -> {
            if (defined) {
                updateValueActively(FloatOptional.of(min == null ? 0 : min));
            } else {
                updateValueActively(FloatOptional.of());
            }
            updateTextFieldValue();
        });
        definedSelector.setCandidateUIProvider(UIElementProvider.text(defined -> defined ?
                Component.literal("-") : Component.translatable("initial")));

        textField.layout(layout -> {
            layout.flex(2);
        }).setDisplay(value.isDefined());
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
        textField.setDisplay(value.isDefined());
        textField.setText(String.valueOf(value.getValue()), false);
    }

    protected void updateSelector() {
        assert value != null;
        definedSelector.setValue(value.isDefined(), false);
    }

    @Override
    protected void onValueUpdatePassively(FloatOptional newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        updateValueActively(Optional.ofNullable(definedSelector.getValue()).orElse(false) ? FloatOptional.of(number) : FloatOptional.of());
    }
}
