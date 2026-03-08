package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TextFieldClientSupport {
    private TextFieldClientSupport() {
    }

    static void copyHighlightedText(TextField field) {
        ClipboardManager.INSTANCE.copyDirect(field.getClipboardSelectionText());
    }

    static String getClipboardText() {
        var pasted = ClipboardManager.INSTANCE.paste();
        return pasted instanceof String text ? text : "";
    }

    static float computeDisplayOffset(TextField field) {
        var font = Minecraft.getInstance().font;
        var scale = field.getTextFieldStyle().fontSize() / font.lineHeight;
        var cursorPosX = font.getSplitter().stringWidth(TextUtilities.withFont(field.getRawPrefix(field.getCursorPos()), field.getTextFieldStyle().font())) * scale;
        var width = field.getContentWidth();
        float rightPad = 1f;
        var displayOffset = field.getDisplayOffset();
        var rel = cursorPosX - displayOffset;
        if (rel > width - rightPad || rel < 0) {
            return Math.max(cursorPosX - width + rightPad, 0);
        }
        return displayOffset;
    }

    static int getCursorUnderMouseX(TextField field, double mouseX) {
        var x = field.getContentX();
        var font = Minecraft.getInstance().font;
        var textFont = field.getTextFieldStyle().font();
        var scale = field.getTextFieldStyle().fontSize() / font.lineHeight;
        var availableWidth = ((mouseX - x + field.getDisplayOffset()) * scale);

        var lineWithFont = TextUtilities.withFont(field.getRawText(), textFont);
        var subWithFont = font.substrByWidth(lineWithFont, (int) availableWidth);
        float fullLength = font.getSplitter().stringWidth(lineWithFont) * scale;
        float subLength = font.getSplitter().stringWidth(subWithFont) * scale;
        int col;
        if (subLength >= fullLength) {
            col = field.getRawText().length();
        } else {
            var sub = subWithFont.getString();
            float nextCharWidth = font.getSplitter().stringWidth(TextUtilities.withFont(field.getRawCharacterAt(sub.length()), textFont)) * scale;
            col = (availableWidth - subLength) - nextCharWidth / 2f > 0 ? sub.length() + 1 : sub.length();
        }
        return Mth.clamp(col, 0, field.getRawText().length());
    }

    static Tuple<FormattedCharSequence, Float> computeFormattedLine(TextField field) {
        var font = field.getTextFieldStyle().font();
        var formattedText = field.getDisplayText();
        var textWithFont = font.equals(FontDescription.DEFAULT.id())
                ? formattedText
                : formattedText.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new FontDescription.Resource(font)));
        var lines = TextUtilities.computeFormattedLines(
                Minecraft.getInstance().font,
                textWithFont,
                field.getTextFieldStyle().fontSize(),
                Float.MAX_VALUE
        );
        if (lines.isEmpty()) {
            return new Tuple<>(FormattedCharSequence.EMPTY, 0f);
        }
        return lines.getFirst();
    }
}
