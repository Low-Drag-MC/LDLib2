package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextAreaConfigurator extends ValueConfigurator<String[]> {
    public final TextArea textArea;

    public TextAreaConfigurator(String name, Supplier<String[]> supplier, Consumer<String[]> onUpdate, @Nonnull String[] defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) value = defaultValue;
        inlineContainer.addChild(textArea = new TextArea());
        textArea.setLinesResponder(this::updateValueActively);
        textArea.setValue(value, false);
    }

    @Override
    protected void onValueUpdatePassively(String[] newValue) {
        if (newValue == null) newValue = defaultValue;
        if (Arrays.equals(newValue, value)) return;
        super.onValueUpdatePassively(newValue);
        textArea.setValue(newValue, false);
    }

}
