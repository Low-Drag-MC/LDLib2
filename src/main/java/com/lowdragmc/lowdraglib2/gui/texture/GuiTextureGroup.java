package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;

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
        for (IGuiTexture texture : textures) {
            texture.setColor(color);
        }
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        for (IGuiTexture texture : textures) {
            texture.draw(graphics, mouseX,mouseY,  x, y, width, height, partialTicks);
        }
    }

    @Override
    public IGuiTexture copy() {
        return new GuiTextureGroup(textures);
    }
}
