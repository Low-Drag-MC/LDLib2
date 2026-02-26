package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.client.shader.management.ShaderProgram;
import com.lowdragmc.lowdraglib2.client.shader.uniform.UniformCache;
import com.lowdragmc.lowdraglib2.client.utils.RenderBufferUtils;
import com.lowdragmc.lowdraglib2.utils.FluidHelper;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Rect;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class DrawerHelper {

    public static void drawFluidTexture(VertexConsumer buffer, PoseStack.Pose pose, float xCoord, float yCoord, TextureAtlasSprite textureSprite, float maskTop, float maskRight, float zLevel, int fluidColor) {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax = uMax - maskRight / 16f * (uMax - uMin);
        vMax = vMax - maskTop / 16f * (vMax - vMin);

        var mat = pose.pose();
        buffer.addVertex(mat, xCoord, yCoord + 16, zLevel).setUv(uMin, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + 16, zLevel).setUv(uMax, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).setUv(uMax, vMin).setColor(fluidColor);
        buffer.addVertex(mat, xCoord, yCoord + maskTop, zLevel).setUv(uMin, vMin).setColor(fluidColor);
    }

    public static void drawFluidForGui(@Nonnull GuiGraphics graphics, FluidStack contents, float startX, float startY, float widthT, float heightT, int color) {
        ResourceLocation LOCATION_BLOCKS_TEXTURE = InventoryMenu.BLOCK_ATLAS;
        TextureAtlasSprite fluidStillSprite = FluidHelper.getStillTexture(contents);
        if (fluidStillSprite == null) {
            fluidStillSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
            if (Platform.isDevEnv()) {
                LDLib2.LOGGER.error("Missing fluid texture for fluid: " + contents.getHoverName().getString());
            }
        }

        int fluidColor = FluidHelper.getColor(contents) | 0xff000000;
        if (color != -1) {
            fluidColor = ColorUtils.mulColor(fluidColor, color);
        }


        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(LOCATION_BLOCKS_TEXTURE));
        RenderSystem.disableDepthTest();

        final int xTileCount = (int) (widthT / 16);
        final float xRemainder = widthT - xTileCount * 16;
        final int yTileCount = (int) (heightT / 16);
        final float yRemainder = heightT - yTileCount * 16;

        final float yStart = startY + heightT;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                float width = xTile == xTileCount ? xRemainder : 16;
                float height = yTile == yTileCount ? yRemainder : 16;
                float x = startX + xTile * 16;
                float y = yStart - (yTile + 1) * 16;
                if (width > 0 && height > 0) {
                    float maskTop = 16 - height;
                    float maskRight = 16 - width;
                    drawFluidTexture(buffer, graphics.pose().last(), x, y, fluidStillSprite, maskTop, maskRight, 0, fluidColor);
                }
            }
        }
        RenderSystem.enableBlend();
    }

    public static void drawBorder(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int color, int border) {
        if (border >= 0) {
            drawSolidRect(graphics,x - border, y + height, width + 2 * border, border, color);
            drawSolidRect(graphics,x - border, y, border, height, color);
            drawSolidRect(graphics,x + width, y, border, height, color);
            drawSolidRect(graphics,x - border, y - border, width + 2 * border, border, color);
        } else {
            float absBorder = Math.abs(border);
            drawSolidRect(graphics, x, y, width - absBorder, absBorder, color);
            drawSolidRect(graphics, x, y + absBorder, absBorder, height - absBorder, color);
            drawSolidRect(graphics, x + absBorder, y + height - absBorder, width - absBorder, absBorder, color);
            drawSolidRect(graphics, x + width - absBorder, y, absBorder, height - absBorder, color);
        }
    }

    public static void drawStringSized(@Nonnull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale, boolean center) {
        graphics.pose().pushPose();
        Font fontRenderer = Minecraft.getInstance().font;
        double scaledTextWidth = center ? fontRenderer.getSplitter().stringWidth(text) * scale : 0.0;
        graphics.pose().translate(x - scaledTextWidth / 2.0, y, 0.0f);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawString(fontRenderer, text, 0, 0, color, dropShadow);
        graphics.pose().popPose();
    }

    public static void drawStringFixedCorner(@Nonnull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale) {
        Font fontRenderer = Minecraft.getInstance().font;
        float scaledWidth = fontRenderer.getSplitter().stringWidth(text) * scale;
        float scaledHeight = fontRenderer.lineHeight * scale;
        drawStringSized(graphics, text, x - scaledWidth, y - scaledHeight, color, dropShadow, scale, false);
    }

    public static void drawText(@Nonnull GuiGraphics graphics, String text, float x, float y, float scale, int color) {
        drawText(graphics, text, x, y, scale, color, false);
    }

    public static void drawText(@Nonnull GuiGraphics graphics, String text, float x, float y, float scale, int color, boolean shadow) {
        Font fontRenderer = Minecraft.getInstance().font;
        RenderSystem.disableBlend();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 0f);
        float sf = 1 / scale;
        graphics.drawString(fontRenderer, text, (int) (x * sf), (int) (y * sf), color, shadow);
        graphics.pose().popPose();
        RenderSystem.enableBlend();
    }

    public static void drawItemStack(@Nonnull GuiGraphics graphics, ItemStack itemStack, int x, int y, int color, @Nullable String altTxt) {
        if (itemStack.isEmpty()) return;
        var a = ColorUtils.alpha(color);
        var r = ColorUtils.red(color);
        var g = ColorUtils.green(color);
        var b = ColorUtils.blue(color);
        RenderSystem.setShaderColor(r, g, b, a);

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        Minecraft mc = Minecraft.getInstance();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 232);
        graphics.renderItem(itemStack, x, y);
        var font = IClientItemExtensions.of(itemStack).getFont(itemStack, IClientItemExtensions.FontContext.ITEM_COUNT);
        graphics.renderItemDecorations(font == null ? mc.font : font, itemStack, x, y, altTxt);
        graphics.pose().popPose();

        // clear depth buffer,it may cause some rendering issues?
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
    }

    public static List<Component> getItemToolTip(ItemStack itemStack) {
        Minecraft mc = Minecraft.getInstance();
        return Screen.getTooltipFromItem(mc, itemStack);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, Rect rect, int color) {
        drawSolidRect(graphics, rect.left, rect.up, rect.right, rect.down, color);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int color) {
        drawSolidRect(graphics, RenderType.guiOverlay(), x, y, width, height, color);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, RenderType type, float x, float y, float width, float height, int color) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(type);
        RenderSystem.disableDepthTest();
        vertexconsumer.addVertex(matrix4f, x, y, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x, y + height, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x + width, y + height, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x + width, y, 0).setColor(color);
    }

    public static void drawRectShadow(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int distance) {
        drawGradientRect(graphics, x + distance, y + height, width - distance, distance, 0x4f000000, 0, false);
        drawGradientRect(graphics, x + width, y + distance, distance, height - distance, 0x4f000000, 0, true);

        float startAlpha = (float) (0x4f) / 255.0F;
        var buffer = graphics.bufferSource().getBuffer(RenderType.guiOverlay());
        RenderSystem.disableDepthTest();

        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        x += width;
        y += height;
        Matrix4f mat = graphics.pose().last().pose();
        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);

        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y, 0).setColor(0, 0, 0, 0);
    }

    public static void drawGradientRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int startColor, int endColor) {
        drawGradientRect(graphics, x, y, width, height, startColor, endColor, false);
    }

    public static void drawGradientRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
        float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
        float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
        float startBlue  = (float)(startColor       & 255) / 255.0F;
        float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
        float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
        float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
        float endBlue    = (float)(endColor         & 255) / 255.0F;
        var buffer = graphics.bufferSource().getBuffer(RenderType.guiOverlay());
        RenderSystem.disableDepthTest();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        Matrix4f mat = graphics.pose().last().pose();
        if (horizontal) {
            buffer.addVertex(mat,x + width, y, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
        } else {
            buffer.addVertex(mat,x + width, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
        }
    }

    public static void drawLines(@Nonnull GuiGraphics graphics, List<Vector2f> points, int startColor, int endColor, float width) {
        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.stripLines());
        RenderSystem.disableDepthTest();
        RenderBufferUtils.drawColorLines(graphics.pose(), buffer, points, startColor, endColor, width);
    }

    public static void drawTexLines(@Nonnull GuiGraphics graphics, RenderType renderType, List<Vector2f> points, int startColor, int endColor, float width) {
        var buffer = graphics.bufferSource().getBuffer(renderType);
        RenderSystem.disableDepthTest();
        RenderBufferUtils.drawColorTexLines(graphics.pose(), buffer, points, startColor, endColor, width, true);
    }

    public static void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltipTexts, ItemStack tooltipStack, @Nullable TooltipComponent tooltipComponent, Font tooltipFont) {
        graphics.renderTooltip(tooltipFont, tooltipTexts, Optional.ofNullable(tooltipComponent), tooltipStack, mouseX, mouseY);
    }

    public static ClientTooltipComponent getClientTooltipComponent(TooltipComponent component) {
        return ClientTooltipComponent.create(component);
    }
}
