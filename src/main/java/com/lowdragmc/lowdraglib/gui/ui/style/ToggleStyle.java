package com.lowdragmc.lowdraglib.gui.ui.style;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true, fluent = true)
public class ToggleStyle extends Style {
    @Getter
    @Setter
    private IGuiTexture baseTexture = Sprites.RECT_DARK;
    @Getter
    @Setter
    private IGuiTexture hoverTexture = new GuiTextureGroup(Sprites.RECT_DARK, ColorPattern.WHITE.borderTexture(-1));
    @Getter
    @Setter
    private IGuiTexture unmarkTexture = IGuiTexture.EMPTY;
    @Getter
    @Setter
    private IGuiTexture markTexture = Icons.CHECK_SPRITE;

    public ToggleStyle(UIElement holder) {
        super(holder);
    }
}
