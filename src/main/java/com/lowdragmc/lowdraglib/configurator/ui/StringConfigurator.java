package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.gui.ui.elements.TextField;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringConfigurator extends ValueConfigurator<String> {
    protected final TextField textField;
    protected boolean isResourceLocation;
    protected boolean isCompoundTag;

    public StringConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate, @Nonnull String defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(textField = new TextField());
        textField.setTextResponder(this::onStringUpdate);
        textField.setText(value, false);
    }

    public StringConfigurator setResourceLocation(boolean resourceLocation) {
        isResourceLocation = resourceLocation;
        textField.setResourceLocationOnly();
        return this;
    }

    public StringConfigurator setCompoundTag(boolean compoundTag) {
        isCompoundTag = compoundTag;
        textField.setCompoundTagOnly();
        return this;
    }

    @Override
    protected void onValueUpdate(String newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        textField.setText(newValue, false);
    }

    private void onStringUpdate(String s) {
        value = s;
        updateValue();
    }
}
