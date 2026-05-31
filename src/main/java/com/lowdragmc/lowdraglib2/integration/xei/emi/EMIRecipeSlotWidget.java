package com.lowdragmc.lowdraglib2.integration.xei.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class EMIRecipeSlotWidget extends SlotWidget {
    public final Supplier<EmiIngredient> ingredientProvider;
    public final Supplier<Matrix4f> localToWorldSupplier;
    public final BiPredicate<Float, Float> isMouseOver;
    public final Supplier<Bounds> boundsProvider;

    public EMIRecipeSlotWidget(Supplier<EmiIngredient> ingredientProvider,
                               Supplier<Matrix4f> localToWorldSupplier,
                               BiPredicate<Float, Float> isMouseOver,
                               Supplier<Bounds> boundsProvider) {
        super(EmiStack.EMPTY, 0, 0);
        this.localToWorldSupplier = localToWorldSupplier;
        this.isMouseOver = isMouseOver;
        this.ingredientProvider = ingredientProvider;
        this.boundsProvider = boundsProvider;
    }

    public Vector2f getWorldMouse(float mouseX, float mouseY) {
        var realMouse = localToWorldSupplier.get().transformPosition(new Vector3f(0, 0, 0))
                .mul(-1)
                .add(mouseX, mouseY, 0);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    @Override
    public EmiIngredient getStack() {
        return ingredientProvider.get();
    }

    @Override
    public Bounds getBounds() {
        var bounds = boundsProvider.get();
        var transform = localToWorldSupplier.get();
        var pos = transform.transformPosition(new Vector3f(bounds.x(), bounds.y(), 0));
        var size = transform.transformDirection(new Vector3f(bounds.width(), bounds.height(), 0));
        return new Bounds((int) pos.x, (int) pos.y, (int) size.x, (int) size.y);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        var realMouse = getWorldMouse(mouseX, mouseY);
        if (!isMouseOver.test(realMouse.x, realMouse.y)) return List.of();
        return super.getTooltip(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        var realMouse = getWorldMouse(mouseX, mouseY);
        if (!isMouseOver.test(realMouse.x, realMouse.y)) return false;
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
