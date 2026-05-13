package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTexturePreviewHelper;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.WeakHashMap;

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
    private Identifier imageLocation = LDLib2.id("textures/gui/icon.png");

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

    public static SpriteTexture of(Identifier imageLocation) {
        return new SpriteTexture().setImageLocation(imageLocation);
    }

    public static SpriteTexture of(String imageLocation) {
        return of(Identifier.parse(imageLocation));
    }

    @ConfigSetter(field = "imageLocation")
    public SpriteTexture setImageLocation(Identifier imageLocation) {
        this.imageLocation = imageLocation;
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
            return SpriteTextureInterpolation.of(copy(), spriteTexture.copy(), lerp);
        }
        return super.interpolate(other, lerp);
    }

    @Override
    public void createPreview(ConfiguratorGroup father) {
        SpriteTextureClientSupport.createPreview(this, father);
    }

    public static final class SpriteTextureClientSupport {
        private static final Map<SpriteTexture, Size> IMAGE_SIZE_CACHE = new WeakHashMap<>();

        private SpriteTextureClientSupport() {
        }

        public static Size getImageSize(SpriteTexture texture) {
            return IMAGE_SIZE_CACHE.computeIfAbsent(texture, SpriteTextureClientSupport::resolveImageSize);
        }

        public static void createPreview(SpriteTexture texture, ConfiguratorGroup father) {
            GuiTexturePreviewHelper.createPreview(texture, father);
            var configurator = new Configurator("ldlib.gui.editor.group.base_image");
            father.addConfigurators(configurator
                    .addChildren(
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
                                    }).style(style -> style.backgroundTexture(GuiTexture.of((context, x, y, width, height) ->
                                            drawRawTextureGuides(texture, context, x, y, width, height))))),
                            new Button().setText("ldlib.gui.editor.tips.select_image").setOnClick(e -> {
                                Dialog.showFileDialog("ldlib.gui.editor.tips.select_image", LDLib2.getAssetsDir(), true, Dialog.suffixFilter(".png"), r -> {
                                    if (r != null && r.isFile()) {
                                        var location = IGuiTexture.getTextureFromFile(r);
                                        if (location == null) {
                                            return;
                                        }
                                        texture.setImageLocation(location);
                                        IMAGE_SIZE_CACHE.remove(texture);
                                        var size = getImageSize(texture);
                                        texture.setSprite(0, 0, size.getWidth(), size.getHeight());
                                        configurator.notifyChanges();
                                    }
                                }).show(e.currentElement.getModularUI());
                            }).layout(layout -> layout.alignSelf(AlignItems.CENTER))
                    ));
        }

        public static void drawRawTextureGuides(SpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
            context.drawTexture(SpriteTexture.of(texture.getImageLocation()), x, y, width, height);
            var imageSize = getImageSize(texture);
            var spriteSize = texture.spriteSize;
            if (spriteSize.getWidth() <= 0 || spriteSize.getHeight() <= 0) {
                spriteSize = imageSize;
            }
            var spriteX = x + texture.spritePosition.x * width / imageSize.width;
            var spriteY = y + texture.spritePosition.y * height / imageSize.height;
            var spriteWidth = spriteSize.width * width / imageSize.width;
            var spriteHeight = spriteSize.height * height / imageSize.height;
            context.drawTexture(new ColorBorderTexture(1, 0xFFFF0000), spriteX, spriteY, spriteWidth, spriteHeight);
            DrawerHelperClient.drawSolidRect(context,
                    spriteX + texture.borderLT.getX() * width / imageSize.width,
                    spriteY,
                    1,
                    spriteHeight, 0xFFFF0000);
            DrawerHelperClient.drawSolidRect(context,
                    spriteX,
                    spriteY + texture.borderLT.getY() * height / imageSize.height,
                    spriteWidth,
                    1, 0xFFFF0000);
            DrawerHelperClient.drawSolidRect(context,
                    spriteX + spriteWidth - texture.borderRB.getX() * width / imageSize.width,
                    spriteY,
                    1,
                    spriteHeight, 0xFFFF0000);
            DrawerHelperClient.drawSolidRect(context,
                    spriteX,
                    spriteY + spriteHeight - texture.borderRB.getY() * height / imageSize.height,
                    spriteWidth,
                    1, 0xFFFF0000);
        }

        private static Size resolveImageSize(SpriteTexture texture) {
            try {
                return Minecraft.getInstance().getTextureManager().getTexture(texture.getImageLocation()) instanceof ITextureSize textureSize
                        ? Size.of(textureSize.ldlib2$getImageWidth(), textureSize.ldlib2$getImageHeight())
                        : Size.of(1, 1);
            } catch (Exception e) {
                return Size.of(1, 1);
            }
        }
    }

    @LDLRegisterClient(name = "sprite_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredSpriteTextureRenderer implements RegisteredGuiTextureRenderer<SpriteTexture, RegisteredSpriteTextureRenderer> {
        @Override
        public Class<SpriteTexture> type() {
            return SpriteTexture.class;
        }

        @Override
        public void draw(SpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(SpriteTexture texture, GUIContext context, float x, float y, float width, float height) {
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
            if (texture.wrapMode == WrapMode.CLAMP) {
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
                    if (texture.wrapMode == WrapMode.MIRRORED_REPEAT) {
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
}
