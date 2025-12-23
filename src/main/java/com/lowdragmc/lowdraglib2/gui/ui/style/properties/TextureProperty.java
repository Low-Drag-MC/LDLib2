package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.TextureValue;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public class TextureProperty extends Property<IGuiTexture> {
    public TextureProperty(String name, IGuiTexture initialValue) {
        super(name, IGuiTexture.class, IGuiTexture.CODEC, initialValue, TextureValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    private IGuiTexture interpolate(IGuiTexture from, IGuiTexture to, float lerp) {
        return from.interpolate(to, lerp);
    }
}
