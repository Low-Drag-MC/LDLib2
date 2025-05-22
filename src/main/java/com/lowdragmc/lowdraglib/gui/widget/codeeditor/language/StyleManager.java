package com.lowdragmc.lowdraglib.gui.widget.codeeditor.language;

import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;

@Getter
public class StyleManager {
    private final Map<String, Style> styleMap = new HashMap<>();
    @Setter
    public Style defaultStyle = Style.EMPTY.withColor(-1);

    public StyleManager() {
        styleMap.put(TokenTypes.KEYWORD.name, Style.EMPTY.withColor(ColorPattern.ORANGE.color));
        styleMap.put(TokenTypes.IDENTIFIER.name, Style.EMPTY.withColor(ColorPattern.WHITE.color));
        styleMap.put(TokenTypes.STRING.name, Style.EMPTY.withColor(ColorPattern.GREEN.color));
        styleMap.put(TokenTypes.COMMENT.name, Style.EMPTY.withColor(ColorPattern.GRAY.color));
        styleMap.put(TokenTypes.NUMBER.name, Style.EMPTY.withColor(ColorPattern.CYAN.color));
        styleMap.put(TokenTypes.OPERATOR.name, Style.EMPTY.withColor(ColorPattern.WHITE.color));
    }

    public Style getStyleForTokenType(TokenType type) {
        return styleMap.getOrDefault(type.name, defaultStyle);
    }
}