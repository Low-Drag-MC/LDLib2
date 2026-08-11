package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.font.LDFonts;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@KJSBindings
@LDLRegisterClient(name = "text_texture", registry = "ldlib2:gui_texture")
public class TextTexture extends TransformTexture {
    @Configurable
    public String text;

    @Configurable
    @ConfigColor
    public int color;

    @Configurable
    @ConfigColor
    public int backgroundColor;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_width")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    public int width;

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public float rollSpeed = 1;

    @Configurable
    public boolean dropShadow;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_type")
    public TextType type;

    public Supplier<String> supplier;
    List<String> texts;
    long lastTick;

    public TextTexture() {
        this("A", -1);
        setWidth(50);
    }

    public TextTexture(String text, int color) {
        this.color = color;
        this.type = TextType.NORMAL;
        this.text = LocalizationUtils.format(text);
        this.texts = Collections.singletonList(this.text);
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
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

    @ConfigSetter(field = "text")
    public void updateText(String text) {
        this.text = LocalizationUtils.format(text);
        this.texts = Collections.singletonList(this.text);
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
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
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
        }
        return this;
    }

    public TextTexture setType(TextType type) {
        this.type = type;
        return this;
    }

    @Override
    public TextTexture copy() {
        var copied = new TextTexture(text, color);
        copied.type = type;
        copied.dropShadow = dropShadow;
        copied.rollSpeed = rollSpeed;
        copied.width = width;
        copied.backgroundColor = backgroundColor;
        copied.supplier = supplier;
        copied.texts = List.copyOf(texts);
        copied.copyTransform(this);
        return copied;
    }

    public int getLines() {
        return texts.size();
    }

    public enum TextType {
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

    public static final class TextTextureClientSupport {
        private TextTextureClientSupport() {
        }

        public static void updateTick(TextTexture texture) {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == texture.lastTick) {
                return;
            }
            texture.lastTick = tick;
            if (texture.supplier != null) {
                texture.updateText(texture.supplier.get());
            }
        }

        public static void refreshTexts(TextTexture texture) {
            if (texture.width > 0) {
                texture.texts = LDFonts.font().getSplitter()
                        .splitLines(texture.text, texture.width, Style.EMPTY)
                        .stream()
                        .map(FormattedText::getString)
                        .collect(Collectors.toList());
                if (texture.texts.isEmpty()) {
                    texture.texts = Collections.singletonList(texture.text);
                }
            } else {
                texture.texts = Collections.singletonList(texture.text);
            }
        }

        public static void draw(TextTexture texture, GUIContext context, float x, float y, float width, float height) {
            updateTick(texture);
            if (texture.backgroundColor != 0) {
                DrawerHelperClient.drawSolidRect(context, (int) x, (int) y, (int) width, (int) height, texture.backgroundColor);
            }
            Font fontRenderer = LDFonts.font();
            int textH = fontRenderer.lineHeight;
            if (texture.type == TextType.NORMAL) {
                textH *= texture.texts.size();
                for (int i = 0; i < texture.texts.size(); i++) {
                    String line = texture.texts.get(i);
                    int lineWidth = fontRenderer.width(line);
                    float drawX = x + (width - lineWidth) / 2f;
                    float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                    LDFonts.drawText(context, fontRenderer, line, (int) drawX, (int) drawY, texture.color, texture.dropShadow);
                }
                return;
            }
            if (texture.type == TextType.LEFT) {
                textH *= texture.texts.size();
                for (int i = 0; i < texture.texts.size(); i++) {
                    String line = texture.texts.get(i);
                    float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                    LDFonts.drawText(context, fontRenderer, line, (int) x, (int) drawY, texture.color, texture.dropShadow);
                }
                return;
            }
            if (texture.type == TextType.RIGHT) {
                textH *= texture.texts.size();
                for (int i = 0; i < texture.texts.size(); i++) {
                    String line = texture.texts.get(i);
                    int lineWidth = fontRenderer.width(line);
                    float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                    LDFonts.drawText(context, fontRenderer, line, (int) (x + width - lineWidth), (int) drawY, texture.color, texture.dropShadow);
                }
                return;
            }
            if (texture.type == TextType.HIDE) {
                if (UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY) && texture.texts.size() > 1) {
                    drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
                } else {
                    String line = texture.texts.getFirst() + (texture.texts.size() > 1 ? ".." : "");
                    drawTextLine(texture, context, x, y, width, height, fontRenderer, textH, line);
                }
                return;
            }
            if (texture.type == TextType.ROLL || texture.type == TextType.ROLL_ALWAYS) {
                if (texture.texts.size() > 1 && (texture.type == TextType.ROLL_ALWAYS || UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY))) {
                    drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
                } else {
                    drawTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.texts.getFirst());
                }
                return;
            }
            if (texture.type == TextType.LEFT_HIDE) {
                if (UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY) && texture.texts.size() > 1) {
                    drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
                } else {
                    String line = texture.texts.getFirst() + (texture.texts.size() > 1 ? ".." : "");
                    float drawY = y + (height - textH) / 2f;
                    LDFonts.drawText(context, fontRenderer, line, (int) x, (int) drawY, texture.color, texture.dropShadow);
                }
                return;
            }
            if (texture.texts.size() > 1 && (texture.type == TextType.LEFT_ROLL_ALWAYS || UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY))) {
                drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
            } else {
                float drawY = y + (height - textH) / 2f;
                LDFonts.drawText(context, fontRenderer, texture.texts.getFirst(), (int) x, (int) drawY, texture.color, texture.dropShadow);
            }
        }

        private static void drawRollTextLine(TextTexture texture, GUIContext context, float x, float y, float width, float height,
                                             Font fontRenderer, int textH, String line) {
            float drawY = y + (height - textH) / 2f;
            float textW = fontRenderer.width(line);
            float totalW = width + textW + 10;
            float from = x + width;
            // Local coordinates, untransformed: GUIContext#enableScissor takes (x, y, width, height)
            // and GuiGraphicsExtractor applies the current pose to the rect itself. Passing
            // pose-transformed corners here both double-transformed the box and fed a corner in where
            // a size was expected, so the clip landed nowhere near the text.
            context.enableScissor(x, y, width, height);
            var t = texture.rollSpeed > 0 ? ((((texture.rollSpeed * Math.abs((int) (System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / totalW) : 0.5;
            LDFonts.drawText(context, fontRenderer, line, (int) (from - t * totalW), (int) drawY, texture.color, texture.dropShadow);
            context.disableScissor();
        }

        private static void drawTextLine(TextTexture texture, GUIContext context, float x, float y, float width, float height,
                                         Font fontRenderer, int textH, String line) {
            int textW = fontRenderer.width(line);
            float drawX = x + (width - textW) / 2f;
            float drawY = y + (height - textH) / 2f;
            LDFonts.drawText(context, fontRenderer, line, (int) drawX, (int) drawY, texture.color, texture.dropShadow);
        }
    }

    @LDLRegisterClient(name = "text_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredTextTextureRenderer implements RegisteredGuiTextureRenderer<TextTexture, RegisteredTextTextureRenderer> {
        @Override
        public Class<TextTexture> type() {
            return TextTexture.class;
        }

        @Override
        public void draw(TextTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(TextTexture texture, GUIContext context, float x, float y, float width, float height) {
            TextTextureClientSupport.draw(texture, context, x, y, width, height);
        }
    }
}
