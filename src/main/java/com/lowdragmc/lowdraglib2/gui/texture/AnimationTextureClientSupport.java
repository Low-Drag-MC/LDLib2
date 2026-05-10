package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.GuiTexturePreviewHelper;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import dev.vfyjxf.taffy.style.AlignItems;
import net.minecraft.client.Minecraft;

public final class AnimationTextureClientSupport {
    private AnimationTextureClientSupport() {
    }

    public static void updateTick(AnimationTexture texture) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        long tick = Minecraft.getInstance().level.getGameTime();
        if (tick == texture.lastTick) {
            return;
        }
        texture.lastTick = tick;
        if (texture.currentTime >= texture.getAnimation()) {
            texture.currentTime = 0;
            texture.currentFrame += 1;
        } else {
            texture.currentTime++;
        }
        if (texture.currentFrame > texture.getTo() || texture.currentFrame < texture.getFrom()) {
            texture.currentFrame = texture.getFrom();
        }
    }

    public static void createPreview(AnimationTexture texture, ConfiguratorGroup father) {
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
                                    texture.imageLocation = location;
                                    configurator.notifyChanges();
                                }
                            }).show(e.currentElement.getModularUI());
                        }).layout(layout -> layout.alignSelf(AlignItems.CENTER))
                ));
    }

    public static void drawRawTextureGuides(AnimationTexture texture, GUIContext context, float x, float y, float width, float height) {
        context.drawTexture(SpriteTexture.of(texture.imageLocation), x, y, width, height);
        float cell = 1f / texture.getCellSize();
        int frameX = texture.getFrom() % texture.getCellSize();
        int frameY = texture.getFrom() / texture.getCellSize();
        float imageU = frameX * cell;
        float imageV = frameY * cell;
        context.drawTexture(new ColorBorderTexture(1, 0xff00ff00),
                x + width * imageU, y + height * imageV,
                width * cell, height * cell);

        frameX = texture.getTo() % texture.getCellSize();
        frameY = texture.getTo() / texture.getCellSize();
        imageU = frameX * cell;
        imageV = frameY * cell;
        context.drawTexture(new ColorBorderTexture(1, 0xffff0000),
                x + width * imageU, y + height * imageV,
                width * cell, height * cell);
    }
}
