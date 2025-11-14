package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor;


import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;

public record StyledLine(int line, List<StyledText> text) {
    public float getWidth(Font font, Style style) {
        var w = 0f;
        for (var t : text) {
            w += font.getSplitter().stringWidth(Component.literal(t.text()).withStyle(style).withStyle(t.style()));
        }
        return w;
    }
}
