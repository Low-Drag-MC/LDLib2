package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ITextureSize;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.WeakHashMap;

public final class SpriteTextureClientSupport {
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
