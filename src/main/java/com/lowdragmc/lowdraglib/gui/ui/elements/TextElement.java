package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib.utils.TextUtilities;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TextElement extends UIElement {
    @Getter
    private Component text = Component.empty();
    @Getter
    private boolean isComputed = false;
    /**
     * The formatted text to be displayed in each line and its width.
     */
    private List<Tuple<FormattedCharSequence, Float>> formattedLinesCache = Collections.emptyList();

    public void recompute() {
        this.isComputed = false;
        this.formattedLinesCache = Collections.emptyList();
    }

    @Override
    protected void onStyleChanged() {
        super.onStyleChanged();
        recompute();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        recompute();
    }

    @HideFromJS
    public TextElement setText(Component text) {
        this.text = text;
        recompute();
        return this;
    }

    @HideFromJS
    public TextElement setText(String text) {
        return setText(text,true);
    }

    public TextElement setText(String text, boolean translate) {
        return setText(translate ? Component.translatable(text) : Component.literal(text));
    }

    public TextElement kjs$setText(Component text) {
        return setText(text);
    }

    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    public List<Tuple<FormattedCharSequence, Float>> getFormattedLinesCache() {
        if (!isComputed) {
            formattedLinesCache = TextUtilities.computeFormattedLines(
                    getFont(),
                    text,
                    getStyle().fontSize(),
                    getStyle().textWrap() == TextWrap.WRAP ? getContentWidth() : Float.MAX_VALUE
            );
        }
        return formattedLinesCache;
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var formattedLines = getFormattedLinesCache();
        if (formattedLines.isEmpty()) return;
        graphics.drawManaged(() -> {
            var font = getFont();
            var defaultLineHeight = font.lineHeight;
            var x = getContentX();
            var y = getContentY();
            var width = getContentWidth();
            var height = getContentHeight();
            var hAlign = getStyle().textAlignHorizontal();
            var vAlign = getStyle().textAlignVertical();
            var lineHeight = getStyle().fontSize();
            var lineSpacing = getStyle().lineSpacing();
            var color = getStyle().textColor();
            var dropShadow = getStyle().textShadow();
            var scale = lineHeight / defaultLineHeight;


            // calculate the total height of the text
            var totalTextHeight = formattedLines.size() * (lineHeight + lineSpacing) - lineSpacing;
            var startY = y;

            // according to the vertical alignment, adjust the starting Y coordinate
            switch (vAlign) {
                case TOP -> startY = y;
                case CENTER -> startY = y + (height - totalTextHeight) / 2;
                case BOTTOM -> startY = y + (height - totalTextHeight);
            }

            // render each line of text
            for (int i = 0; i < formattedLines.size(); i++) {
                var tuple = formattedLines.get(i);
                var line = tuple.getA();
                float lineWidth = tuple.getB();
                var lineX = x;

                // according to the horizontal alignment, adjust the starting X coordinate
                switch (hAlign) {
                    case LEFT -> lineX = x;
                    case CENTER -> lineX = x + (width - lineWidth) / 2;
                    case RIGHT -> lineX = x + (width - lineWidth);
                }

                // calculate the Y coordinate of the current line (including line spacing)
                var lineY = startY + i * (lineHeight + lineSpacing);

                // make sure the text does not exceed the content area
                if (lineY + lineHeight > y + height) break;

                // draw the text line
                graphics.pose().pushPose();
                graphics.pose().translate(lineX, lineY, 0);
                graphics.pose().scale(scale, scale, 1);
                graphics.drawString(font, line, 0, 0, color, dropShadow);
                graphics.pose().popPose();
            }
        });
    }
}
