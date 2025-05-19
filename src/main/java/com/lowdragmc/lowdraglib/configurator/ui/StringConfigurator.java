package com.lowdragmc.lowdraglib.configurator.ui;

import com.lowdragmc.lowdraglib.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringConfigurator extends ValueConfigurator<String> {
    public final TextField textField;
    @Getter
    protected boolean isResourceLocation;
    @Getter
    protected boolean isCompoundTag;

    public StringConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate, @Nonnull String defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        if (value == null) value = defaultValue;
        inlineContainer.addChild(textField = new TextField());
        textField.setTextResponder(this::updateValueActively);
        textField.addEventListener(UIEvents.DRAG_PERFORM, this::onDragPerform);
        textField.setText(value, false);
    }

    private void onDragPerform(UIEvent event) {
        if (event.dragHandler.draggingObject instanceof CharSequence string) {
            updateValueActively(string.toString());
        }
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
