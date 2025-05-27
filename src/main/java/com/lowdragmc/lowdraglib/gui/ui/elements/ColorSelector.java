package com.lowdragmc.lowdraglib.gui.ui.elements;

import com.lowdragmc.lowdraglib.client.shader.Shaders;
import com.lowdragmc.lowdraglib.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib.core.mixins.accessor.BufferBuilderAccessor;
import com.lowdragmc.lowdraglib.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.lwjgl.system.MemoryUtil;

import java.util.function.IntConsumer;

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
    private enum HSB_MODE {
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
    private float h = 204;
    /**
     * saturation component, must range from 0f to 1f
     */
    private float s = 0.72f;
    /**
     * the brightness component, must range from 0f to 1f
     */
    private float b = 0.94f;
    /**
     * thr alpha used for draw main and slide
     */
    private float alpha = 1;
    /**
     * the rgb transformed from hsb color space
     * [0x00rrggbb]
     */
    private int argb;
    private HSB_MODE mode = HSB_MODE.H;

    public ColorSelector() {
        this.pickerContainer = new UIElement();
        this.colorPreview = new UIElement();
        this.colorSlider = new UIElement();
        this.alphaSlider = new UIElement();
        this.hsbButton = new Button();

        colorSlider.layout(layout -> {
            layout.setWidth(12);
            layout.setPadding(YogaEdge.ALL, 3);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1)).addChildren(
                new UIElement().layout(layout -> layout.setFlex(1))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustColorSlider)
                        .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustColorSlider)
                        .setId("color_slider").style(style -> style.backgroundTexture(this::drawColorSlider)));

        alphaSlider.layout(layout -> {
            layout.setFlexGrow(1);
            layout.setHeight(12);
            layout.setPadding(YogaEdge.ALL, 3);
        }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1)).addChildren(
                new UIElement().layout(layout -> layout.setFlex(1))
                        .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustAlphaSlider)
                        .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustAlphaSlider)
                        .setId("alpha_slider").style(style -> style.backgroundTexture(this::drawAlphaSlider)));

        hsbButton.setOnClick(this::onSwitchHSB).textStyle(textStyle -> textStyle.fontSize(6)).setText("H").layout(layout -> {
            layout.setWidth(12);
            layout.setHeight(12);
        });

        pickerContainer.layout(layout -> {
            layout.setAspectRatio(1);
        }).addChildren(
                new UIElement().layout(layout -> {
                    layout.setFlex(1);
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                }).addChildren(colorPreview.layout(layout -> {
                    layout.setFlex(1);
                    layout.setPadding(YogaEdge.ALL, 4);
                }).style(style -> style.backgroundTexture(Sprites.BORDER1_THICK_RT1)).addChild(
                        new UIElement().layout(layout -> layout.setFlex(1))
                                .addEventListener(UIEvents.MOUSE_DOWN, this::onAdjustHsbContext)
                                .addEventListener(UIEvents.DRAG_SOURCE_UPDATE, this::onAdjustHsbContext)
                                .setId("color_context").style(style -> style.backgroundTexture(this::drawHsbContext))
                ), colorSlider),

                new UIElement().layout(layout -> layout.setFlexDirection(YogaFlexDirection.ROW))
                        .addChildren(alphaSlider, hsbButton));

        this.textContainer = new UIElement().layout(layout -> {
            layout.setMargin(YogaEdge.TOP, 2);
            layout.setGap(YogaGutter.ALL, 1);
        });
        this.textContainer.addChildren(
                new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setGap(YogaGutter.ALL, 2);
                    layout.setAlignItems(YogaAlign.CENTER);
                }).addChildren(
                        new UIElement().layout(layout -> {
                            layout.setWidth(10);
                            layout.setHeight(10);
                        }).style(style -> style.backgroundTexture(this::drawColorPreview)),
                        hexConfigurator = new StringConfigurator("", () -> String.format("#%08x", argb), s -> {
                            try {
                                setValue(Integer.parseUnsignedInt(s.substring(1), 16));
                            } catch (Exception ignored) {}}, "#FFFFFFFF", false),
                        new Button().setOnClick(this::onCopy).textStyle(textStyle -> textStyle.fontSize(6).adaptiveWidth(true))
                                .setText("Copy").layout(layout -> {
                                    layout.setHeight(10);
                                    layout.setPadding(YogaEdge.HORIZONTAL, 2);
                                })),
                new NumberConfigurator("r", () -> ColorUtils.redI(argb), r -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        r.intValue(), ColorUtils.greenI(argb), ColorUtils.blueI(argb))), 255, true).setRange(0, 255),
                new NumberConfigurator("g", () -> ColorUtils.greenI(argb), g -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        ColorUtils.redI(argb), g.intValue(), ColorUtils.blueI(argb))), 255, true).setRange(0, 255),
                new NumberConfigurator("b", () -> ColorUtils.blueI(argb), b -> setColor(ColorUtils.color(ColorUtils.alphaI(argb),
                        ColorUtils.redI(argb), ColorUtils.greenI(argb), b.intValue())), 255, true).setRange(0, 255));

        hexConfigurator.layout(layout -> layout.setFlexGrow(1));

        addChildren(pickerContainer, textContainer);
        refreshRGB();
    }

    protected void onCopy(UIEvent event) {
        Minecraft.getInstance().keyboardHandler.setClipboard(String.format("#%08x", argb));
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
        float normalizedX = (event.x - event.target.getPositionX()) / event.target.getSizeWidth();
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
        float normalizedX = (event.x - event.target.getPositionX()) / event.target.getSizeWidth();
        float normalizedY = (event.y - event.target.getPositionY()) / event.target.getSizeHeight();
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
    public ColorSelector setValue(Integer value, boolean notify) {
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

    protected void drawColorPreview(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        DrawerHelper.drawSolidRect(graphics, (int) x, (int) y, (int) width, (int) height, argb);
    }

    protected void drawHsbContext(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Shaders.HSB_VERTEX_FORMAT);
        RenderSystem.setShader(Shaders::getHsbShader);
        var pose = graphics.pose().last().pose();

        float _h = 0, _s = 0, _b = 0f;
        {
            //left-up corner
            switch (mode) {
                case H -> {
                    _h = h;
                    _s = 0f;
                    _b = 1f;
                }
                case S -> {
                    _h = 0f;
                    _s = s;
                    _b = 1f;
                }
                case B -> {
                    _h = 0f;
                    _s = 1f;
                    _b = b;
                }
            }
            builder.addVertex(pose, x, y, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }

        {
            //left-down corner
            switch (mode) {
                case H -> {
                    _h = h;
                    _s = 0f;
                    _b = 0f;
                }
                case S -> {
                    _h = 0f;
                    _s = s;
                    _b = 0f;
                }
                case B -> {
                    _h = 0f;
                    _s = 0;
                    _b = b;
                }
            }
            builder.addVertex(pose, x, y + height, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }

        {
            //right-down corner
            switch (mode) {
                case H -> {
                    _h = h;
                    _s = 1f;
                    _b = 0f;
                }
                case S -> {
                    _h = 360f;
                    _s = s;
                    _b = 0f;
                }
                case B -> {
                    _h = 360f;
                    _s = 0f;
                    _b = b;
                }
            }
            builder.addVertex(pose, x + width, y + height, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }

        {
            //right-up corner
            switch (mode) {
                case H -> {
                    _h = h;
                    _s = 1f;
                    _b = 1f;
                }
                case S -> {
                    _h = 360f;
                    _s = s;
                    _b = 1f;
                }
                case B -> {
                    _h = 360f;
                    _s = 1f;
                    _b = b;
                }
            }

            builder.addVertex(pose, x + width, y, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());

        // draw indicator
        float mainX = 0, mainY = 0;
        switch (mode) {
            case H -> {
                mainX = s;
                mainY = 1 - b;
            }
            case S -> {
                mainX = h / 360f;
                mainY = 1 - b;
            }
            case B -> {
                mainX = h / 360f;
                mainY = 1- s;
            }
        }

        DrawerHelper.drawSolidRect(graphics, (int) (x + mainX * width) - 1, (int) (y + mainY * height) - 1, 2, 2, b > 0.5f ? 0xff000000 : 0xffffffff);
    }

    protected void drawColorSlider(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, Shaders.HSB_VERTEX_FORMAT);
        RenderSystem.setShader(Shaders::getHsbShader);
        var pose = graphics.pose().last().pose();

        float _h = 0f, _s = 0f, _b = 0f;
        {
            //down two corners
            switch (mode) {
                case H -> {
                    _h = 0f;
                    _s = 1f;
                    _b = 1f;
                }
                case S -> {
                    _h = h;
                    _s = 0f;
                    _b = b;
                }
                case B -> {
                    _h = h;
                    _s = s;
                    _b = 0f;
                }
            }
            builder.addVertex(pose, x, y + height, 0.0f);
            putColor(builder, _h, _s, _b, 1);

            builder.addVertex(pose, x + width, y + height, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }

        {
            //up two corners
            switch (mode) {
                case H -> {
                    _h = 360f;
                    _s = 1f;
                    _b = 1f;
                }
                case S -> {
                    _h = h;
                    _s = 1f;
                    _b = b;
                }
                case B -> {
                    _h = h;
                    _s = s;
                    _b = 1f;
                }
            }
            builder.addVertex(pose, x + width, y, 0.0f);
            putColor(builder, _h, _s, _b, 1);

            builder.addVertex(pose, x, y, 0.0f);
            putColor(builder, _h, _s, _b, 1);
        }
        BufferUploader.drawWithShader(builder.buildOrThrow());

        // draw indicator
        float color = 0;
        switch (mode) {
            case H -> {
                color = (1 - h / 360f);
            }
            case S -> {
                color = (1 - s);
            }
            case B -> {
                color = (1 - b);
            }
        }
        DrawerHelper.drawSolidRect(graphics, (int) (x - 2), (int) (y + color * height), (int) width + 4, 1, 0xffff0000);

    }

    protected void drawAlphaSlider(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        DrawerHelper.drawGradientRect(graphics, x, y, width, height, argb & 0x00ffffff, argb | 0xff000000, true);

        // draw indicator
        DrawerHelper.drawSolidRect(graphics, (int) (x + alpha * width), (int) (y - 2), 1, (int) (height + 4), 0xffff0000);
    }

    /**
     * put hsb color into BufferBuilder
     */
    private BufferBuilder putColor(BufferBuilder builder, float h, float s, float b, float a) {
        if (builder instanceof BufferBuilderAccessor accessor) {
            var i = accessor.invokeBeginElement(Shaders.HSB_Alpha);
            if (i != -1L) {
                MemoryUtil.memPutFloat(i, h);
                MemoryUtil.memPutFloat(i + 4L, s);
                MemoryUtil.memPutFloat(i + 8L, b);
                MemoryUtil.memPutFloat(i + 12L, a);
            }
        }
        return builder;
    }

}
