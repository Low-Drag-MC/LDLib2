package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.shader.LDLibRenderTypes;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * @author KilaBash
 * @date 2022/9/14
 * @implNote AnimationTexture
 */
@KJSBindings
@LDLRegisterClient(name = "animation_texture", registry = "ldlib2:gui_texture")
public class AnimationTexture extends TransformTexture {

    @Configurable(name = "ldlib.gui.editor.name.resource")
    public ResourceLocation imageLocation;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_size")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    @Getter
    protected int cellSize;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_from")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int from;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_to")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int to;

    @Configurable(tips = "ldlib.gui.editor.tips.cell_animation")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Getter
    protected int animation;

    @Configurable
    @ConfigColor
    @Getter
    protected int color = -1;

    protected int currentFrame;

    protected int currentTime;
    private long lastTick;

    public AnimationTexture() {
        this("ldlib2:textures/gui/particles.png");
        setCellSize(8).setAnimation(32,  44).setAnimation(1);
    }

    public AnimationTexture(String imageLocation) {
        this.imageLocation = ResourceLocation.parse(imageLocation);
    }

    public AnimationTexture(ResourceLocation imageLocation) {
        this.imageLocation = imageLocation;
    }

    public AnimationTexture copy() {
        var copied = new AnimationTexture(imageLocation).setCellSize(cellSize).setAnimation(from, to).setAnimation(animation).setColor(color);
        copied.copyTransform(this);
        return copied;
    }

    public AnimationTexture setTexture(String imageLocation) {
        this.imageLocation = ResourceLocation.parse(imageLocation);
        return this;
    }

    public AnimationTexture setCellSize(int cellSize) {
        this.cellSize = cellSize;
        return this;
    }

    public AnimationTexture setAnimation(int from, int to) {
        this.currentFrame = from;
        this.from = from;
        this.to = to;
        return this;
    }

    public AnimationTexture setAnimation(int animation) {
        this.animation = animation;
        return this;
    }

    @Override
    public AnimationTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        if (Minecraft.getInstance().level != null) {
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == lastTick) return;
            lastTick = tick;
            if (currentTime >= animation) {
                currentTime = 0;
                currentFrame += 1;
            } else {
                currentTime++;
            }
            if (currentFrame > to) {
                currentFrame = from;
            } else if (currentFrame < from) {
                currentFrame = from;
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        updateTick();
        float cell = 1f / this.cellSize;
        int X = currentFrame % cellSize;
        int Y = currentFrame / cellSize;

        float imageU = X * cell;
        float imageV = Y * cell;

        var buffer = graphics.bufferSource().getBuffer(LDLibRenderTypes.guiTexture(imageLocation));
        RenderSystem.disableDepthTest();

        var matrix4f = graphics.pose().last().pose();
        buffer.addVertex(matrix4f, x, y + height, 0).setUv(imageU, imageV + cell).setColor(color);
        buffer.addVertex(matrix4f, x + width, y + height, 0).setUv(imageU + cell, imageV + cell).setColor(color);
        buffer.addVertex(matrix4f, x + width, y, 0).setUv(imageU + cell, imageV).setColor(color);
        buffer.addVertex(matrix4f, x, y, 0).setUv(imageU, imageV).setColor(color);
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
                                    imageLocation = location;
                                    configurator.notifyChanges();
                                }
                            }).show(e.currentElement.getModularUI());
                        }).layout(layout -> layout.alignSelf(AlignItems.CENTER))
                ));
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawRawTextureGuides(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        SpriteTexture.of(imageLocation.toString()).draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
        float cell = 1f / this.cellSize;
        int X = from % cellSize;
        int Y = from / cellSize;

        float imageU = X * cell;
        float imageV = Y * cell;

        new ColorBorderTexture(1, 0xff00ff00).draw(graphics, 0, 0,
                x + width * imageU, y + height * imageV,
                (width * (cell)), (height * (cell)), partialTicks);

        X = to % cellSize;
        Y = to / cellSize;

        imageU = X * cell;
        imageV = Y * cell;

        new ColorBorderTexture(1, 0xffff0000).draw(graphics, 0, 0,
                x + width * imageU, y + height * imageV,
                (width * (cell)), (height * (cell)), partialTicks);
    }
}
