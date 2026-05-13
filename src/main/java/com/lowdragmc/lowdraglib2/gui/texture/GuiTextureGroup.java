package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.Getter;

@KJSBindings
@LDLRegisterClient(name = "group_texture", registry = "ldlib2:gui_texture")
public class GuiTextureGroup extends TransformTexture {
    @Configurable(collapse = false)
    @Getter
    private IGuiTexture[] textures;

    public GuiTextureGroup() {
        this(new ColorBorderTexture(1, -1), new SpriteTexture());
    }

    public GuiTextureGroup(IGuiTexture... textures) {
        this.textures = textures;
    }

    public static GuiTextureGroup of(IGuiTexture... textures) {
        return new GuiTextureGroup(textures);
    }

    public GuiTextureGroup setTextures(IGuiTexture... textures) {
        this.textures = textures;
        return this;
    }

    @Override
    public GuiTextureGroup setColor(int color) {
        var copiedTextures = new IGuiTexture[textures.length];
        for (int i = 0; i < textures.length; i++) {
            copiedTextures[i] = textures[i].copy().setColor(color);
        }
        var copied = new GuiTextureGroup(copiedTextures);
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public GuiTextureGroup copy() {
        var copied = new GuiTextureGroup(textures);
        copied.copyTransform(this);
        return copied;
    }

    @LDLRegisterClient(name = "group_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredGuiTextureGroupRenderer implements RegisteredGuiTextureRenderer<GuiTextureGroup, RegisteredGuiTextureGroupRenderer> {
        @Override
        public Class<GuiTextureGroup> type() {
            return GuiTextureGroup.class;
        }

        @Override
        public void draw(GuiTextureGroup texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(GuiTextureGroup texture, GUIContext context, float x, float y, float width, float height) {
            for (IGuiTexture child : texture.getTextures()) {
                context.drawTexture(child, x, y, width, height);
            }
        }
    }
}
