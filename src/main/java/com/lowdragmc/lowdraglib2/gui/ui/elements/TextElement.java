package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.configurator.annotation.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import lombok.Getter;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Tuple;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

//@RemapPrefixForJS("kjs$")
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

        public Identifier font() {
            return getValueSave(PropertyRegistry.FONT);
        }

        public TextStyle font(Identifier font) {
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
        if (!LDLib2.isRemote()) return;
        TextElementRenderer.recompute(this);
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

//    @HideFromJS
    @ConfigSetter(field = "text")
    public TextElement setText(Component text) {
        if (this.text.equals(text)) return this;
        this.text = text;
        recompute();
        return this;
    }

//    @HideFromJS
    public TextElement setText(String text) {
        return setText(text,true);
    }

    public TextElement setText(String text, boolean translate) {
        return setText(translate ? Component.translatable(text) : Component.literal(text));
    }

//    public TextElement kjs$setText(Component text) {
//        return setText(text);
//    }

    List<Tuple<FormattedCharSequence, Float>> getFormattedLines() {
        return formattedLines;
    }

    void setFormattedLines(List<Tuple<FormattedCharSequence, Float>> formattedLines) {
        this.formattedLines = formattedLines;
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

    @LDLRegisterClient(name = "text_element", registry = "ldlib2:ui_element_renderer")
    public static final class TextElementRenderer extends DelegatingUIElementRenderer<TextElement, TextElementRenderer> {
        @Override
        public Class<TextElement> type() {
            return TextElement.class;
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
                    LDFonts.font(),
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

        @Override
        public void drawBackgroundAdditional(TextElement textElement, IGUIContext context) {
            if (!(context instanceof GUIContext guiContext)) {
                drawParentBackgroundAdditional(textElement, context);
                return;
            }
            drawBackgroundAdditional(textElement, guiContext);
        }

        static void drawBackgroundAdditional(TextElement textElement, GUIContext context) {
            var formattedLines = textElement.getFormattedLines();
            if (formattedLines.isEmpty()) return;
            var font = LDFonts.font();
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

            List<Tuple<FormattedCharSequence, Float>> displayLines = formattedLines;
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
                LDFonts.drawText(context, font, line, 0, 0, color, dropShadow);
                context.pose.popPose();
            }
        }
    }
}
