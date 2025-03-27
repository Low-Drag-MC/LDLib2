package com.lowdragmc.lowdraglib.integration.jei;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class IRecipeIngredientSlotWrapper implements IDrawable {
    public IRecipeIngredientSlot slot;

    public IRecipeIngredientSlotWrapper(IRecipeIngredientSlot slot) {
        this.slot = slot;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {

    }
}
