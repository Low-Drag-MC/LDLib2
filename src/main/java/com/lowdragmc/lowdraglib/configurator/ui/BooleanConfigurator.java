package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.gui.ui.elements.Toggle;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanConfigurator extends ValueConfigurator<Boolean> {
    public final Toggle toggle;

    public BooleanConfigurator(String name, Supplier<Boolean> supplier, Consumer<Boolean> onUpdate, @Nonnull Boolean defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChildren(toggle = new Toggle());
        toggle.toggleLabel.setText("");
        toggle.setOn(value, false);
        toggle.setOnToggleChanged(this::updateValueActively);
    }

    @Override
    protected void onValueUpdatePassively(Boolean newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        toggle.setOn(newValue, false);
    }
}
