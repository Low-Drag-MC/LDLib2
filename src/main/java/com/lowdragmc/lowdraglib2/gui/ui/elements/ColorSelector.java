package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.editor.ClipboardManager;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.IntConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "color-selector", group = "misc", registry = "ldlib2:ui_element")
public class ColorSelector extends BindableUIElement<Integer> {
    public final UIElement pickerContainer;
    public final UIElement colorPreview;
    public final UIElement colorSlider;
    public final UIElement alphaSlider;
    public final Button hsbButton;
    public final UIElement textContainer;
    public final StringConfigurator hexConfigurator;

    /**
     * all supported pick mode
     */
    enum HSB_MODE {
        H("hue"), S("saturation"), B("brightness");
        private final String name;

        HSB_MODE(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * hue component, must range from 0f to 360f
     */
    float h = 204;
    /**
     * saturation component, must range from 0f to 1f
     */
    float s = 0.72f;
    /**
     * the brightness component, must range from 0f to 1f
     */
    float b = 0.94f;
    /**
     * thr alpha used for draw main and slide
     */
    float alpha = 1;
    /**
     * the rgb transformed from hsb color space
     * [0x00rrggbb]
     */
    int argb;
    HSB_MODE mode = HSB_MODE.H;

    public ColorSelector() {
        this.pickerContainer = new UIElement().addClass("__color-selector_picker-container__");
        this.colorPreview = new UIElement().addClass("__color-selector_color-preview__");
        this.colorSlider = new UIElement().addClass("__color-selector_color-slider__");
        this.alphaSlider = new UIElement().addClass("__color-selector_alpha-slider__");
        this.hsbButton = new Button();
        this.hsbButton.addClass("__color-selector_hsb-button__");

        colorSlider.layout(layout -> {
            layout.width(12);
            layout.paddingAll(3);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1))
                .addChildren(new UIElement().layout(layout -> layout.flex(1))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustColorSlider)
                        .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustColorSlider)
                        .addClass("__color-selector_color-slider_bar__").style(style -> style.backgroundTexture(ColorSelectorTextures.colorSlider(this))));

