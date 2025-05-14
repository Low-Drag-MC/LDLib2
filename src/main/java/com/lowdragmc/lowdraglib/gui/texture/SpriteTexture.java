package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.annotation.NumberColor;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nullable;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

@LDLRegisterClient(name = "shader_texture", registry = "ldlib:sprite_texture")
@Accessors(chain = true)
public class SpriteTexture extends TransformTexture {
    public enum WrapMode {
        CLAMP,
        REPEAT,
        MIRRORED_REPEAT
    }

    @Configurable(name = "ldlib.gui.editor.name.resource", forceUpdate = false)
    @Getter
    private ResourceLocation imageLocation = LDLib.id("textures/gui/icon.png");
    @Configurable
    @Setter
    public Position spritePosition = Position.of(0, 0);
    @Configurable
    @Setter
    public Size spriteSize = Size.of(1, 1);
    @Configurable
    @Setter
    public Position borderLT = Position.of(0, 0);
    @Configurable
    @Setter
    public Position borderRB = Position.of(0, 0);
    @Configurable
    @NumberColor
    public int color = -1;
    // TODO: wrap mode
    @Configurable
    public WrapMode wrapMode = WrapMode.CLAMP;
    @Nullable
    private Size imageSizeCache;

    public static SpriteTexture of(ResourceLocation imageLocation) {
        return new SpriteTexture().setImageLocation(imageLocation);
    }

    @ConfigSetter(field = "imageLocation")
    public SpriteTexture setImageLocation(ResourceLocation imageLocation) {
        this.imageLocation = imageLocation;
        this.imageSizeCache = null;
        return this;
    }

    public SpriteTexture setSprite(int x, int y, int width, int height) {
        this.spritePosition = Position.of(x, y);
        this.spriteSize = Size.of(width, height);
        return this;
    }

    public SpriteTexture setBorder(int left, int top, int right, int bottom) {
        this.borderLT = Position.of(left, top);
        this.borderRB = Position.of(right, bottom);
        return this;
    }


    @OnlyIn(Dist.CLIENT)
    public Size getImageSize() {
        if (imageSizeCache == null) {
            var abstracttexture = Minecraft.getInstance().getTextureManager().getTexture(imageLocation);
            imageSizeCache = abstracttexture instanceof ITextureSize textureSize ? Size.of(textureSize.ldlib$getImageWidth(), textureSize.ldlib$getImageHeight()) : Size.of(0, 0);
        }
        return imageSizeCache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        var poseStack = graphics.pose();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, imageLocation);

        var imageSize = getImageSize();
        // uv
        float uStart = spritePosition.getX() * 1f / imageSize.getWidth();
        float vStart = spritePosition.getY() * 1f / imageSize.getHeight();
        float uEnd = (spritePosition.getX() * 1f + spriteSize.getWidth()) / imageSize.getWidth();
        float vEnd = (spritePosition.getY() * 1f + spriteSize.getHeight()) / imageSize.getHeight();

        // border size
        float borderLeft = Math.min(borderLT.getX(), spriteSize.getWidth() / 2f);
        float borderRight = Math.min(borderRB.getX(), spriteSize.getWidth() / 2f);
        float borderTop = Math.min(borderLT.getY(), spriteSize.getHeight() / 2f);
        float borderBottom = Math.min(borderRB.getY(), spriteSize.getHeight() / 2f);

        // center area size
        float centerWidth = width - borderLeft - borderRight;
        float centerHeight = height - borderTop - borderBottom;

        // center uv
        float uCenterStart = uStart + borderLeft / imageSize.getWidth();
        float uCenterEnd = uEnd - borderRight / imageSize.getWidth();
        float vCenterStart = vStart + borderTop / imageSize.getHeight();
        float vCenterEnd = vEnd - borderBottom / imageSize.getHeight();

        // rendering
        var matrix = poseStack.last().pose();
        var tessellator = Tesselator.getInstance();
        var buffer = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX_COLOR);

        // 1. corners
        if (borderLeft > 0 && borderTop > 0) {
            drawQuad(buffer, matrix, x, y, borderLeft, borderTop,
                    uStart, vStart, uCenterStart, vCenterStart, color); // left top
        }
        if (borderRight > 0 && borderTop > 0) {
            drawQuad(buffer, matrix, x + width - borderRight, y, borderRight, borderTop,
                    uCenterEnd, vStart, uEnd, vCenterStart, color); // right top
        }
        if (borderLeft > 0 && borderBottom > 0) {
            drawQuad(buffer, matrix, x, y + height - borderBottom, borderLeft, borderBottom,
                    uStart, vCenterEnd, uCenterStart, vEnd, color); // left bottom
        }
        if (borderRight > 0 && borderBottom > 0) {
            drawQuad(buffer, matrix, x + width - borderRight, y + height - borderBottom, borderRight, borderBottom,
                    uCenterEnd, vCenterEnd, uEnd, vEnd, color); // right bottom
        }

        // 2. edges
        if (centerWidth > 0) {
            if (borderTop > 0) {
                drawQuad(buffer, matrix, x + borderLeft, y, centerWidth, borderTop,
                        uCenterStart, vStart, uCenterEnd, vCenterStart, color); // top
            }
            if (borderBottom > 0) {
                drawQuad(buffer, matrix, x + borderLeft, y + height - borderBottom, centerWidth, borderBottom,
                        uCenterStart, vCenterEnd, uCenterEnd, vEnd, color); // bottom
            }
        }
        if (centerHeight > 0) {
            if (borderLeft > 0) {
                drawQuad(buffer, matrix, x, y + borderTop, borderLeft, centerHeight,
                        uStart, vCenterStart, uCenterStart, vCenterEnd, color); // left
            }
            if (borderRight > 0) {
                drawQuad(buffer, matrix, x + width - borderRight, y + borderTop, borderRight, centerHeight,
                        uCenterEnd, vCenterStart, uEnd, vCenterEnd, color); // right
            }
        }

        // 3. center area
        if (centerWidth > 0 && centerHeight > 0) {
            drawQuad(buffer, matrix, x + borderLeft, y + borderTop, centerWidth, centerHeight,
                    uCenterStart, vCenterStart, uCenterEnd, vCenterEnd, color);
        }

        try {
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } catch (Exception e) {
            LDLib.LOGGER.error("Failed to draw sprite texture", e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawQuad(BufferBuilder buffer, Matrix4f matrix,
                          float x, float y, float w, float h,
                          float u1, float v1, float u2, float v2, int color) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        buffer.addVertex(matrix, x, y + h, 0).setUv(u1, v2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x + w, y + h, 0).setUv(u2, v2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x + w, y, 0).setUv(u2, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x, y, 0).setUv(u1, v1).setColor(r, g, b, a);
    }
}
