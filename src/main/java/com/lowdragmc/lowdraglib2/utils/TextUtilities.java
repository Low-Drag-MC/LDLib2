package com.lowdragmc.lowdraglib2.utils;

import it.unimi.dsi.fastutil.Pair;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

@UtilityClass
public final class TextUtilities {

    /**
     * Computes the formatted lines of text based on the given font, text, line height, and maximum width.
     * This method scales the text to fit within the specified maximum width and returns a list of tuples.
     *
     * @param font       The font to use for formatting the text.
     * @param text       The text to format.
     * @param lineHeight The height of each line in pixels.
     * @param maxWidth   The maximum width of the text in pixels.
     * @return A list of tuples containing the formatted text and its width.
     */
    public static List<Pair<FormattedCharSequence, Float>> computeFormattedLines(
            Font font,
            FormattedText text,
            float lineHeight,
            float maxWidth
    ) {
        var defaultLineHeight = font.lineHeight;
        var scale = lineHeight / defaultLineHeight;
        var maxWidthScaled = (int) (maxWidth / scale);
        var formattedLines = font.split(text, maxWidthScaled);
        return formattedLines.stream()
                .map(line -> {
                    var lineWidth = font.getSplitter().stringWidth(line);
                    var realLineWidth = (lineWidth * scale);
                    return Pair.of(line, realLineWidth);
                })
                .toList();
    }

    public Component withFont(String text, Identifier font) {
        return font.equals(FontDescription.DEFAULT.id()) ? Component.literal(text) : Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(font)));
    }

    public Component withFont(Component component, Identifier font) {
        return font.equals(FontDescription.DEFAULT.id()) ? component : component.copy().withStyle(style -> style.withFont(new FontDescription.Resource(font)));
    }

}
