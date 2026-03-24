package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.data.LengthPercent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Configurator for LengthPercent values (px or %).
 */
public class LengthPercentConfigurator extends ValueConfigurator<LengthPercent> {
    public enum Unit {
        PX, PERCENT
    }

    public final TextField textField;
    public final Selector<Unit> unitSelector;

    public LengthPercentConfigurator(String name, Supplier<LengthPercent> supplier, Consumer<LengthPercent> onUpdate, @Nonnull LengthPercent defaultValue, boolean forceUpdate) {
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
        unitSelector.setCandidates(List.of(Unit.PX, Unit.PERCENT));
        updateSelector();

        unitSelector.setOnValueChanged(unit -> {
            assert value != null;
            switch (unit) {
                case PX -> updateValueActively(LengthPercent.px(value.getValue()));
                case PERCENT -> updateValueActively(LengthPercent.percent(value.getValue()));
            }
        });

        unitSelector.setCandidateUIProvider(UIElementProvider.text(value -> switch (value) {
            case PX -> Component.literal("px");
            case PERCENT -> Component.literal("%");
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
        textField.setText(String.valueOf(value.getValue()), false);
    }

    protected void updateSelector() {
        assert value != null;
        unitSelector.setValue(value.isPercent() ? Unit.PERCENT : Unit.PX, false);
    }

    @Override
    protected void onValueUpdatePassively(LengthPercent newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        updateTextFieldValue();
        updateSelector();
    }

    private void onNumberUpdate(String s) {
        var number = Float.parseFloat(s);
        assert value != null;
        updateValueActively(new LengthPercent(number, value.isPercent()));
    }
}
