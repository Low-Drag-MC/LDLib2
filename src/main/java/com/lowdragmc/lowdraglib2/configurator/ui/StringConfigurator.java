package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StringConfigurator extends ValueConfigurator<String> {
    public final TextField textField;
    @Getter
    protected boolean isResourceLocation;

    public StringConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate, @Nonnull String defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);
        setCopyDirect(true);

        if (value == null) value = defaultValue;
        inlineContainer.addChild(textField = new TextField());
        textField.setTextResponder(this::updateValueActively);
        textField.setText(value, false);
    }

    public StringConfigurator setResourceLocation(boolean resourceLocation) {
        isResourceLocation = resourceLocation;
        textField.setResourceLocationOnly();
        return this;
    }

    public StringConfigurator setTextValidator(Predicate<String> validator) {
        textField.setTextValidator(validator);
        return this;
    }

    @Override
    protected void onValueUpdatePassively(String newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        if (isResourceLocation && value != null) {
            if (ResourceLocation.parse(newValue).equals(ResourceLocation.parse(value))) return;
        }
        super.onValueUpdatePassively(newValue);
        textField.setText(newValue, false);
    }

}
