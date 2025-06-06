package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.gui.ui.Dialog;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR;

/**
 * @author KilaBash
 * @date 2022/9/14
 * @implNote AnimationTexture
 */
@LDLRegisterClient(name = "animation_texture", registry = "ldlib:gui_texture")
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
        this("ldlib:textures/gui/particles.png");
        setCellSize(8).setAnimation(32,  44).setAnimation(1);
    }

    public AnimationTexture(String imageLocation) {
        this.imageLocation = ResourceLocation.parse(imageLocation);
    }

    public AnimationTexture(ResourceLocation imageLocation) {
        this.imageLocation = imageLocation;
    }

    public AnimationTexture copy() {
        return new AnimationTexture(imageLocation).setCellSize(cellSize).setAnimation(from, to).setAnimation(animation).setColor(color);
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
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        updateTick();
        float cell = 1f / this.cellSize;
        int X = currentFrame % cellSize;
        int Y = currentFrame / cellSize;

        float imageU = X * cell;
        float imageV = Y * cell;

        Tesselator tessellator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, imageLocation);
        var matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = tessellator.begin(VertexFormat.Mode.QUADS, POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, x, y + height, 0).setUv(imageU, imageV + cell).setColor(color);
        bufferbuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(imageU + cell, imageV + cell).setColor(color);
        bufferbuilder.addVertex(matrix4f, x + width, y, 0).setUv(imageU + cell, imageV).setColor(color);
        bufferbuilder.addVertex(matrix4f, x, y, 0).setUv(imageU, imageV).setColor(color);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
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
                                    layout.setAspectRatio(1.0f);
                                    layout.setWidthPercent(80);
                                    layout.setPadding(YogaEdge.ALL, 3);
                                    layout.setAlignSelf(YogaAlign.CENTER);
                                }).style(style -> style.backgroundTexture(Sprites.BORDER1_RT1))
                                .addChild(new UIElement().layout(layout -> {
                                    layout.setWidthPercent(100);
                                    layout.setHeightPercent(100);
                                }).style(style -> style.backgroundTexture(this::drawRawTextureGuides))),
                        // button to select image
                        new Button().setText("ldlib.gui.editor.tips.select_image").setOnClick(e -> {
                            var mui = e.currentElement.getModularUI();
                            if (mui == null) return;
                            Dialog.showFileDialog("ldlib.gui.editor.tips.select_image", LDLib.getAssetsDir(), true, Dialog.suffixFilter(".png"), r -> {
                                if (r != null && r.isFile()) {
                                    var location = getTextureFromFile(r);
                                    if (location == null) return;
                                    imageLocation = location;
                                    configurator.notifyChanges();
                                }
                            }).show(mui.ui.rootElement);
                        }).layout(layout -> layout.setAlignSelf(YogaAlign.CENTER))
                ));
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawRawTextureGuides(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
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
