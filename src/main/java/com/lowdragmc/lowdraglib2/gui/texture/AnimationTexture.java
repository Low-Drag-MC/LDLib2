package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
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
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * @author KilaBash
 * @date 2022/9/14
 * @implNote AnimationTexture
 */
@KJSBindings
@LDLRegisterClient(name = "animation_texture", registry = "ldlib2:gui_texture")
public class AnimationTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.resource")
    public Identifier imageLocation;

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
    protected long lastTick;

    public AnimationTexture() {
        this("ldlib2:textures/gui/particles.png");
        setCellSize(8).setAnimation(32, 44).setAnimation(1);
    }

    public AnimationTexture(String imageLocation) {
        this.imageLocation = Identifier.parse(imageLocation);
    }

    public AnimationTexture(Identifier imageLocation) {
        this.imageLocation = imageLocation;
    }

    @Override
    public AnimationTexture copy() {
        var copied = new AnimationTexture(imageLocation).setCellSize(cellSize).setAnimation(from, to).setAnimation(animation).setColor(color);
        copied.copyTransform(this);
        return copied;
    }

    public AnimationTexture setTexture(String imageLocation) {
        this.imageLocation = Identifier.parse(imageLocation);
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

    @Override
    public void createPreview(ConfiguratorGroup father) {
        AnimationTextureClientSupport.createPreview(this, father);
    }

    public static final class AnimationTextureClientSupport {
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

    @LDLRegisterClient(name = "animation_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredAnimationTextureRenderer implements RegisteredGuiTextureRenderer<AnimationTexture, RegisteredAnimationTextureRenderer> {
        @Override
        public Class<AnimationTexture> type() {
            return AnimationTexture.class;
        }

        @Override
        public void draw(AnimationTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(AnimationTexture texture, GUIContext context, float x, float y, float width, float height) {
            AnimationTextureClientSupport.updateTick(texture);
            float cell = 1f / texture.getCellSize();
            int frameX = texture.currentFrame % texture.getCellSize();
            int frameY = texture.currentFrame / texture.getCellSize();
            float imageU = frameX * cell;
            float imageV = frameY * cell;

            context.blit(RenderPipelines.GUI_TEXTURED, texture.imageLocation, x, y,
                    width, height, imageU, imageV, imageU + cell, imageV + cell,
                    texture.getColor());
        }
    }
}
