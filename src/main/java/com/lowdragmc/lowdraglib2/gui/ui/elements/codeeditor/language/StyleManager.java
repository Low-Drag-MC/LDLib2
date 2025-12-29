package com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;

@Getter
public class StyleManager {
    public final static StyleManager DEFAULT = new StyleManager();
    
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

        styleMap.put(TokenTypes.CSS_CLASS_SELECTOR.name, Style.EMPTY.withColor(ColorPattern.YELLOW.color));
        styleMap.put(TokenTypes.CSS_ID_SELECTOR.name, Style.EMPTY.withColor(ColorPattern.BRIGHT_CYAN.color));
        styleMap.put(TokenTypes.CSS_PROPERTY.name, Style.EMPTY.withColor(ColorPattern.ORANGE.color));
        styleMap.put(TokenTypes.CSS_UNIT.name, Style.EMPTY.withColor(ColorPattern.CYAN.color));
        styleMap.put(TokenTypes.CSS_COLOR.name, Style.EMPTY.withColor(ColorPattern.MAGENTA.color));

        styleMap.put(TokenTypes.XML_COMMENT.name, Style.EMPTY.withColor(ColorPattern.GRAY.color));
        styleMap.put(TokenTypes.XML_CDATA.name, Style.EMPTY.withColor(ColorPattern.GREEN.color));
        styleMap.put(TokenTypes.XML_ATTRIBUTE_VALUE.name, Style.EMPTY.withColor(ColorPattern.GREEN.color));
        styleMap.put(TokenTypes.XML_ATTRIBUTE_NAME.name, Style.EMPTY.withColor(ColorPattern.ORANGE.color));
        styleMap.put(TokenTypes.XML_ENTITY_REF.name, Style.EMPTY.withColor(ColorPattern.MAGENTA.color));
        styleMap.put(TokenTypes.XML_TAG_NAME.name, Style.EMPTY.withColor(ColorPattern.YELLOW.color));
        styleMap.put(TokenTypes.XML_TAG_END.name, Style.EMPTY.withColor(ColorPattern.YELLOW.color));
        styleMap.put(TokenTypes.XML_EQ.name, Style.EMPTY.withColor(ColorPattern.WHITE.color));
    }

    public Style getStyleForTokenType(TokenType type) {
        return styleMap.getOrDefault(type.name, defaultStyle);
    }
}