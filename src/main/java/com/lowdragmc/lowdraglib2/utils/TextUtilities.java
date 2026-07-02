package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;

import java.util.List;
import java.util.Optional;

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
    public static List<Tuple<FormattedCharSequence, Float>> computeFormattedLines(
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
                    return new Tuple<>(line, realLineWidth);
                })
                .toList();
    }

    public Component withFont(String text, ResourceLocation font) {
        return font.equals(Style.DEFAULT_FONT) ? Component.literal(text) : Component.literal(text).withStyle(style -> style.withFont(font));
    }

    public Component withFont(Component component, ResourceLocation font) {
        return font.equals(Style.DEFAULT_FONT) ? component : component.copy().withStyle(style -> style.withFont(font));
    }

    /**
     * Returns a styled copy of {@code styled} containing only its first {@code charCount} characters, while
     * preserving each run's style (font, bold, ...). Measuring the width of the result therefore accounts for
     * style-dependent advances (e.g. bold adds ~1px per character), which a plain-text measurement misses.
     * This keeps caret/selection positions aligned with what is actually rendered.
     *
     * @param styled    The styled content, in the same styling as it will be rendered.
     * @param charCount The number of leading characters to keep.
     * @return A styled component with the first {@code charCount} characters.
     */
    public Component truncateStyled(Component styled, int charCount) {
        MutableComponent result = Component.empty();
        if (charCount <= 0) return result;
        var remaining = new int[]{charCount};
        styled.visit((style, content) -> {
            if (remaining[0] > 0) {
                var take = Math.min(content.length(), remaining[0]);
                result.append(Component.literal(content.substring(0, take)).withStyle(style));
                remaining[0] -= take;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

}
