package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class TextElementRenderer {
    private TextElementRenderer() {
    }

    public static void recompute(TextElement textElement) {
        float maxWidth;
        var wrap = textElement.getTextStyle().textWrap();
        var font = textElement.getTextStyle().font();
        if (textElement.getTextStyle().adaptiveWidth() || wrap == TextWrap.NONE || wrap == TextWrap.ROLL || wrap == TextWrap.HOVER_ROLL) {
            maxWidth = Float.MAX_VALUE;
        } else {
            maxWidth = textElement.getContentWidth();
        }
        textElement.setFormattedLines(TextUtilities.computeFormattedLines(
                Minecraft.getInstance().font,
                TextUtilities.withFont(textElement.getText(), font),
                textElement.getTextStyle().fontSize(),
                maxWidth
        ));
        if (textElement.getTextStyle().adaptiveWidth()) {
            Style.importantPipeline(textElement.getLayout(), layout -> layout.width(textElement.getFormattedLines().stream().findFirst().map(Tuple::getB).orElse(0f) + textElement.getSizeWidth() - textElement.getContentWidth()));
        } else {
            textElement.getStyleBag().removeCandidates(LayoutProperties.WIDTH, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
        if (textElement.getTextStyle().adaptiveHeight()) {
            Style.importantPipeline(textElement.getLayout(), layout -> layout.height(textElement.getFormattedLines().size() * (textElement.getTextStyle().fontSize() + textElement.getTextStyle().lineSpacing()) - textElement.getTextStyle().lineSpacing() + textElement.getSizeHeight() - textElement.getContentHeight()));
        } else {
            textElement.getStyleBag().removeCandidates(LayoutProperties.HEIGHT, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
    }

    public static void drawBackgroundAdditional(TextElement textElement, GUIContext context) {
        var formattedLines = textElement.getFormattedLines();
        if (formattedLines.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        var defaultLineHeight = font.lineHeight;
        var x = textElement.getContentX();
        var y = textElement.getContentY();
        var width = textElement.getContentWidth();
        var height = textElement.getContentHeight();
        var hAlign = textElement.getTextStyle().textAlignHorizontal();
        var vAlign = textElement.getTextStyle().textAlignVertical();
        var lineHeight = textElement.getTextStyle().fontSize();
        var lineSpacing = textElement.getTextStyle().lineSpacing();
        var color = textElement.getTextStyle().textColor();
        var dropShadow = textElement.getTextStyle().textShadow();
        var scale = lineHeight / defaultLineHeight;

        List<Tuple<net.minecraft.util.FormattedCharSequence, Float>> displayLines = formattedLines;
        var textWrap = textElement.getTextStyle().textWrap();
        if (textWrap == TextWrap.HIDE) {
            displayLines = formattedLines.subList(0, Math.min(1, formattedLines.size()));
        }

        var totalTextHeight = displayLines.size() * (lineHeight + lineSpacing) - lineSpacing;
        var startY = switch (vAlign) {
            case TOP -> y;
            case CENTER -> y + (height - totalTextHeight) / 2;
            case BOTTOM -> y + (height - totalTextHeight);
        };

        var roll = textWrap == TextWrap.ROLL || (textWrap == TextWrap.HOVER_ROLL && textElement.isSelfOrChildHover());
        for (int i = 0; i < displayLines.size(); i++) {
            var tuple = displayLines.get(i);
            var line = tuple.getA();
            float lineWidth = tuple.getB();
            var lineX = x;
            if (roll && lineWidth > width) {
                var rollSpeed = textElement.getTextStyle().rollSpeed();
                float totalW = width + lineWidth + 10;
                var t = rollSpeed > 0 ? ((((rollSpeed * Math.abs((int) (System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / (totalW)) : 0.5;
                lineX = (float) (x + width - totalW * t);
            } else {
                lineX = switch (hAlign) {
                    case LEFT -> x;
                    case CENTER -> (lineWidth > width) ? x : (x + (width - lineWidth) / 2);
                    case RIGHT -> x + (width - lineWidth);
                };
            }

            var lineY = startY + i * (lineHeight + lineSpacing);
            context.pose.pushPose();
            context.pose.translate(lineX, lineY);
            context.pose.scale(scale, scale);
            context.graphics.drawString(font, line, 0, 0, color, dropShadow);
            context.pose.popPose();
        }
    }
}
