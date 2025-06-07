package com.lowdragmc.lowdraglib2.integration.emi;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.tooltip.IngredientTooltipComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ListEmiIngredient implements EmiIngredient {
    private final List<? extends EmiIngredient> ingredients;
    private final Supplier<EmiIngredient> currentSupplier;
    private final List<EmiStack> fullList;
    private long amount;
    private float chance = 1.0F;

    public ListEmiIngredient(List<? extends EmiIngredient> ingredients, Supplier<EmiIngredient> currentSupplier, long amount) {
        this.ingredients = ingredients;
        this.fullList = ingredients.stream().flatMap((i) -> i.getEmiStacks().stream()).toList();
        this.currentSupplier = currentSupplier;
        if (this.fullList.isEmpty()) {
            throw new IllegalArgumentException("ListEmiIngredient cannot be empty");
        } else {
            this.amount = amount;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof ListEmiIngredient other) {
            return other.getEmiStacks().equals(this.getEmiStacks());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return this.fullList.hashCode();
    }

    public EmiIngredient copy() {
        return EmiIngredient.of(this.ingredients, this.amount).setChance(this.chance);
    }

    public String toString() {
        return "Ingredient" + this.getEmiStacks();
    }

    public List<EmiStack> getEmiStacks() {
        return this.fullList;
    }

    public long getAmount() {
        return this.amount;
    }

    public EmiIngredient setAmount(long amount) {
        this.amount = amount;
        return this;
    }

    public float getChance() {
        return this.chance;
    }

    public EmiIngredient setChance(float chance) {
        this.chance = chance;
        return this;
    }

    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        EmiIngredient current = currentSupplier.get();
        if ((flags & RENDER_ICON) != 0) {
            current.render(draw, x, y, delta, -3);
        }

        if ((flags & RENDER_AMOUNT) != 0) {
            current.copy().setAmount(this.amount).render(draw, x, y, delta, 2);
        }

        if ((flags & RENDER_INGREDIENT) != 0) {
            EmiRender.renderIngredientIcon(this, draw, x, y);
        }
    }

    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tooltip.add(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.translatable("tooltip.emi.accepts"))));
        tooltip.add(new IngredientTooltipComponent(this.ingredients));
        tooltip.addAll(currentSupplier.get().getTooltip());
        return tooltip;
    }
}
