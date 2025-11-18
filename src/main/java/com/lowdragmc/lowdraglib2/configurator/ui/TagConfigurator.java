package com.lowdragmc.lowdraglib2.configurator.ui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TagField;
import net.minecraft.nbt.Tag;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TagConfigurator extends ValueConfigurator<Tag> {
    public final TagField tagField;

    public TagConfigurator(String name, Supplier<Tag> supplier, Consumer<Tag> onUpdate, @Nonnull Tag defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setCopiable(value -> value);

        if (value == null) value = defaultValue;
        inlineContainer.addChild(tagField = new TagField());
        tagField.setTagResponder(this::updateValueActively);
        tagField.setValue(value, false);
    }


    @Override
    protected void onValueUpdatePassively(Tag newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        tagField.setValue(newValue, false);
    }
}
