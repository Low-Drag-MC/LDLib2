package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.shader.Shaders;
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
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class DrawerHelper {

    public static ShaderProgram ROUND;
    public static ShaderProgram PANEL_BG;
    public static ShaderProgram ROUND_BOX;
    public static ShaderProgram PROGRESS_ROUND_BOX;
    public static ShaderProgram FRAME_ROUND_BOX;
    public static ShaderProgram ROUND_LINE;

    public static void init() {
        ROUND = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.ROUND_F).attach(Shaders.SCREEN_V));
        PANEL_BG = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.PANEL_BG_F).attach(Shaders.SCREEN_V));
        ROUND_BOX = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.ROUND_BOX_F).attach(Shaders.SCREEN_V));
        PROGRESS_ROUND_BOX = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.PROGRESS_ROUND_BOX_F).attach(Shaders.SCREEN_V));
        FRAME_ROUND_BOX = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.FRAME_ROUND_BOX_F).attach(Shaders.SCREEN_V));
        ROUND_LINE = Util.make(new ShaderProgram(), program
                -> program.attach(Shaders.ROUND_LINE_F).attach(Shaders.SCREEN_V));
    }

    public static void drawFluidTexture(@Nonnull GuiGraphics graphics, float xCoord, float yCoord, TextureAtlasSprite textureSprite, float maskTop, float maskRight, float zLevel, int fluidColor) {
        float uMin = textureSprite.getU0();
        float uMax = textureSprite.getU1();
        float vMin = textureSprite.getV0();
        float vMax = textureSprite.getV1();
        uMax = uMax - maskRight / 16f * (uMax - uMin);
        vMax = vMax - maskTop / 16f * (vMax - vMin);

        // TODO optimal rendertype?
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        var mat = graphics.pose().last().pose();
        buffer.addVertex(mat, xCoord, yCoord + 16, zLevel).setUv(uMin, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + 16, zLevel).setUv(uMax, vMax).setColor(fluidColor);
        buffer.addVertex(mat, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).setUv(uMax, vMin).setColor(fluidColor);
        buffer.addVertex(mat, xCoord, yCoord + maskTop, zLevel).setUv(uMin, vMin).setColor(fluidColor);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void drawFluidForGui(@Nonnull GuiGraphics graphics, FluidStack contents, float startX, float startY, float widthT, float heightT) {
        ResourceLocation LOCATION_BLOCKS_TEXTURE = InventoryMenu.BLOCK_ATLAS;
        TextureAtlasSprite fluidStillSprite = FluidHelper.getStillTexture(contents);
        if (fluidStillSprite == null) {
            fluidStillSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(MissingTextureAtlasSprite.getLocation());
            if (Platform.isDevEnv()) {
                LDLib2.LOGGER.error("Missing fluid texture for fluid: " + contents.getHoverName().getString());
            }
        }
        int fluidColor = FluidHelper.getColor(contents) | 0xff000000;
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, LOCATION_BLOCKS_TEXTURE);

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
                    drawFluidTexture(graphics, x, y, fluidStillSprite, maskTop, maskRight, 0, fluidColor);
                }
            }
        }
        RenderSystem.enableBlend();
    }

    public static void drawBorder(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int color, int border) {
        graphics.drawManaged(() -> {
            if (border >= 0) {
                drawSolidRect(graphics,x - border, y + height, width + 2 * border, border, color, false);
                drawSolidRect(graphics,x - border, y, border, height, color, false);
                drawSolidRect(graphics,x + width, y, border, height, color, false);
                drawSolidRect(graphics,x - border, y - border, width + 2 * border, border, color, false);
            } else {
                float absBorder = Math.abs(border);
                drawSolidRect(graphics, x, y, width - absBorder, absBorder, color, false);
                drawSolidRect(graphics, x, y + absBorder, absBorder, height - absBorder, color, false);
                drawSolidRect(graphics, x + absBorder, y + height - absBorder, width - absBorder, absBorder, color, false);
                drawSolidRect(graphics, x + width - absBorder, y, absBorder, height - absBorder, color, false);
            }
        });
    }

    public static void drawStringSized(@Nonnull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale, boolean center) {
        graphics.pose().pushPose();
        Font fontRenderer = Minecraft.getInstance().font;
        double scaledTextWidth = center ? fontRenderer.width(text) * scale : 0.0;
        graphics.pose().translate(x - scaledTextWidth / 2.0, y, 0.0f);
        graphics.pose().scale(scale, scale, scale);
        graphics.drawString(fontRenderer, text, 0, 0, color, dropShadow);
        graphics.pose().popPose();
    }

    public static void drawStringFixedCorner(@Nonnull GuiGraphics graphics, String text, float x, float y, int color, boolean dropShadow, float scale) {
        Font fontRenderer = Minecraft.getInstance().font;
        float scaledWidth = fontRenderer.width(text) * scale;
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
        graphics.renderItemDecorations(mc.font, itemStack, x, y, altTxt);
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
        drawSolidRect(graphics, x, y, width, height, color, true);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int color, boolean flush) {
        drawSolidRect(graphics, RenderType.gui(), x, y, width, height, color, flush);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, RenderType type, float x, float y, float width, float height, int color) {
        drawSolidRect(graphics, type, x, y, width, height, color, true);
    }

    public static void drawSolidRect(@Nonnull GuiGraphics graphics, RenderType type, float x, float y, float width, float height, int color, boolean flush) {
        Matrix4f matrix4f = graphics.pose().last().pose();
        VertexConsumer vertexconsumer = graphics.bufferSource().getBuffer(type);
        vertexconsumer.addVertex(matrix4f, x, y, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x, y + height, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x + width, y + height, 0).setColor(color);
        vertexconsumer.addVertex(matrix4f, x + width, y, 0).setColor(color);
        if (flush) graphics.flush();
    }

    public static void drawRectShadow(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, int distance) {
        drawGradientRect(graphics, x + distance, y + height, width - distance, distance, 0x4f000000, 0, false);
        drawGradientRect(graphics, x + width, y + distance, distance, height - distance, 0x4f000000, 0, true);

        float startAlpha = (float) (0x4f) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        x += width;
        y += height;
        Matrix4f mat = graphics.pose().last().pose();
        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);

        buffer.addVertex(mat, x, y, 0).setColor(0, 0, 0, startAlpha);
        buffer.addVertex(mat, x + distance, y + distance, 0).setColor(0, 0, 0, 0);
        buffer.addVertex(mat, x + distance, y, 0).setColor(0, 0, 0, 0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
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
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Matrix4f mat = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        if (horizontal) {
            buffer.addVertex(mat,x + width, y, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } else {
            buffer.addVertex(mat,x + width, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x + width, y + height, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
    }

    public static void drawLines(@Nonnull GuiGraphics graphics, List<Vec2> Vec2s, int startColor, int endColor, float width) {
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        RenderBufferUtils.drawColorLines(graphics.pose(), bufferbuilder, Vec2s, startColor, endColor, width);

        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    public static void drawTextureRect(@Nonnull GuiGraphics graphics, float x, float y, float width, float height) {
        Tesselator tesselator = Tesselator.getInstance();
        Matrix4f mat = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(mat, x, y + height, 0).setUv(0, 0);
        buffer.addVertex(mat, x + width, y + height, 0).setUv(1, 0);
        buffer.addVertex(mat, x + width, y, 0).setUv(1, 1);
        buffer.addVertex(mat, x, y, 0).setUv(0, 1);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    public static void updateScreenVshUniform(@Nonnull GuiGraphics graphics, UniformCache uniform) {
        var window = Minecraft.getInstance().getWindow();

        uniform.glUniform1F("GuiScale", (float) window.getGuiScale());
        uniform.glUniform2F("ScreenSize", (float) window.getWidth(), (float) window.getHeight());
        uniform.glUniformMatrix4F("PoseStack",graphics.pose().last().pose());
        uniform.glUniformMatrix4F("ProjMat", RenderSystem.getProjectionMatrix());
    }

    public static void drawRound(@Nonnull GuiGraphics graphics, int color, float radius, Position centerPos) {
        DrawerHelper.ROUND.use(uniform -> {

            DrawerHelper.updateScreenVshUniform(graphics, uniform);

            uniform.fillRGBAColor("Color", color);

            uniform.glUniform1F("StepLength", 1f);
            uniform.glUniform1F("Radius", radius);
            uniform.glUniform2F("CenterPos", centerPos.x, centerPos.y);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    public static void drawPanelBg(@Nonnull GuiGraphics graphics) {
        DrawerHelper.PANEL_BG.use(uniform -> {

            DrawerHelper.updateScreenVshUniform(graphics, uniform);

            uniform.glUniform1F("Density", 5);
            uniform.glUniform1F("SquareSize", 0.1f);
            var bg = 20f / 255f;
            uniform.glUniform4F("BgColor", bg, bg, bg, 0.95f);
            var square = 40f / 255f;
            uniform.glUniform4F("SquareColor", square, square, square, 0.95f);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    public static void drawRoundBox(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, Vector4f radius, int color) {
        DrawerHelper.ROUND_BOX.use(uniform -> {
            DrawerHelper.updateScreenVshUniform(graphics, uniform);
            uniform.glUniformMatrix4F("PoseStack", new Matrix4f());
            var point1 = new Vector4f(x - 0.25f, y - 0.25f, 0, 1);
            var point2 = new Vector4f(x + width - 0.25f, y + height - 0.25f, 0, 1);
            var matrix = graphics.pose().last().pose();
            point1.mul(matrix);
            point2.mul(matrix);
            var v1 = matrix.transform(new Vector4f(1, 1, 1, 1));
            var v2 = matrix.transform(new Vector4f(0, 0, 0, 1));
            var scale = v1.x - v2.x; // we just use the x scale

            uniform.glUniform4F("SquareVertex", point1.x, point1.y, point2.x, point2.y);
            uniform.glUniform4F("RoundRadius", radius.x() * scale, radius.y() * scale, radius.z() * scale, radius.w() * scale);
            uniform.fillRGBAColor("Color", color);
            uniform.glUniform1F("Blur", 2);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    public static void drawProgressRoundBox(@Nonnull GuiGraphics graphics, Rect square, Vector4f radius, int color1, int color2, float progress) {
        DrawerHelper.PROGRESS_ROUND_BOX.use(uniform -> {
            DrawerHelper.updateScreenVshUniform(graphics, uniform);

            uniform.glUniform4F("SquareVertex", square.left, square.up, square.right, square.down);
            uniform.glUniform4F("RoundRadius", radius.x(), radius.y(), radius.z(), radius.w());
            uniform.fillRGBAColor("Color1", color1);
            uniform.fillRGBAColor("Color2", color2);
            uniform.glUniform1F("Blur", 2);
            uniform.glUniform1F("Progress", progress);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    public static void drawFrameRoundBox(@Nonnull GuiGraphics graphics, float x, float y, float width, float height, float thickness, Vector4f radius1, Vector4f radius2, int color) {
        DrawerHelper.FRAME_ROUND_BOX.use(uniform -> {
            DrawerHelper.updateScreenVshUniform(graphics, uniform);
            uniform.glUniformMatrix4F("PoseStack", new Matrix4f());
            var point1 = new Vector4f(x - 0.25f, y - 0.25f, 0, 1);
            var point2 = new Vector4f(x + width - 0.25f, y + height - 0.25f, 0, 1);
            var matrix = graphics.pose().last().pose();
            point1.mul(matrix);
            point2.mul(matrix);
            var v1 = matrix.transform(new Vector4f(1, 1, 1, 1));
            var v2 = matrix.transform(new Vector4f(0, 0, 0, 1));
            var scale = v1.x - v2.x; // we just use the x scale

            uniform.glUniform4F("SquareVertex", point1.x, point1.y, point2.x, point2.y);
            uniform.glUniform4F("RoundRadius1", radius1.x() * scale, radius1.y() * scale, radius1.z() * scale, radius1.w() * scale);
            uniform.glUniform4F("RoundRadius2", radius2.x() * scale, radius2.y() * scale, radius2.z() * scale, radius2.w() * scale);
            uniform.glUniform1F("Thickness", thickness * scale);
            uniform.fillRGBAColor("Color", color);
            uniform.glUniform1F("Blur", 2);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    public static void drawRoundLine(@Nonnull GuiGraphics graphics, Position begin, Position end, int width, int color1, int color2) {
        DrawerHelper.ROUND_LINE.use(uniform -> {
            DrawerHelper.updateScreenVshUniform(graphics, uniform);

            uniform.glUniform1F("Width", width);
            uniform.glUniform2F("Point1", begin.x, begin.y);
            uniform.glUniform2F("Point2", end.x, end.y);
            uniform.fillRGBAColor("Color1", color1);
            uniform.fillRGBAColor("Color2", color2);
            uniform.glUniform1F("Blur", 2);
        });

        RenderSystem.enableBlend();
        uploadScreenPosVertex();
    }

    private static void uploadScreenPosVertex() {
        var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1, 1, 0);
        builder.addVertex(-1, -1, 0);
        builder.addVertex(1, -1, 0);
        builder.addVertex(1, 1, 0);
        BufferUploader.draw(builder.buildOrThrow());
    }

    public static void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, List<Component> tooltipTexts, ItemStack tooltipStack, @Nullable TooltipComponent tooltipComponent, Font tooltipFont) {
        graphics.renderTooltip(tooltipFont, tooltipTexts, Optional.ofNullable(tooltipComponent), tooltipStack, mouseX, mouseY);
    }

    public static ClientTooltipComponent getClientTooltipComponent(TooltipComponent component) {
        return ClientTooltipComponent.create(component);
    }
}
