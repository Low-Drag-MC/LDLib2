package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.*;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@LDLRegister(name = "text_element", registry = "ldlib2:ui_element")
public class TextElement extends UIElement {
    @Accessors(chain = true, fluent = true)
    public static class TextStyle extends Style {
        @Getter @Setter
        @Configurable(name = "adaptiveWidth", tips = "adaptiveWidth.tips")
        private boolean adaptiveWidth = false;
        @Getter @Setter
        @Configurable(name = "adaptiveHeight", tips = "adaptiveHeight.tips")
        private boolean adaptiveHeight = false;
        @Getter @Setter
        @Configurable(name = "textAlignHorizontal")
        private Horizontal textAlignHorizontal = Horizontal.LEFT;
        @Getter @Setter
        @Configurable(name = "textAlignVertical")
        private Vertical textAlignVertical = Vertical.TOP;
        @Getter @Setter
        @Configurable(name = "textWrap", tips = {"textWrap.tips.NONE", "textWrap.tips.WRAP", "textWrap.tips.HOVER_ROLL", "textWrap.tips.HIDE"})
        private TextWrap textWrap = TextWrap.NONE;
        @Getter @Setter
        @Configurable(name = "rollSpeed")
        @ConfigNumber(range = {0f, Float.MAX_VALUE})
        private float rollSpeed = 1;
        @Getter @Setter
        @Configurable(name = "fontSize")
        @ConfigNumber(range = {0f, Float.MAX_VALUE})
        private float fontSize = 9;
        @Getter @Setter
        @Configurable(name = "font")
        @ConfigFont
        private ResourceLocation font = net.minecraft.network.chat.Style.DEFAULT_FONT;
        @Getter @Setter
        @Configurable(name = "lineSpacing")
        @ConfigNumber(range = {0f, Float.MAX_VALUE})
        private float lineSpacing = 1;
        @Getter @Setter
        @Configurable(name = "textColor")
        @ConfigColor
        private int textColor = -1;
        @Getter @Setter
        @Configurable(name = "textShadow")
        private boolean textShadow = true;

        public TextStyle(UIElement holder) {
            super(holder);
        }

        @Override
        @OnlyIn(Dist.CLIENT)
        public void buildConfigurator(ConfiguratorGroup father) {
            super.buildConfigurator(father);
            if (holder instanceof TextElement textElement) {
                father.addEventListener(Configurator.CHANGE_EVENT, event -> textElement.recompute());
            }
        }
    }

    @Getter
    @Configurable(name = "textStyle", subConfigurable = true)
    private final TextStyle textStyle = new TextStyle(this);

    @Getter
    @Configurable(name = "text")
    private Component text = Component.empty();

    /**
     * The formatted text to be displayed in each line and its width.
     */
    private List<Tuple<FormattedCharSequence, Float>> formattedLines = Collections.emptyList();

    public void recompute() {
        if (!LDLib2.isClient()) return;
        var maxWidth = 0f;
        var wrap = getTextStyle().textWrap();
        var font = getTextStyle().font();
        if (getTextStyle().adaptiveWidth() || wrap == TextWrap.NONE || wrap == TextWrap.ROLL || wrap == TextWrap.HOVER_ROLL) {
            maxWidth = Float.MAX_VALUE;
        } else {
            maxWidth = getContentWidth();
        }
        formattedLines = TextUtilities.computeFormattedLines(
                getFont(),
                TextUtilities.withFont(text, font),
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
        recompute();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        textStyle.applyStyles(values);
        recompute();
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        recompute();
    }

    @HideFromJS
    @ConfigSetter(field = "text")
    public TextElement setText(Component text) {
        if (this.text.equals(text)) return this;
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

    @OnlyIn(Dist.CLIENT)
    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        if (formattedLines.isEmpty()) return;
        guiContext.graphics.drawManaged(() -> {
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
                guiContext.pose.pushPose();
                guiContext.pose.translate(lineX, lineY, 0);
                guiContext.pose.scale(scale, scale, 1);
                guiContext.graphics.drawString(font, line, 0, 0, color, dropShadow);
                guiContext.pose.popPose();
            }
        });
    }

}
