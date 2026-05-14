package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public class AutoFloatConfigurator extends ValueConfigurator<Float> {
    public enum Unit {
        AUTO, NUMBER;

        public static Unit parseFloat(float value) {
            return Float.isNaN(value) ? AUTO : NUMBER;
        }
    }

    public final TextField textField;
    public final Selector<Unit> unitSelector;
    protected float min = -Float.MAX_VALUE;
    protected float max = Float.MAX_VALUE;
    protected float wheel = 0.1f;

    public AutoFloatConfigurator(String name, Supplier<Float> supplier, Consumer<Float> onUpdate, @Nonnull Float defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) {
            value = defaultValue;
        }

        inlineContainer.layout(layout -> layout.flexDirection(FlexDirection.ROW));
        inlineContainer.addChildren(textField = new TextField(), unitSelector = new Selector<>());

        textField.layout(layout -> layout.flex(2));
        textField.setNumbersOnlyFloat(min, max);
        textField.setWheelDur(wheel);
        textField.setTextResponder(this::onNumberUpdate);

        unitSelector.buttonIcon.setDisplay(false);
        unitSelector.layout(layout -> layout.flex(1));
        unitSelector.setCandidates(List.of(Unit.AUTO, Unit.NUMBER));
        unitSelector.setValue(Unit.parseFloat(value));
        unitSelector.setCandidateUIProvider(UIElementProvider.text(value -> switch (value) {
            case NUMBER -> Component.literal("number");
            case AUTO -> Component.literal("auto");
        }));
        unitSelector.setOnValueChanged(this::onUnitUpdate);

        updateTextFieldValue();
        updateSelector();
    }

    public AutoFloatConfigurator setRange(float min, float max) {
        this.min = min;
        this.max = max;
        textField.setNumbersOnlyFloat(min, max);
        textField.setWheelDur(wheel);
        return this;
    }

    public AutoFloatConfigurator setWheel(float wheel) {
        if (wheel == 0) return this;
        this.wheel = wheel;
        textField.setWheelDur(wheel);
        return this;
    }

    protected void updateTextFieldValue() {
        assert value != null;
        if (Float.isNaN(value)) {
            textField.setDisplay(false);
        } else {
            textField.setText(String.valueOf(value), false);
            textField.setDisplay(true);
        }
    }

    protected void updateSelector() {
        assert value != null;
        unitSelector.setValue(Float.isNaN(value) ? Unit.AUTO : Unit.NUMBER, false);
    }

    @Override
    protected void onValueUpdatePassively(Float newValue) {
        if (newValue == null) newValue = defaultValue;
        if (Objects.equals(newValue, value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    @Override
    protected boolean canDropObject(@Nullable Object object) {
        return object instanceof Number || super.canDropObject(object);
    }

    @Override
    protected void onDropObject(@Nullable Object object) {
        if (object instanceof Number number) {
            updateValueActively(number.floatValue());
            updateTextFieldValue();
            updateSelector();
        } else {
            super.onDropObject(object);
        }
    }

    private void onUnitUpdate(Unit unit) {
        switch (unit) {
            case AUTO -> updateValueActively(Float.NaN);
            case NUMBER -> updateValueActively(value == null || Float.isNaN(value) ? 0f : value);
        }
        updateTextFieldValue();
    }

    private void onNumberUpdate(String s) {
        updateValueActively(Float.parseFloat(s));
    }
}
