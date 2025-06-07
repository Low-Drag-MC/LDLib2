package com.lowdragmc.lowdraglib2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ingredient.IRecipeIngredientSlot;
import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;

public class RecipeIngredientSlotWidget extends SlotWidget {
    public final IRecipeIngredientSlot slot;

    public RecipeIngredientSlotWidget(IRecipeIngredientSlot slot) {
        super(getEmiIngredient(slot), slot.self().getPositionX(), slot.self().getPositionY());
        this.slot = slot;
        this.custom = true;
        this.customWidth = slot.self().getSizeWidth();
        this.customHeight = slot.self().getSizeHeight();
        this.drawBack(false);
    }

    static EmiIngredient getEmiIngredient(IRecipeIngredientSlot slot) {
        var list = slot.getXEIIngredients().stream()
                .filter(EmiIngredient.class::isInstance)
                .map(EmiIngredient.class::cast)
                .toList();
        if (list.isEmpty()) {
            return EmiStack.EMPTY;
        } else if (list.size() == 1) {
            return list.getFirst();
        } else {
            return new ListEmiIngredient(list,
                    () -> {
                        if (slot.getXEICurrentIngredient() instanceof EmiIngredient ingredient) {
                            return ingredient;
                        } else {
                            int index = (int)(System.currentTimeMillis() / 1000L % list.size());
                            return list.get(index);
                        }
                    },
                    list.getFirst().getAmount())
                    .setChance(list.getFirst().getChance());
        }
    }


    @Override
    public void drawStack(GuiGraphics draw, int mouseX, int mouseY, float delta) {
//        super.drawStack(draw, mouseX, mouseY, delta);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
        var tooltips = super.getTooltip(mouseX, mouseY);
        for (var component : slot.self().getTooltipTexts()) {
            tooltips.add(ClientTooltipComponent.create(EmiPort.ordered(component)));
        }
        return tooltips;
    }
}
