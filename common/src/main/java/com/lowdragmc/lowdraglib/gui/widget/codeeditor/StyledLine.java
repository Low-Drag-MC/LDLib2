package com.lowdragmc.lowdraglib.gui.widget.codeeditor;


import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

public record StyledLine(int line, List<StyledText> text) {
    public int getWidth(Font font, Style style) {
        var w = 0;
        for (var t : text) {
            w += font.width(Component.literal(t.getText()).withStyle(style).withStyle(t.getStyle())) - 1;
        }
        return w;
    }
}
