package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;

@LDLRegister(name = "group_texture", registry = "ldlib:gui_texture")
public class GuiTextureGroup extends TransformTexture {

    @Configurable(collapse = false)
    public IGuiTexture[] textures;

    public GuiTextureGroup() {
        this(ResourceBorderTexture.BORDERED_BACKGROUND, new ResourceTexture());
    }

    public GuiTextureGroup(IGuiTexture... textures) {
        this.textures = textures;
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
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        for (IGuiTexture texture : textures) {
            texture.draw(graphics, mouseX,mouseY,  x, y, width, height);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        for (IGuiTexture texture : textures) {
            texture.updateTick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawSubAreaInternal(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
        for (IGuiTexture texture : textures) {
            texture.drawSubArea(graphics, x, y, width, height, drawnU, drawnV, drawnWidth, drawnHeight);
        }
    }

    @Override
    public void setUIResource(Resource<IGuiTexture> texturesResource) {
        for (IGuiTexture texture : textures) {
            texture.setUIResource(texturesResource);
        }
    }
}
