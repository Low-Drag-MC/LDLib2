package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.annotation.*;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.joml.Vector4f;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@LDLRegisterClient(name = "text_texture", registry = "ldlib:gui_texture")
public class TextTexture extends TransformTexture {

    @Configurable
    public String text;

    @Configurable
    @NumberColor
    public int color;

    @Configurable
    @NumberColor
    public int backgroundColor;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_width")
    @NumberRange(range = {1, Integer.MAX_VALUE})
    public int width;
    @Configurable
    @NumberRange(range = {0, Integer.MAX_VALUE})
    @Setter
    public float rollSpeed = 1;
    @Configurable
    public boolean dropShadow;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_type")
    public TextType type;

    public Supplier<String> supplier;
    @OnlyIn(Dist.CLIENT)
    private List<String> texts;

    private long lastTick;

    public TextTexture() {
        this("A", -1);
        setWidth(50);
    }

    public TextTexture(String text, int color) {
        this.color = color;
        this.type = TextType.NORMAL;
        if (LDLib.isClient()) {
            this.text = LocalizationUtils.format(text);
            texts = Collections.singletonList(this.text);
        }
    }

    public TextTexture(String text) {
        this(text, -1);
        setDropShadow(true);
    }

    public TextTexture(Supplier<String> text) {
        this("", -1);
        setSupplier(text);
        setDropShadow(true);
    }

    public TextTexture setSupplier(Supplier<String> supplier) {
        this.supplier = supplier;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        if (Minecraft.getInstance().level != null) {
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == lastTick) return;
            lastTick = tick;
        }
        if (supplier != null) {
            updateText(supplier.get());
        }
    }

    @ConfigSetter(field = "text")
    public void updateText(String text) {
        if (LDLib.isClient()) {
            this.text = LocalizationUtils.format(text);
            texts = Collections.singletonList(this.text);
            setWidth(this.width);
        }
    }

    public TextTexture setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public TextTexture setColor(int color) {
        this.color = color;
        return this;
    }

    public TextTexture setDropShadow(boolean dropShadow) {
        this.dropShadow = dropShadow;
        return this;
    }

    public TextTexture setWidth(int width) {
        this.width = width;
        if (LDLib.isClient()) {
            if (this.width > 0) {
                texts = Minecraft.getInstance()
                        .font.getSplitter()
                        .splitLines(text, width, Style.EMPTY)
                        .stream().map(FormattedText::getString)
                        .collect(Collectors.toList());
                if (texts.isEmpty()) {
                    texts = Collections.singletonList(text);
                }
            } else {
                texts = Collections.singletonList(text);
            }
        }
        return this;
    }

    public TextTexture setType(TextType type) {
        this.type = type;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        updateTick();
        if (backgroundColor != 0) {
            DrawerHelper.drawSolidRect(graphics, (int) x, (int) y, (int) width, (int) height, backgroundColor);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        Font fontRenderer = Minecraft.getInstance().font;
        int textH = fontRenderer.lineHeight;
        if (type == TextType.NORMAL) {
            textH *= texts.size();
            for (int i = 0; i < texts.size(); i++) {
                String line = texts.get(i);
                int lineWidth = fontRenderer.width(line);
                float _x = x + (width - lineWidth) / 2f;
                float _y = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                graphics.drawString(fontRenderer, line, (int) _x, (int) _y, color, dropShadow);
            }
        } else if (type == TextType.LEFT) {
            textH *= texts.size();
            for (int i = 0; i < texts.size(); i++) {
                String line = texts.get(i);
                float _y = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                graphics.drawString(fontRenderer, line, (int) x, (int) _y, color, dropShadow);
            }
        } else if (type == TextType.RIGHT) {
            textH *= texts.size();
            for (int i = 0; i < texts.size(); i++) {
                String line = texts.get(i);
                int lineWidth = fontRenderer.width(line);
                float _y = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                graphics.drawString(fontRenderer, line, (int) (x + width - lineWidth), (int) _y, color, dropShadow);
            }
        } else if (type == TextType.HIDE) {
            if (Widget.isMouseOver((int) x, (int) y, (int) width, (int) height, mouseX, mouseY) && texts.size() > 1) {
                drawRollTextLine(graphics, x, y, width, height, fontRenderer, textH, text);
            } else {
                String line = texts.get(0) + (texts.size() > 1 ? ".." : "");
                drawTextLine(graphics, x, y, width, height, fontRenderer, textH, line);
            }
        } else if (type == TextType.ROLL || type == TextType.ROLL_ALWAYS) {
            if (texts.size() > 1 && (type == TextType.ROLL_ALWAYS || Widget.isMouseOver((int) x, (int) y, (int) width, (int) height, mouseX, mouseY))) {
                drawRollTextLine(graphics, x, y, width, height, fontRenderer, textH, text);
            } else {
                drawTextLine(graphics, x, y, width, height, fontRenderer, textH, texts.get(0));
            }
        } else if (type == TextType.LEFT_HIDE) {
            if (Widget.isMouseOver((int) x, (int) y, (int) width, (int) height, mouseX, mouseY) && texts.size() > 1) {
                drawRollTextLine(graphics, x, y, width, height, fontRenderer, textH, text);
            } else {
                String line = texts.get(0) + (texts.size() > 1 ? ".." : "");
                float _y = y + (height - textH) / 2f;
                graphics.drawString(fontRenderer, line, (int) x, (int) _y, color, dropShadow);
            }
        } else if (type == TextType.LEFT_ROLL || type == TextType.LEFT_ROLL_ALWAYS) {
            if (texts.size() > 1 && (type == TextType.LEFT_ROLL_ALWAYS || Widget.isMouseOver((int) x, (int) y, (int) width, (int) height, mouseX, mouseY))) {
                drawRollTextLine(graphics, x, y, width, height, fontRenderer, textH, text);
            } else {
                float _y = y + (height - textH) / 2f;
                graphics.drawString(fontRenderer, texts.get(0), (int) x, (int) _y, color, dropShadow);
            }
        }
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawRollTextLine(GuiGraphics graphics, float x, float y, float width, float height, Font fontRenderer, int textH, String line) {
        float _y = y + (height - textH) / 2f;
        float textW = fontRenderer.width(line);
        float totalW = width + textW + 10;
        float from = x + width;
        var trans = graphics.pose().last().pose();
        var realPos = trans.transform(new Vector4f(x, y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        var t = rollSpeed > 0 ? ((((rollSpeed * Math.abs((int)(System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / (totalW)) : 0.5;
        graphics.drawString(fontRenderer, line, (int) (from - t * totalW), (int) _y, color, dropShadow);
        graphics.disableScissor();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawTextLine(GuiGraphics graphics, float x, float y, float width, float height, Font fontRenderer, int textH, String line) {
        int textW = fontRenderer.width(line);
        float _x = x + (width - textW) / 2f;
        float _y = y + (height - textH) / 2f;
        graphics.drawString(fontRenderer, line, (int) _x, (int) _y, color, dropShadow);
    }

    @OnlyIn(Dist.CLIENT)
    public int getLines() {
        return texts.size();
    }

    public enum TextType{
        NORMAL,
        HIDE,
        ROLL,
        ROLL_ALWAYS,
        LEFT,
        RIGHT,
        LEFT_HIDE,
        LEFT_ROLL,
        LEFT_ROLL_ALWAYS
    }
}
