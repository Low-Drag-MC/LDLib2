package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class ButtonStyle extends Style {
    @Getter
    @Setter
    private IGuiTexture defaultTexture = Sprites.RECT_RD;
    @Getter
    @Setter
    private IGuiTexture hoverTexture = Sprites.RECT_RD_LIGHT;
    @Getter
    @Setter
    private IGuiTexture pressedTexture = Sprites.RECT_RD_DARK;

    public ButtonStyle(UIElement holder) {
        super(holder);
    }
}
