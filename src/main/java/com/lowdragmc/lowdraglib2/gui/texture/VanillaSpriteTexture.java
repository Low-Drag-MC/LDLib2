package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSearch;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.SearchComponentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/**
 * The sprite ResourceLocation should point to a registered vanilla sprite
 * (e.g. from a GUI atlas), not a raw texture file.
 */
@KJSBindings
@LDLRegisterClient(name = "vanilla_sprite_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class VanillaSpriteTexture extends TransformTexture {

    @Configurable(name = "ldlib.gui.editor.name.resource")
    @ConfigSearch(searchConfiguratorMethod = "searchSprites")
    @Getter
    @Setter
    private ResourceLocation sprite = ResourceLocation.withDefaultNamespace("toast/recipe_book");

    @Configurable
    @ConfigColor
    @Getter
    @Setter
    private int color = -1;

    public VanillaSpriteTexture() {
    }

    public VanillaSpriteTexture(ResourceLocation sprite) {
        this.sprite = sprite;
    }

    @HideFromJS
    public static VanillaSpriteTexture of(ResourceLocation sprite) {
        return new VanillaSpriteTexture(sprite);
    }

    @HideFromJS
    public static VanillaSpriteTexture of(String sprite) {
        return of(ResourceLocation.parse(sprite));
    }

    @RemapForJS("of")
    public static VanillaSpriteTexture kjs$of(ResourceLocation sprite) {
        return of(sprite);
    }

    @Override
    public VanillaSpriteTexture copy() {
        var copied = new VanillaSpriteTexture(sprite);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        if (sprite == null || width <= 0 || height <= 0) {
            return;
        }
        var spriteManager = Minecraft.getInstance().getGuiSprites();
        TextureAtlasSprite atlasSprite = spriteManager.getSprite(sprite);
        GuiSpriteScaling scaling = spriteManager.getSpriteScaling(atlasSprite);
        if (scaling instanceof GuiSpriteScaling.Stretch) {
            blitSpriteFloat(graphics, atlasSprite, x, y, width, height);
        } else if (scaling instanceof GuiSpriteScaling.Tile tile) {
            blitTiledSpriteFloat(graphics, atlasSprite, x, y, width, height,
                    0, 0, tile.width(), tile.height(), tile.width(), tile.height());
        } else if (scaling instanceof GuiSpriteScaling.NineSlice nineSlice) {
            blitNineSlicedSpriteFloat(graphics, atlasSprite, nineSlice, x, y, width, height);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private SearchComponentConfigurator.ISearchConfigurator<ResourceLocation> searchSprites() {
        return new SearchComponentConfigurator.ISearchConfigurator<>() {
            @Override
            @NotNull
            public ResourceLocation defaultValue() {
                return ResourceLocation.withDefaultNamespace("toast/recipe_book");
            }

            @Override
            public void search(String word, IResultHandler<ResourceLocation> searchHandler) {
                var lowerWord = word.toLowerCase();
                var atlas = Minecraft.getInstance().getGuiSprites().textureAtlas;
                for (var key : atlas.getTextures().keySet()) {
                    if (Thread.currentThread().isInterrupted()) return;
                    if (key.toString().toLowerCase().contains(lowerWord)) {
                        searchHandler.acceptResult(key);
                    }
                }
            }

            @Override
            @NotNull
            public String resultText(@NotNull ResourceLocation value) {
                return value.toString();
            }

            @Override
            public UIElementProvider<ResourceLocation> candidateUIProvider() {
                return UIElementProvider.iconText(VanillaSpriteTexture::of, res -> Component.literal(res.toString()));
            }
        };
    }

    /**
     * Blit a full sprite with float coordinates (stretch mode).
     */
    @OnlyIn(Dist.CLIENT)
    private void blitSpriteFloat(GuiGraphics graphics, TextureAtlasSprite atlasSprite, float x, float y, float width, float height) {
        innerBlitFloat(graphics, atlasSprite,
                x, x + width, y, y + height,
                atlasSprite.getU0(), atlasSprite.getU1(), atlasSprite.getV0(), atlasSprite.getV1());
    }

    /**
     * Blit a sub-region of a sprite with float coordinates.
     */
    @OnlyIn(Dist.CLIENT)
    private void blitSubSpriteFloat(GuiGraphics graphics, TextureAtlasSprite sprite,
                                    int textureWidth, int textureHeight,
                                    int uPosition, int vPosition,
                                    float x, float y, float uWidth, float vHeight) {
        if (uWidth == 0 || vHeight == 0) return;
        innerBlitFloat(graphics, sprite,
                x, x + uWidth, y, y + vHeight,
                sprite.getU((float) uPosition / textureWidth),
                sprite.getU((uPosition + uWidth) / textureWidth),
                sprite.getV((float) vPosition / textureHeight),
                sprite.getV((vPosition + vHeight) / textureHeight));
    }

    /**
     * Core float quad rendering.
     */
    @OnlyIn(Dist.CLIENT)
    private void innerBlitFloat(GuiGraphics graphics, TextureAtlasSprite sprite,
                                float x1, float x2, float y1, float y2,
                                float u0, float u1, float v0, float v1) {
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        var matrix = graphics.pose().last().pose();
        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(sprite.atlasLocation()));
        buffer.addVertex(matrix, x1, y1, 0).setUv(u0, v0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y2, 0).setUv(u0, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, 0).setUv(u1, v1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y1, 0).setUv(u1, v0).setColor(r, g, b, a);
    }

    @OnlyIn(Dist.CLIENT)
    private void blitTiledSpriteFloat(GuiGraphics graphics, TextureAtlasSprite sprite,
                                      float x, float y, float width, float height,
                                      int uPosition, int vPosition,
                                      int spriteWidth, int spriteHeight,
                                      int nineSliceWidth, int nineSliceHeight) {
        if (width <= 0 || height <= 0 || spriteWidth <= 0 || spriteHeight <= 0) return;
        for (float i = 0; i < width; i += spriteWidth) {
            float j = Math.min(spriteWidth, width - i);
            for (float k = 0; k < height; k += spriteHeight) {
                float l = Math.min(spriteHeight, height - k);
                blitSubSpriteFloat(graphics, sprite, nineSliceWidth, nineSliceHeight,
                        uPosition, vPosition, x + i, y + k, j, l);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void blitNineSlicedSpriteFloat(GuiGraphics graphics, TextureAtlasSprite sprite,
                                           GuiSpriteScaling.NineSlice nineSlice,
                                           float x, float y, float width, float height) {
        var border = nineSlice.border();
        float left = Math.min(border.left(), width / 2);
        float right = Math.min(border.right(), width / 2);
        float top = Math.min(border.top(), height / 2);
        float bottom = Math.min(border.bottom(), height / 2);
        int nsW = nineSlice.width();
        int nsH = nineSlice.height();

        if (width == nsW && height == nsH) {
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, 0, x, y, width, height);
            return;
        }

        float centerW = width - left - right;
        float centerH = height - top - bottom;
        int bLeft = border.left();
        int bRight = border.right();
        int bTop = border.top();
        int bBottom = border.bottom();

        // corners
        if (left > 0 && top > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, 0, x, y, left, top);
        if (right > 0 && top > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, nsW - bRight, 0, x + width - right, y, right, top);
        if (left > 0 && bottom > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, 0, nsH - bBottom, x, y + height - bottom, left, bottom);
        if (right > 0 && bottom > 0)
            blitSubSpriteFloat(graphics, sprite, nsW, nsH, nsW - bRight, nsH - bBottom, x + width - right, y + height - bottom, right, bottom);

        // edges
        if (centerW > 0 && top > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y, centerW, top,
                    bLeft, 0, nsW - bLeft - bRight, bTop, nsW, nsH);
        if (centerW > 0 && bottom > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y + height - bottom, centerW, bottom,
                    bLeft, nsH - bBottom, nsW - bLeft - bRight, bBottom, nsW, nsH);
        if (left > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x, y + top, left, centerH,
                    0, bTop, bLeft, nsH - bTop - bBottom, nsW, nsH);
        if (right > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x + width - right, y + top, right, centerH,
                    nsW - bRight, bTop, bRight, nsH - bTop - bBottom, nsW, nsH);

        // center
        if (centerW > 0 && centerH > 0)
            blitTiledSpriteFloat(graphics, sprite, x + left, y + top, centerW, centerH,
                    bLeft, bTop, nsW - bLeft - bRight, nsH - bTop - bBottom, nsW, nsH);
    }
}
