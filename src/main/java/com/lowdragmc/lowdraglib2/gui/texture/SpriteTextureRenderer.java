package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.gui.texture.rendering.SpriteTextureClientSupport;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.math.Size;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SpriteTextureRenderer {
    private SpriteTextureRenderer() {
    }

    public static void draw(SpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
        TransformTextureRenderer.draw(texture, context, x, y, width, height, SpriteTextureRenderer::drawInternal);
    }

    private static void drawInternal(SpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        var imageSize = SpriteTextureClientSupport.getImageSize(texture);
        var spriteSize = resolveSpriteSize(texture, imageSize);
        float uStart = texture.spritePosition.getX() * 1f / imageSize.getWidth();
        float vStart = texture.spritePosition.getY() * 1f / imageSize.getHeight();
        float uEnd = (texture.spritePosition.getX() * 1f + spriteSize.getWidth()) / imageSize.getWidth();
        float vEnd = (texture.spritePosition.getY() * 1f + spriteSize.getHeight()) / imageSize.getHeight();

        float borderLeft = Math.min(texture.borderLT.getX(), spriteSize.getWidth() / 2f);
        float borderRight = Math.min(texture.borderRB.getX(), spriteSize.getWidth() / 2f);
        float borderTop = Math.min(texture.borderLT.getY(), spriteSize.getHeight() / 2f);
        float borderBottom = Math.min(texture.borderRB.getY(), spriteSize.getHeight() / 2f);

        float centerWidth = width - borderLeft - borderRight;
        float centerHeight = height - borderTop - borderBottom;

        float uCenterStart = uStart + borderLeft / imageSize.getWidth();
        float uCenterEnd = uEnd - borderRight / imageSize.getWidth();
        float vCenterStart = vStart + borderTop / imageSize.getHeight();
        float vCenterEnd = vEnd - borderBottom / imageSize.getHeight();

        if (borderLeft > 0 && borderTop > 0) {
            drawQuad(texture, context, x, y, borderLeft, borderTop, uStart, vStart, uCenterStart, vCenterStart);
        }
        if (borderRight > 0 && borderTop > 0) {
            drawQuad(texture, context, x + width - borderRight, y, borderRight, borderTop, uCenterEnd, vStart, uEnd, vCenterStart);
        }
        if (borderLeft > 0 && borderBottom > 0) {
            drawQuad(texture, context, x, y + height - borderBottom, borderLeft, borderBottom, uStart, vCenterEnd, uCenterStart, vEnd);
        }
        if (borderRight > 0 && borderBottom > 0) {
            drawQuad(texture, context, x + width - borderRight, y + height - borderBottom, borderRight, borderBottom, uCenterEnd, vCenterEnd, uEnd, vEnd);
        }

        if (centerWidth > 0) {
            if (borderTop > 0) {
                drawQuad(texture, context, x + borderLeft, y, centerWidth, borderTop, uCenterStart, vStart, uCenterEnd, vCenterStart);
            }
            if (borderBottom > 0) {
                drawQuad(texture, context, x + borderLeft, y + height - borderBottom, centerWidth, borderBottom, uCenterStart, vCenterEnd, uCenterEnd, vEnd);
            }
        }
        if (centerHeight > 0) {
            if (borderLeft > 0) {
                drawQuad(texture, context, x, y + borderTop, borderLeft, centerHeight, uStart, vCenterStart, uCenterStart, vCenterEnd);
            }
            if (borderRight > 0) {
                drawQuad(texture, context, x + width - borderRight, y + borderTop, borderRight, centerHeight, uCenterEnd, vCenterStart, uEnd, vCenterEnd);
            }
        }

        if (centerWidth <= 0 || centerHeight <= 0) {
            return;
        }
        if (texture.wrapMode == SpriteTexture.WrapMode.CLAMP) {
            drawQuad(texture, context, x + borderLeft, y + borderTop, centerWidth, centerHeight, uCenterStart, vCenterStart, uCenterEnd, vCenterEnd);
            return;
        }

        float centerSpriteWidth = spriteSize.getWidth() - borderLeft - borderRight;
        float centerSpriteHeight = spriteSize.getHeight() - borderTop - borderBottom;
        if (centerSpriteHeight <= 0 || centerSpriteWidth <= 0) {
            return;
        }
        float ox = x + borderLeft;
        float oy = y + borderTop;
        float remainY = centerHeight;
        int tileRow = 0;
        while (remainY > 0) {
            float tileH = Math.min(centerSpriteHeight, remainY);
            float remainX = centerWidth;
            int tileCol = 0;
            while (remainX > 0) {
                float tileW = Math.min(centerSpriteWidth, remainX);
                float tileUFrac = tileW / centerSpriteWidth;
                float tileVFrac = tileH / centerSpriteHeight;

                float tu0;
                float tu1;
                float tv0;
                float tv1;
                if (texture.wrapMode == SpriteTexture.WrapMode.MIRRORED_REPEAT) {
                    boolean flipX = (tileCol % 2) != 0;
                    boolean flipY = (tileRow % 2) != 0;
                    tu0 = flipX ? uCenterEnd : uCenterStart;
                    tu1 = flipX ? uCenterEnd - tileUFrac * (uCenterEnd - uCenterStart)
                            : uCenterStart + tileUFrac * (uCenterEnd - uCenterStart);
                    tv0 = flipY ? vCenterEnd : vCenterStart;
                    tv1 = flipY ? vCenterEnd - tileVFrac * (vCenterEnd - vCenterStart)
                            : vCenterStart + tileVFrac * (vCenterEnd - vCenterStart);
                } else {
                    tu0 = uCenterStart;
                    tu1 = uCenterStart + tileUFrac * (uCenterEnd - uCenterStart);
                    tv0 = vCenterStart;
                    tv1 = vCenterStart + tileVFrac * (vCenterEnd - vCenterStart);
                }

                drawQuad(texture, context, ox + centerWidth - remainX, oy + centerHeight - remainY, tileW, tileH, tu0, tv0, tu1, tv1);
                remainX -= tileW;
                tileCol++;
            }
            remainY -= tileH;
            tileRow++;
        }
    }

    private static Size resolveSpriteSize(SpriteTexture texture, Size imageSize) {
        var spriteSize = texture.spriteSize;
        if (spriteSize.getWidth() <= 0 || spriteSize.getHeight() <= 0) {
            return imageSize;
        }
        return spriteSize;
    }

    private static void drawQuad(SpriteTexture texture, GUIContext context,
                                 float x, float y, float w, float h,
                                 float u0, float v0, float u1, float v1) {
        context.blit(RenderPipelines.GUI_TEXTURED, texture.getImageLocation(),
                x, y, w, h, u0, v0, u1, v1, texture.color);
    }
}
