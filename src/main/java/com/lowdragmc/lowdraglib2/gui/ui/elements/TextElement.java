package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
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
import java.util.Map;
import java.util.function.Consumer;

@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TextElement extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class TextStyle extends Style {
        @Getter @Setter
        private boolean adaptiveWidth = false;
        @Getter @Setter
        private boolean adaptiveHeight = false;
        @Getter @Setter
        private Horizontal textAlignHorizontal = Horizontal.LEFT;
        @Getter @Setter
        private Vertical textAlignVertical = Vertical.TOP;
        @Getter @Setter
        private TextWrap textWrap = TextWrap.NONE;
        @Getter @Setter
        private float rollSpeed = 1;
        @Getter @Setter
        private float fontSize = 9;
        @Getter @Setter
        private float lineSpacing = 1;
        @Getter @Setter
        private int textColor = -1;
        @Getter @Setter
        private boolean textShadow = true;

        public TextStyle(UIElement holder) {
            super(holder);
        }
    }
    @Getter
    private Component text = Component.empty();

    @Getter
    private final TextStyle textStyle = new TextStyle(this);

    /**
     * The formatted text to be displayed in each line and its width.
     */
    private List<Tuple<FormattedCharSequence, Float>> formattedLines = Collections.emptyList();

    public void recompute() {
        var maxWidth = 0f;
        var wrap = getTextStyle().textWrap();
        if (getTextStyle().adaptiveWidth() || wrap == TextWrap.NONE || wrap == TextWrap.ROLL || wrap == TextWrap.HOVER_ROLL) {
            maxWidth = Float.MAX_VALUE;
        } else {
            maxWidth = getContentWidth();
        }
        formattedLines = TextUtilities.computeFormattedLines(
                getFont(),
                text,
                getTextStyle().fontSize(),
                maxWidth
        );
        if (getTextStyle().adaptiveWidth()) {
            layout(layout -> layout.setWidth(formattedLines.stream().findFirst().map(Tuple::getB).orElse(0f) + getSizeWidth() - getContentWidth()));
        }
        if (getTextStyle().adaptiveHeight()) {
            layout(layout -> layout.setHeight(formattedLines.size() * (getTextStyle().fontSize() + getTextStyle().lineSpacing()) - getTextStyle().lineSpacing() + getSizeHeight() - getContentHeight()));
        }
    }

    public TextElement textStyle(Consumer<TextStyle> style) {
        style.accept(textStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textStyle.applyStyles(values);
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

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (formattedLines.isEmpty()) return;
        graphics.drawManaged(() -> {
            var font = getFont();
            var defaultLineHeight = font.lineHeight;
            var x = getContentX();
            var y = getContentY();
            var width = getContentWidth();
            var height = getContentHeight();
            var hAlign = getTextStyle().textAlignHorizontal();
            var vAlign = getTextStyle().textAlignVertical();
            var lineHeight = getTextStyle().fontSize();
            var lineSpacing = getTextStyle().lineSpacing();
            var color = getTextStyle().textColor();
            var dropShadow = getTextStyle().textShadow();
            var scale = lineHeight / defaultLineHeight;


            // calculate the total height of the text
            var displayLines = formattedLines;
            var textWrap = getTextStyle().textWrap();
            if (textWrap == TextWrap.HIDE) {
                // display the first line only
                displayLines = formattedLines.subList(0, Math.min(1, formattedLines.size()));
            }

            var totalTextHeight = displayLines.size() * (lineHeight + lineSpacing) - lineSpacing;
            var startY = y;

            // according to the vertical alignment, adjust the starting Y coordinate
            switch (vAlign) {
                case TOP -> startY = y;
                case CENTER -> startY = y + (height - totalTextHeight) / 2;
                case BOTTOM -> startY = y + (height - totalTextHeight);
            }

            // render each line of text
            var roll = textWrap == TextWrap.ROLL || (textWrap == TextWrap.HOVER_ROLL && isChildHover());
            for (int i = 0; i < displayLines.size(); i++) {
                var tuple = displayLines.get(i);
                var line = tuple.getA();
                float lineWidth = tuple.getB();
                var lineX = x;

                // according to the horizontal alignment, adjust the starting X coordinate
                if (roll && lineWidth > width) {
                    // for rolling text, always align to the left
                    var rollSpeed = getTextStyle().rollSpeed();
                    float totalW = width + lineWidth + 10;
                    var t = rollSpeed > 0 ? ((((rollSpeed * Math.abs((int)(System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / (totalW)) : 0.5;
                    lineX = (float) (x + width - totalW * t);
                } else {
                    switch (hAlign) {
                        case LEFT -> lineX = x;
                        case CENTER -> lineX = (lineWidth > width) ? x : (x + (width - lineWidth) / 2);
                        case RIGHT -> lineX = x + (width - lineWidth);
                    }
                }

                // calculate the Y coordinate of the current line (including line spacing)
                var lineY = startY + i * (lineHeight + lineSpacing);

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
