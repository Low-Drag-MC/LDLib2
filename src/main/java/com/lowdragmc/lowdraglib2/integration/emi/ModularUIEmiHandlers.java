package com.lowdragmc.lowdraglib2.integration.emi;

import com.lowdragmc.lowdraglib2.gui.ingredient.Target;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUIGuiContainer;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;

import java.util.List;

/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote ModularUIEmiHandlers
 */
public final class ModularUIEmiHandlers {
    public static final EmiDragDropHandler<ModularUIGuiContainer> DRAG_DROP_HANDLER = (screen, stack, x, y) -> {
        var stacks = stack.getEmiStacks();
        if (stacks.isEmpty()) return false;
        for (EmiStack emiStack : stacks) {
            List<Target> targets = screen.modularUI.mainGroup.getPhantomTargets(emiStack);
            if (targets.isEmpty()) continue;
            for (Target target : targets) {
                var area = target.getArea();
                if (area.contains(x, y)) {
                    target.accept(emiStack);
                    return true;
                }
            }
        }
        return false;
    };
    public static final EmiExclusionArea<ModularUIGuiContainer> EXCLUSION_AREA = (screen, consumer) -> screen.getGuiExtraAreas().stream().map(rect2 -> new Bounds(rect2.getX(), rect2.getY(), rect2.getWidth(), rect2.getHeight())).forEach(consumer);
    public static final EmiStackProvider<ModularUIGuiContainer> STACK_PROVIDER = (screen, x, y) -> {
        var target = screen.modularUI.mainGroup.getXEIIngredientOverMouse(x, y);
        if (target instanceof EmiStackInteraction entryStack) {
            return entryStack;
        } else if (target instanceof EmiIngredient entryStacks) {
            return new EmiStackInteraction(entryStacks, null, false);
        }
        return EmiStackInteraction.EMPTY;
    };
}
