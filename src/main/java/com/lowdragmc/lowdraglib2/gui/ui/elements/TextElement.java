package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@RemapPrefixForJS("kjs$")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "text", group = "basic", registry = "ldlib2:ui_element")
public class TextElement extends UIElement {
    @Configurable(name = "TextStyle")
    public class TextStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.ADAPTIVE_HEIGHT,
                PropertyRegistry.ADAPTIVE_WIDTH,
                PropertyRegistry.VERTICAL_ALIGN,
                PropertyRegistry.HORIZONTAL_ALIGN,
                PropertyRegistry.TEXT_WRAP,
                PropertyRegistry.ROLL_SPEED,
                PropertyRegistry.FONT,
                PropertyRegistry.FONT_SIZE,
                PropertyRegistry.TEXT_COLOR,
                PropertyRegistry.TEXT_SHADOW,
                PropertyRegistry.LINE_SPACING,
        };

        public TextStyle() {
            super(TextElement.this);
        }

        public static void init() {
            PropertyRegistry.ADAPTIVE_WIDTH.addListener(TextStyle::onPropertyChanged);
            PropertyRegistry.ADAPTIVE_HEIGHT.addListener(TextStyle::onPropertyChanged);
            PropertyRegistry.TEXT_WRAP.addListener(TextStyle::onPropertyChanged);
            PropertyRegistry.FONT_SIZE.addListener(TextStyle::onPropertyChanged);
            PropertyRegistry.FONT.addListener(TextStyle::onPropertyChanged);
            PropertyRegistry.LINE_SPACING.addListener(TextStyle::onPropertyChanged);
        }

        private static <T> void onPropertyChanged(UIElement element, Property<T> property, @Nullable T oldValue, @Nullable T newValue) {
            if (element instanceof TextElement textElement) {
                textElement.onTextStyleChanged();
            }
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public boolean adaptiveHeight() {
            return getValueSave(PropertyRegistry.ADAPTIVE_HEIGHT);
        }

        public TextStyle adaptiveHeight(boolean adaptiveHeight) {
            set(PropertyRegistry.ADAPTIVE_HEIGHT, adaptiveHeight);
            return this;
        }

        public boolean adaptiveWidth() {
            return getValueSave(PropertyRegistry.ADAPTIVE_WIDTH);
        }

        public TextStyle adaptiveWidth(boolean adaptiveWidth) {
            set(PropertyRegistry.ADAPTIVE_WIDTH, adaptiveWidth);
            return this;
        }

        public TextWrap textWrap() {
            return getValueSave(PropertyRegistry.TEXT_WRAP);
        }

        public TextStyle textWrap(TextWrap textWrap) {
            set(PropertyRegistry.TEXT_WRAP, textWrap);
            return this;
        }

        public float rollSpeed() {
            return getValueSave(PropertyRegistry.ROLL_SPEED);
        }

        public TextStyle rollSpeed(float rollSpeed) {
            set(PropertyRegistry.ROLL_SPEED, rollSpeed);
            return this;
        }

        public float lineSpacing() {
            return getValueSave(PropertyRegistry.LINE_SPACING);
        }

        public TextStyle lineSpacing(float lineSpacing) {
            set(PropertyRegistry.LINE_SPACING, lineSpacing);
            return this;
        }

        public int textColor() {
            return getValueSave(PropertyRegistry.TEXT_COLOR);
        }

        public TextStyle textColor(int textColor) {
            set(PropertyRegistry.TEXT_COLOR, textColor);
            return this;
        }

        public boolean textShadow() {
            return getValueSave(PropertyRegistry.TEXT_SHADOW);
        }

        public TextStyle textShadow(boolean textShadow) {
            set(PropertyRegistry.TEXT_SHADOW, textShadow);
            return this;
        }

        public ResourceLocation font() {
            return getValueSave(PropertyRegistry.FONT);
        }

        public TextStyle font(ResourceLocation font) {
            set(PropertyRegistry.FONT, font);
            return this;
        }

        public float fontSize() {
            return getValueSave(PropertyRegistry.FONT_SIZE);
        }

        public TextStyle fontSize(float fontSize) {
            set(PropertyRegistry.FONT_SIZE, fontSize);
            return this;
        }

        public Horizontal textAlignHorizontal() {
            return getValueSave(PropertyRegistry.HORIZONTAL_ALIGN);
        }

        public TextStyle textAlignHorizontal(Horizontal textAlignHorizontal) {
            set(PropertyRegistry.HORIZONTAL_ALIGN, textAlignHorizontal);
            return this;
        }

        public Vertical textAlignVertical() {
            return getValueSave(PropertyRegistry.VERTICAL_ALIGN);
        }

        public TextStyle textAlignVertical(Vertical textAlignVertical) {
            set(PropertyRegistry.VERTICAL_ALIGN, textAlignVertical);
            return this;
        }

    }

    @Getter
    private final TextStyle textStyle = new TextStyle();

    @Getter
    @Configurable(name = "value")
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
            Style.importantPipeline(getLayout(), layout -> layout.width(formattedLines.stream().findFirst().map(Tuple::getB).orElse(0f) + getSizeWidth() - getContentWidth()));
        } else {
            getStyleBag().removeCandidates(LayoutProperties.WIDTH, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
        if (getTextStyle().adaptiveHeight()) {
            Style.importantPipeline(getLayout(), layout -> layout.height(formattedLines.size() * (getTextStyle().fontSize() + getTextStyle().lineSpacing()) - getTextStyle().lineSpacing() + getSizeHeight() - getContentHeight()));
        } else {
            getStyleBag().removeCandidates(LayoutProperties.HEIGHT, slot -> slot.origin() == StyleOrigin.IMPORTANT);
        }
    }

    public TextElement textStyle(Consumer<TextStyle> style) {
        style.accept(textStyle);
        return this;
    }

    protected void onTextStyleChanged() {
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
        RenderSystem.depthMask(false);
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
            var roll = textWrap == TextWrap.ROLL || (textWrap == TextWrap.HOVER_ROLL && isSelfOrChildHover());
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
        RenderSystem.depthMask(true);
    }

    @Override
    public void loadXml(Element element) {
        super.loadXml(element);
        XmlUtils.getComponents(element, net.minecraft.network.chat.Style.EMPTY)
                .stream()
                .reduce(MutableComponent::append)
                .ifPresent(this::setText);
    }

    @Override
    protected void parseXmlChildElement(Element childElement) {
        // not able to add children for text
    }
}
