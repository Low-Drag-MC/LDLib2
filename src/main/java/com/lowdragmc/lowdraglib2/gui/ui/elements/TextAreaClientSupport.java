package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ui.data.Cursor;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class TextAreaClientSupport {
    private TextAreaClientSupport() {
    }

    static void copyHighlightedText(TextArea area) {
        ClipboardManager.INSTANCE.copyDirect(area.getClipboardSelectionText());
    }

    static String getClipboardText() {
        var pasted = ClipboardManager.INSTANCE.paste();
        return pasted instanceof String text ? text : "";
    }

    static float scale(TextArea area) {
        return area.getTextAreaStyle().fontSize() / Minecraft.getInstance().font.lineHeight;
    }

    static float getMaxWidth(TextArea area) {
        var font = Minecraft.getInstance().font;
        var scale = scale(area);
        var max = 0f;
        for (String line : area.lines) {
            max = Math.max(font.getSplitter().stringWidth(TextUtilities.withFont(line, area.getTextAreaStyle().font())) * scale, max);
        }
        return max;
    }

    static Tuple<Float, Float> computeVisibleScroll(TextArea area) {
        var width = area.contentView.getContentWidth();
        var height = area.contentView.getContentHeight();
        if (width == 0 || height == 0) {
            return new Tuple<>(area.getScrollX(), area.getScrollY());
        }

        var font = Minecraft.getInstance().font;
        var scale = scale(area);
        var currentLine = area.lines.get(area.getCursorLine());
        float cursorX = font.getSplitter().stringWidth(TextUtilities.withFont(currentLine.substring(0, area.getCursorCol()), area.getTextAreaStyle().font())) * scale;
        float lineTop = area.getCursorLine() * area.lineHeight();
        float lineBottom = lineTop + area.getTextAreaStyle().fontSize();

        float scrollX = area.getScrollX();
        float scrollY = area.getScrollY();
        float rightPad = 1f;
        float preferredScrollX = Math.max(0, cursorX - width + rightPad);

        if (cursorX - scrollX > width - rightPad || cursorX - scrollX < 0) {
            scrollX = preferredScrollX;
        }

        if (lineBottom - scrollY > height) {
            scrollY = lineBottom - height;
        } else if (lineTop - scrollY < 0) {
            scrollY = Math.max(lineTop, 0);
        }

        float contentTotalHeight = Math.max(area.lineHeight(), area.lines.size() * area.lineHeight());
        scrollY = Mth.clamp(Float.isNaN(scrollY) ? 0 : scrollY, 0, Math.max(0, contentTotalHeight - height));
        scrollX = Math.max(0, Float.isNaN(scrollX) ? 0 : scrollX);
        return new Tuple<>(scrollX, scrollY);
    }

    static Cursor getCursorUnderMouse(TextArea area, double mouseX, double mouseY) {
        var x = area.contentView.getContentX();
        var y = area.contentView.getContentY();
        var scale = scale(area);
        var font = Minecraft.getInstance().font;
        var textFont = area.getTextAreaStyle().font();

        var relY = (float) (mouseY - y + area.getScrollY()) - 2;
        int line = Mth.clamp((int) Math.floor(relY / area.lineHeight()), 0, Math.max(0, area.lines.size() - 1));
        var lineText = area.lines.get(line);
        var relX = (float) (mouseX - x + area.getScrollX());

        var lineWithFont = TextUtilities.withFont(lineText, textFont);
        var subWithFont = font.substrByWidth(lineWithFont, (int) (relX / scale));
        float fullLength = font.getSplitter().stringWidth(lineWithFont) * scale;
        float subLength = font.getSplitter().stringWidth(subWithFont) * scale;
        int col;
        if (subLength >= fullLength) {
            col = lineText.length();
        } else {
            var sub = subWithFont.getString();
            float nextCharWidth = font.getSplitter().stringWidth(TextUtilities.withFont(lineText.substring(sub.length(), sub.length() + 1), textFont)) * scale;
            col = (relX - subLength) - nextCharWidth / 2f > 0 ? sub.length() + 1 : sub.length();
        }
        return new Cursor(line, Mth.clamp(col, 0, lineText.length()));
    }
}