        alphaSlider.layout(layout -> {
            layout.setFlexGrow(1);
            layout.height(12);
            layout.paddingAll(3);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1)).addChildren(
                new UIElement().layout(layout -> layout.flex(1))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustAlphaSlider)
                        .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustAlphaSlider)
                        .addClass("__color-selector_alpha-slider_bar__").style(style -> style.backgroundTexture(ColorSelectorTextures.alphaSlider(this))));

        hsbButton.setOnClick(this::onSwitchHSB).textStyle(textStyle -> textStyle.fontSize(6)).setText("H").layout(layout -> {
            layout.width(12);
            layout.height(12);
        });

        pickerContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.setAspectRatio(1);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.flexDirection(FlexDirection.ROW);
                }).addChildren(colorPreview.layout(layout -> {
                    layout.flex(1);
                    layout.paddingAll(4);
                }).style(style -> style.backgroundTexture(Sprites.BORDER1_THICK_RT1)).addChild(
                        new UIElement().layout(layout -> layout.flex(1))
                                .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustHsbContext)
                                .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustHsbContext)
                                .addClass("__color-selector_color-preview_display__").style(style -> style.backgroundTexture(ColorSelectorTextures.hsbContext(this)))
                ), colorSlider),

                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW))
                        .addChildren(alphaSlider, hsbButton));

        this.textContainer = new UIElement().layout(layout -> {
            layout.marginTop(2);
            layout.gapAll(1);
        }).addClass("__color-selector_text-container__");
        this.textContainer.addChildren(
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(2);
                    layout.alignItems(AlignItems.CENTER);
                }).addChildren(
                        new UIElement().layout(layout -> {
                            layout.width(10);
                            layout.height(10);
                        }).style(style -> style.backgroundTexture(ColorSelectorTextures.colorPreview(this))),
                        hexConfigurator = new StringConfigurator("", () -> String.format("#%08x", argb), s -> {
                            try {
                                setValue(Integer.parseUnsignedInt(s.substring(1), 16));
                            } catch (Exception ignored) {}}, "#FFFFFFFF", false),
                        new Button().setOnClick(this::onCopy).textStyle(textStyle -> textStyle.fontSize(6).adaptiveWidth(true))
                                .setText("Copy").layout(layout -> {
                                    layout.height(10);
                                    layout.paddingHorizontal(2);
                                })),
                new NumberConfigurator("r", () -> ColorUtils.redI(argb), r -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        r.intValue(), ColorUtils.greenI(argb), ColorUtils.blueI(argb))), 255, true).setRange(0, 255)
                        .addClass("__color-selector_red-configurator__"),
                new NumberConfigurator("g", () -> ColorUtils.greenI(argb), g -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        ColorUtils.redI(argb), g.intValue(), ColorUtils.blueI(argb))), 255, true).setRange(0, 255)
                        .addClass("__color-selector_green-configurator__"),
                new NumberConfigurator("b", () -> ColorUtils.blueI(argb), b -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        ColorUtils.redI(argb), ColorUtils.greenI(argb), b.intValue())), 255, true).setRange(0, 255)
                        .addClass("__color-selector_blue-configurator__"));

        hexConfigurator.layout(layout -> layout.setFlexGrow(1));
        hexConfigurator.addClass("__color-selector_hex-configurator__");

        addChildren(pickerContainer, textContainer);
        refreshRGB();
        internalSetup();
    }

    protected void onCopy(UIEvent event) {
        ClipboardManager.INSTANCE.copyDirect(String.format("#%08x", argb));
    }

    private void refreshRGB() {
        argb = ColorUtils.HSBtoRGB(h / 360f, s, b, alpha);
        hexConfigurator.textField.setText(String.format("#%08x", argb), false);
    }

    protected void onAdjustColorSlider(UIEvent event) {
        float normalizedY = (event.y - event.target.getPositionY()) / event.target.getSizeHeight();
        if (normalizedY < 0) normalizedY = 0;
        if (normalizedY > 1) normalizedY = 1;
        switch (mode) {
            case H -> h = 360f - normalizedY * 360f;
            case S -> s = 1f - normalizedY;
            case B -> b = 1f - normalizedY;
        }
        refreshRGB();
        if (event.type.equals(UIEvents.MOUSE_DOWN)) {
            event.target.startDrag(null, null);
        }
        notifyListeners();
    }

    protected void onAdjustAlphaSlider(UIEvent event) {
        var localMouse = getLocalMouse(event.x, event.y);
        float normalizedX = (localMouse.x - event.target.getPositionX()) / event.target.getSizeWidth();
        if (normalizedX < 0) normalizedX = 0;
        if (normalizedX > 1) normalizedX = 1;
        this.alpha = normalizedX;
        refreshRGB();
        if (event.type.equals(UIEvents.MOUSE_DOWN)) {
            event.target.startDrag(null, null);
        }
        notifyListeners();
    }

    private void onAdjustHsbContext(UIEvent event) {
        var localMouse = getLocalMouse(event.x, event.y);
        float normalizedX = (localMouse.x - event.target.getPositionX()) / event.target.getSizeWidth();
        float normalizedY = (localMouse.y - event.target.getPositionY()) / event.target.getSizeHeight();
        if (normalizedX < 0) normalizedX = 0;
        if (normalizedX > 1) normalizedX = 1;
        if (normalizedY < 0) normalizedY = 0;
        if (normalizedY > 1) normalizedY = 1;
        switch (mode) {
            case H -> {
                s = normalizedX;
                b = 1.0f - normalizedY;
            }
            case S -> {
                h = normalizedX * 360f;
                b = 1.0f - normalizedY;
            }
            case B -> {
                h = normalizedX * 360f;
                s = 1.0f - normalizedY;
            }
        }
        refreshRGB();
        if (event.type.equals(UIEvents.MOUSE_DOWN)) {
            event.target.startDrag(null, null);
        }
        notifyListeners();
    }

    public ColorSelector setColor(int argb, boolean notify) {
        return setValue(argb, notify);
    }

    public ColorSelector setColor(int argb) {
        return setColor(argb, true);
    }

    public int getColor() {
        return argb;
    }

    /// Data bindings
    @Override
    public Integer getValue() {
        return argb;
    }

    @Override
    public ColorSelector setValue(@Nullable Integer value, boolean notify) {
        if (value == null) value = -1;
        if (this.argb == value) return this;
        this.alpha = ColorUtils.alpha(value);
        var hsb = ColorUtils.RGBtoHSB(value);
        hsb[0] *= 360f;
        this.h = hsb[0];
        this.s = hsb[1];
        this.b = hsb[2];
        refreshRGB();
        if (notify) {
            notifyListeners();
        }
        return this;
    }

    public ColorSelector setOnColorChangeListener(IntConsumer listener) {
        registerValueListener(listener::accept);
        return this;
    }

    protected void onSwitchHSB(UIEvent event) {
        mode = switch (mode) {
            case H -> HSB_MODE.S;
            case S -> HSB_MODE.B;
            case B -> HSB_MODE.H;
        };
        hsbButton.setText(mode.name());
    }
}
