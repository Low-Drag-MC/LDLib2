package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configurator for TaffyDimension values (width, height, size, min/max-size, etc.)
 */
public class DimensionConfigurator extends ValueConfigurator<TaffyDimension> {
    public enum Unit {
        AUTO, LENGTH, PERCENT,
        MIN_CONTENT,
        MAX_CONTENT,
        FIT_CONTENT,
        STRETCH, CONTENT
    }

    public final TextField textField;
    public final Selector<Unit> unitSelector;

    public DimensionConfigurator(String name, Supplier<TaffyDimension> supplier, Consumer<TaffyDimension> onUpdate, @Nonnull TaffyDimension defaultValue, boolean forceUpdate) {
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
        unitSelector.setCandidates(List.of(
                Unit.AUTO,
                Unit.LENGTH,
                Unit.PERCENT,
                Unit.MIN_CONTENT,
                Unit.MAX_CONTENT,
                Unit.FIT_CONTENT,
                Unit.STRETCH,
                Unit.CONTENT
        ));
        updateSelector();

        unitSelector.setOnValueChanged(unit -> {
            switch (unit) {
                case AUTO -> updateValueActively(TaffyDimension.AUTO);
                case LENGTH -> updateValueActively(TaffyDimension.length(0));
                case PERCENT -> updateValueActively(TaffyDimension.percent(0));
                case MIN_CONTENT -> updateValueActively(TaffyDimension.minContent());
                case MAX_CONTENT -> updateValueActively(TaffyDimension.maxContent());
                case FIT_CONTENT -> updateValueActively(TaffyDimension.fitContent());
                case STRETCH -> updateValueActively(TaffyDimension.stretch());
                case CONTENT -> updateValueActively(TaffyDimension.content());
            }
            updateTextFieldValue();
        });

        unitSelector.setCandidateUIProvider(UIElementProvider.text(value -> switch (value) {
            case AUTO -> Component.literal("auto");
            case LENGTH -> Component.literal("px");
            case PERCENT -> Component.literal("%");
            case MIN_CONTENT -> Component.literal("min-content");
            case MAX_CONTENT -> Component.literal("max-content");
            case FIT_CONTENT -> Component.literal("fit-content");
            case STRETCH -> Component.literal("stretch");
            case CONTENT -> Component.literal("content");
        }));

        textField.layout(layout -> {
            layout.flex(2);
        });
        textField.setNumbersOnlyFloat(-Float.MAX_VALUE, Float.MAX_VALUE);
        textField.setWheelDur(1f);
        textField.setTextResponder(this::onNumberUpdate);
        updateTextFieldValue();
    }

    public DimensionConfigurator setCandidates(List<Unit> units) {
        unitSelector.setCandidates(units);
        return this;
    }

    protected void updateTextFieldValue() {
        assert value != null;
        if (value.isLength() || value.isPercent()) {
            // For percentage, convert from 0.0-1.0 to 0-100 for display
            float displayValue = value.isPercent() ? value.getValue() * 100f : value.getValue();
            textField.setText(String.valueOf(displayValue), false);
            textField.setDisplay(true);
        } else {
            textField.setDisplay(false);
        }
    }

    protected void updateSelector() {
        assert value != null;
        var unit = switch (value.getType()) {
            case LENGTH -> Unit.LENGTH;
            case PERCENT -> Unit.PERCENT;
            case AUTO, CALC -> Unit.AUTO;
            case MIN_CONTENT -> Unit.MIN_CONTENT;
            case MAX_CONTENT -> Unit.MAX_CONTENT;
            case FIT_CONTENT -> Unit.FIT_CONTENT;
            case STRETCH -> Unit.STRETCH;
            case CONTENT -> Unit.CONTENT;
        };
        unitSelector.setValue(unit, false);
    }

    @Override
    protected void onValueUpdatePassively(TaffyDimension newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        assert value != null;
        if (value.isLength()) {
            updateValueActively(TaffyDimension.length(number));
        } else if (value.isPercent()) {
            // Convert from 0-100 display to 0.0-1.0 internal representation
            updateValueActively(TaffyDimension.percent(number / 100f));
        }
    }
}
