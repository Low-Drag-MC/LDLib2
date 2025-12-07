package com.lowdragmc.lowdraglib2.integration.xei.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class EMIRecipeSlotWidget extends SlotWidget {
    public Supplier<EmiIngredient> ingredientProvider;
    public BiPredicate<Double, Double> isMouseOver;
    public Supplier<Bounds> boundsProvider;

    public EMIRecipeSlotWidget(Supplier<EmiIngredient> ingredientProvider,
                               BiPredicate<Double, Double> isMouseOver,
                               Supplier<Bounds> boundsProvider) {
        super(EmiStack.EMPTY, 0, 0);
        this.isMouseOver = isMouseOver;
        this.ingredientProvider = ingredientProvider;
        this.boundsProvider = boundsProvider;
    }

    @Override
    public EmiIngredient getStack() {
        return ingredientProvider.get();
    }

    @Override
    public Bounds getBounds() {
        return boundsProvider.get();
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        if (!isMouseOver.test((double) mouseX, (double) mouseY)) return List.of();
        return super.getTooltip(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver.test((double) mouseX, (double) mouseY)) return false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void drawStack(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        // do not draw stack yourself
    }

    @Override
    public void drawBackground(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        // do not draw background yourself
    }

    @Override
    public void drawOverlay(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        // do not draw overlay yourself
    }

    @Override
    public boolean shouldDrawSlotHighlight(int mouseX, int mouseY) {
        return false;
    }
}
