package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

import java.util.Collections;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public final class TextTextureClientSupport {
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
            texture.texts = Minecraft.getInstance()
                    .font.getSplitter()
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
        Font fontRenderer = Minecraft.getInstance().font;
        int textH = fontRenderer.lineHeight;
        if (texture.type == TextTexture.TextType.NORMAL) {
            textH *= texture.texts.size();
            for (int i = 0; i < texture.texts.size(); i++) {
                String line = texture.texts.get(i);
                int lineWidth = fontRenderer.width(line);
                float drawX = x + (width - lineWidth) / 2f;
                float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                context.graphics.drawString(fontRenderer, line, (int) drawX, (int) drawY, texture.color, texture.dropShadow);
            }
            return;
        }
        if (texture.type == TextTexture.TextType.LEFT) {
            textH *= texture.texts.size();
            for (int i = 0; i < texture.texts.size(); i++) {
                String line = texture.texts.get(i);
                float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                context.graphics.drawString(fontRenderer, line, (int) x, (int) drawY, texture.color, texture.dropShadow);
            }
            return;
        }
        if (texture.type == TextTexture.TextType.RIGHT) {
            textH *= texture.texts.size();
            for (int i = 0; i < texture.texts.size(); i++) {
                String line = texture.texts.get(i);
                int lineWidth = fontRenderer.width(line);
                float drawY = y + (height - textH) / 2f + i * fontRenderer.lineHeight;
                context.graphics.drawString(fontRenderer, line, (int) (x + width - lineWidth), (int) drawY, texture.color, texture.dropShadow);
            }
            return;
        }
        if (texture.type == TextTexture.TextType.HIDE) {
            if (UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY) && texture.texts.size() > 1) {
                drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
            } else {
                String line = texture.texts.getFirst() + (texture.texts.size() > 1 ? ".." : "");
                drawTextLine(texture, context, x, y, width, height, fontRenderer, textH, line);
            }
            return;
        }
        if (texture.type == TextTexture.TextType.ROLL || texture.type == TextTexture.TextType.ROLL_ALWAYS) {
            if (texture.texts.size() > 1 && (texture.type == TextTexture.TextType.ROLL_ALWAYS || UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY))) {
                drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
            } else {
                drawTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.texts.getFirst());
            }
            return;
        }
        if (texture.type == TextTexture.TextType.LEFT_HIDE) {
            if (UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY) && texture.texts.size() > 1) {
                drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
            } else {
                String line = texture.texts.getFirst() + (texture.texts.size() > 1 ? ".." : "");
                float drawY = y + (height - textH) / 2f;
                context.graphics.drawString(fontRenderer, line, (int) x, (int) drawY, texture.color, texture.dropShadow);
            }
            return;
        }
        if (texture.texts.size() > 1 && (texture.type == TextTexture.TextType.LEFT_ROLL_ALWAYS || UIElement.isMouseOverRect((int) x, (int) y, (int) width, (int) height, context.localMouseX, context.localMouseY))) {
            drawRollTextLine(texture, context, x, y, width, height, fontRenderer, textH, texture.text);
        } else {
            float drawY = y + (height - textH) / 2f;
            context.graphics.drawString(fontRenderer, texture.texts.getFirst(), (int) x, (int) drawY, texture.color, texture.dropShadow);
        }
    }

    private static void drawRollTextLine(TextTexture texture, GUIContext context, float x, float y, float width, float height,
                                         Font fontRenderer, int textH, String line) {
        float drawY = y + (height - textH) / 2f;
        float textW = fontRenderer.width(line);
        float totalW = width + textW + 10;
        float from = x + width;
        var trans = context.pose.pose;
        var realPos = trans.transformPosition(new Vector2f(x, y));
        var realPos2 = trans.transformPosition(new Vector2f(x + width, y + height));
        context.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        var t = texture.rollSpeed > 0 ? ((((texture.rollSpeed * Math.abs((int) (System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / totalW) : 0.5;
        context.graphics.drawString(fontRenderer, line, (int) (from - t * totalW), (int) drawY, texture.color, texture.dropShadow);
        context.disableScissor();
    }

    private static void drawTextLine(TextTexture texture, GUIContext context, float x, float y, float width, float height,
                                     Font fontRenderer, int textH, String line) {
        int textW = fontRenderer.width(line);
        float drawX = x + (width - textW) / 2f;
        float drawY = y + (height - textH) / 2f;
        context.graphics.drawString(fontRenderer, line, (int) drawX, (int) drawY, texture.color, texture.dropShadow);
    }
}
