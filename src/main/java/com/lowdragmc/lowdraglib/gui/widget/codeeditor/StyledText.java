package com.lowdragmc.lowdraglib.gui.widget.codeeditor;

import net.minecraft.network.chat.Style;

public class StyledText {
    private String text;
    private Style style;

    public StyledText(String text, Style style) {
        this.text = text;
        this.style = style;
    }

    // Getter 方法
    public String getText() {
        return text;
    }

    public Style getStyle() {
        return style;
    }
}

