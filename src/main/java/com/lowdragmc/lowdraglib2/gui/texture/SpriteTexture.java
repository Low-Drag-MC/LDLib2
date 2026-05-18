package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import org.jetbrains.annotations.Nullable;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

@KJSBindings
@LDLRegisterClient(name = "sprite_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class SpriteTexture extends TransformTexture {
    public enum WrapMode {
        CLAMP,
        REPEAT,
        MIRRORED_REPEAT
    }

    @Configurable(name = "ldlib.gui.editor.name.resource")
    @Getter
    private ResourceLocation imageLocation = LDLib2.id("textures/gui/icon.png");
    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position spritePosition = Position.of(0, 0);
    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Size spriteSize = Size.of(0, 0);
    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position borderLT = Position.of(0, 0);
    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public Position borderRB = Position.of(0, 0);
    @Configurable
    @ConfigColor
    public int color = -1;
    @Configurable
    @Setter
    public WrapMode wrapMode = WrapMode.CLAMP;
    // runtime
    @Nullable
    private Size imageSizeCache;

    @HideFromJS
    public static SpriteTexture of(ResourceLocation imageLocation) {
        return new SpriteTexture().setImageLocation(imageLocation);
    }

    @HideFromJS
    public static SpriteTexture of(String imageLocation) {
        return of(ResourceLocation.parse(imageLocation));
    }

    @RemapForJS("of")
    public static SpriteTexture kjs$of(ResourceLocation imageLocation) {
        return of(imageLocation);
    }

    @ConfigSetter(field = "imageLocation")
    public SpriteTexture setImageLocation(ResourceLocation imageLocation) {
        this.imageLocation = imageLocation;
        this.imageSizeCache = null;
        return this;
    }

    /**
     * Sets the sprite position and size.
     *
     * @param x      The x position of the sprite in the image.
     * @param y      The y position of the sprite in the image.
     * @param width  The width of the sprite in pixels.
     * @param height The height of the sprite in pixels.
     * @return This SpriteTexture instance for chaining.
     */
    public SpriteTexture setSprite(int x, int y, int width, int height) {
        this.spritePosition = Position.of(x, y);
        this.spriteSize = Size.of(width, height);
        return this;
    }

    /**
     * Sets the border size for the sprite.
     *
     * @param left   The left border size in pixels.
     * @param top    The top border size in pixels.
     * @param right  The right border size in pixels.
     * @param bottom The bottom border size in pixels.
     * @return This SpriteTexture instance for chaining.
     */
    public SpriteTexture setBorder(int left, int top, int right, int bottom) {
        this.borderLT = Position.of(left, top);
        this.borderRB = Position.of(right, bottom);
        return this;
    }

    public SpriteTexture setBorder(int border) {
        return setBorder(border, border, border, border);
    }

    @Override
    public SpriteTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public SpriteTexture copy() {
        var copied = new SpriteTexture()
                .setImageLocation(imageLocation)
                .setSprite(spritePosition.getX(), spritePosition.getY(), spriteSize.getWidth(), spriteSize.getHeight())
                .setBorder(borderLT.getX(), borderLT.getY(), borderRB.getX(), borderRB.getY())
                .setColor(color)
                .setWrapMode(wrapMode);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof SpriteTexture spriteTexture) {
            return new IGuiTexture() {
                @Override
                @OnlyIn(Dist.CLIENT)
                public void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height,
                                 float partialTicks) {
                    SpriteTexture.this.draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
                    var currentColor = spriteTexture.color;
                    spriteTexture.color = ColorUtils.color(
                            ColorUtils.alpha(currentColor) * lerp,
                            ColorUtils.red(currentColor) * lerp,
                            ColorUtils.green(currentColor) * lerp,
                            ColorUtils.blue(currentColor) * lerp);
                    spriteTexture.draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
                    spriteTexture.color = currentColor;
                }
            };
        }
        return super.interpolate(other, lerp);
    }

    @OnlyIn(Dist.CLIENT)
    public Size getImageSize() {
        if (imageSizeCache == null) {
            try {
                imageSizeCache = Minecraft.getInstance().getTextureManager().getTexture(imageLocation) instanceof ITextureSize textureSize ?
                        Size.of(textureSize.ldlib2$getImageWidth(), textureSize.ldlib2$getImageHeight()) : Size.of(1, 1);
            } catch (Exception e) {
                imageSizeCache = Size.of(1, 1);
            }
        }
        return imageSizeCache;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        if (width <= 0 || height <= 0) {
            return;
        }
        var poseStack = graphics.pose();

        var imageSize = getImageSize();
        var spriteSize = this.spriteSize;
        // if the sprite size is not set, use the image size
        if (spriteSize.getWidth() <= 0 || spriteSize.getHeight() <= 0) {
            spriteSize = imageSize;
        }
        // uv
        float uStart = spritePosition.getX() * 1f / imageSize.getWidth();
        float vStart = spritePosition.getY() * 1f / imageSize.getHeight();
        float uEnd = (spritePosition.getX() * 1f + spriteSize.getWidth()) / imageSize.getWidth();
        float vEnd = (spritePosition.getY() * 1f + spriteSize.getHeight()) / imageSize.getHeight();

        // border size — only scale down when the two opposing borders together exceed the sprite dimension
        float borderLeft = borderLT.getX();
        float borderRight = borderRB.getX();
        float borderTop = borderLT.getY();
        float borderBottom = borderRB.getY();
        float hSum = borderLeft + borderRight;
        if (hSum > spriteSize.getWidth() && hSum > 0) {
            float s = spriteSize.getWidth() / hSum;
            borderLeft *= s;
            borderRight *= s;
        }
        float vSum = borderTop + borderBottom;
        if (vSum > spriteSize.getHeight() && vSum > 0) {
            float s = spriteSize.getHeight() / vSum;
            borderTop *= s;
            borderBottom *= s;
        }

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
        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(imageLocation));
        RenderSystem.disableDepthTest();

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
            if (wrapMode == WrapMode.CLAMP) {
                drawQuad(buffer, matrix, x + borderLeft, y + borderTop, centerWidth, centerHeight,
                        uCenterStart, vCenterStart, uCenterEnd, vCenterEnd, color);
            } else {
                graphics.flush();

                // wrap mode
                var centerSpriteWidth = spriteSize.getWidth() - borderLeft - borderRight;
                var centerSpriteHeight = spriteSize.getHeight() - borderTop - borderBottom;
                if (centerSpriteHeight <= 0 || centerSpriteWidth <= 0) {
                    return;
                }

                // Risky?
                RenderSystem.setShader(LDLibShaders::getSpriteBlitShader);
                RenderSystem.setShaderTexture(0, imageLocation);
                var buffer2 = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, POSITION_TEX_COLOR);
                var shader = LDLibShaders.getSpriteBlitShader();
                shader.safeGetUniform("UVBounds").set(uCenterStart, vCenterStart, uCenterEnd, vCenterEnd);
                shader.safeGetUniform("WrapMode").set(wrapMode.ordinal());

                var u1 = centerWidth / centerSpriteWidth * (uCenterEnd - uCenterStart) + uCenterStart;
                var v1 = centerHeight / centerSpriteHeight * (vCenterEnd - vCenterStart) + vCenterStart;
                drawQuad(buffer2, matrix, x + borderLeft, y + borderTop, centerWidth, centerHeight,
                        uCenterStart, vCenterStart, u1, v1, color);

                // draw border first
                var bufferData = buffer2.build();
                if (bufferData != null) {
                    BufferUploader.drawWithShader(bufferData);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void drawQuad(VertexConsumer buffer, Matrix4f matrix,
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public void createPreview(ConfiguratorGroup father) {
        super.createPreview(father);
        var configurator = new Configurator("ldlib.gui.editor.group.base_image");
        father.addConfigurators(configurator
                .addChildren(
                        // raw image preview
                        new UIElement().layout(layout -> {
                                    layout.setPipelineState(StyleOrigin.DEFAULT);
                                    layout.setAspectRatio(1.0f);
                                    layout.widthPercent(80);
                                    layout.paddingAll(3);
                                    layout.alignSelf(AlignItems.CENTER);
                                    layout.setPipelineState(StyleOrigin.INLINE);
                                }).style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)))
                                .addClass("preview_bg")
                                .addChild(new UIElement().layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.heightPercent(100);
                                }).style(style -> style.backgroundTexture(this::drawRawTextureGuides))),
                        // button to select image
                        new Button().setText("ldlib.gui.editor.tips.select_image").setOnClick(e -> {
                            Dialog.showFileDialog("ldlib.gui.editor.tips.select_image", LDLib2.getAssetsDir(), true, Dialog.suffixFilter(".png"), r -> {
                                if (r != null && r.isFile()) {
                                    var location = IGuiTexture.getTextureFromFile(r);
                                    if (location == null) return;
                                    setImageLocation(location);
                                    var size = getImageSize();
                                    setSprite(0, 0, size.getWidth(), size.getHeight());
                                    configurator.notifyChanges();
                                }
                            }).show(e.currentElement.getModularUI());
                        }).layout(layout -> layout.alignSelf(AlignItems.CENTER))
                ));
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawRawTextureGuides(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        SpriteTexture.of(imageLocation.toString()).draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
        // draw border guides
        var imageSize = getImageSize();
        var spriteSize = this.spriteSize;
        // if the sprite size is not set, use the image size
        if (spriteSize.getWidth() <= 0 || spriteSize.getHeight() <= 0) {
            spriteSize = imageSize;
        }
        // draw sprite box
        var spriteX = x + spritePosition.x * width / imageSize.width;
        var spriteY = y + spritePosition.y * height / imageSize.height;
        var spriteWidth = spriteSize.width * width / imageSize.width;
        var spriteHeight = spriteSize.height * height / imageSize.height;
        new ColorBorderTexture(1,0xFFFF0000).draw(graphics, mouseX, mouseY,
                spriteX, spriteY, spriteWidth, spriteHeight, partialTicks);
        // left
        DrawerHelper.drawSolidRect(graphics,
                spriteX + borderLT.getX() * width / imageSize.width,
                spriteY,
                1,
                spriteHeight, 0xFFFF0000);
        // top
        DrawerHelper.drawSolidRect(graphics,
                spriteX,
                spriteY + borderLT.getY() * height / imageSize.height,
                spriteWidth,
                1, 0xFFFF0000);
        // right
        DrawerHelper.drawSolidRect(graphics,
                spriteX + spriteWidth - borderRB.getX() * width / imageSize.width,
                spriteY,
                1,
                spriteHeight, 0xFFFF0000);
        // bottom
        DrawerHelper.drawSolidRect(graphics,
                spriteX,
                spriteY + spriteHeight - borderRB.getY() * height / imageSize.height,
                spriteWidth,
                1, 0xFFFF0000);
    }
}
