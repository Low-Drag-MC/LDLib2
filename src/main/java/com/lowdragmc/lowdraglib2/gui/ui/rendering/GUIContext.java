package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class GUIContext {
    @OnlyIn(Dist.CLIENT)
    public ModularUI modularUI;
    @OnlyIn(Dist.CLIENT)
    public GuiGraphics graphics;
    @OnlyIn(Dist.CLIENT)
    public int mouseX, mouseY;
    @OnlyIn(Dist.CLIENT)
    public float partialTick;
    @OnlyIn(Dist.CLIENT)
    public PoseStack pose;

    @OnlyIn(Dist.CLIENT)
    public void drawTexture(IGuiTexture texture, float x, float y, float width, float height) {
        texture.draw(graphics, mouseX, mouseY, x, y, width, height, partialTick);
    }

    @OnlyIn(Dist.CLIENT)
    public void enableScissor(float x, float y, float width, float height) {
        enableScissor(x, y, width, height, graphics.pose().last().pose());
    }

    @OnlyIn(Dist.CLIENT)
    public void enableScissor(float x, float y, float width, float height, Matrix4f trans) {
        var realPos = trans.transform(new Vector4f(x, y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
    }

    @OnlyIn(Dist.CLIENT)
    public void disableScissor() {
        graphics.disableScissor();
    }
}
