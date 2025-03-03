package com.lowdragmc.lowdraglib.emi;

import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.jei.IngredientIO;
import com.lowdragmc.lowdraglib.jei.ModularWrapper;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/4/3
 * @implNote ModularEmiRecipe
 */
public abstract class ModularEmiRecipe<T extends Widget> implements EmiRecipe {
    public static final List<ModularWrapper<?>> CACHE_OPENED = new ArrayList<>();

    protected Supplier<T> widget;
    @Getter
    protected List<EmiIngredient> inputs;
    @Getter
    protected List<EmiStack> outputs;
    @Getter
    protected List<EmiIngredient> catalysts;
    protected int width, height;

    public ModularEmiRecipe(Supplier<T> widgetSupplier) {
        this.widget = widgetSupplier;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.catalysts = new ArrayList<>();
        var widget = widgetSupplier.get();
        this.width = widget.getSize().width;
        this.height = widget.getSize().height;

        for (Widget w : getFlatWidgetCollection(widget)) {
            if (w instanceof IRecipeIngredientSlot slot) {
                var io = slot.getIngredientIO();
                for (Object ingredient : slot.getXEIIngredients()) {
                    if (ingredient instanceof EmiIngredient emiIngredient) {
                        if (io == IngredientIO.INPUT || io == IngredientIO.BOTH) {
                            inputs.add(emiIngredient);
                        }
                        if (io == IngredientIO.OUTPUT || io == IngredientIO.BOTH) {
                            outputs.add(emiIngredient.getEmiStacks().get(0));
                        }
                        if (io == IngredientIO.CATALYST) {
                            catalysts.add(emiIngredient);
                        }
                    }
                }
            }
        }
    }

    public List<Widget> getFlatWidgetCollection(T widgetIn) {
        List<Widget> widgetList = new ArrayList<>();
        if (widgetIn instanceof WidgetGroup group) {
            for (Widget widget : group.widgets) {
                widgetList.add(widget);
                if (widget instanceof WidgetGroup group1) {
                    widgetList.addAll(group1.getContainedWidgets(true));
                }
            }
        } else {
            widgetList.add(widgetIn);
        }
        return widgetList;
    }

    @Override
    public int getDisplayWidth() {
        return width;
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        var widget = this.widget.get();
        var modular = new ModularWrapper<>(widget);
        modular.setRecipeWidget(0, 0);

        synchronized (CACHE_OPENED) {
            CACHE_OPENED.add(modular);
        }
        List<dev.emi.emi.api.widget.Widget> slots = new ArrayList<>();
        for (Widget w : getFlatWidgetCollection(widget)) {
            if (w instanceof IRecipeIngredientSlot slot) {
                if (w.getParent() instanceof DraggableScrollableWidgetGroup draggable && draggable.isUseScissor()) {
                    // don't add the EMI widget at all if we have a draggable group, let the draggable widget handle it instead.
                    continue;
                }
                var io = slot.getIngredientIO();
                if (io != null && io != IngredientIO.RENDER_ONLY) {
                    //noinspection unchecked
                    var ingredients = EmiIngredient.of((List<? extends EmiIngredient>) (List<?>) slot.getXEIIngredients());

                    SlotWidget slotWidget = null;
                    // Clear the LDLib slots & add EMI slots based on them.
                    if (slot instanceof com.lowdragmc.lowdraglib.gui.widget.SlotWidget slotW) {
                        clearSlotWidgetHandler(slotW, 0);
                    } else if (slot instanceof com.lowdragmc.lowdraglib.gui.widget.TankWidget tankW) {
                        clearTankWidgetHandler(tankW);
                        long capacity = Math.max(1, ingredients.getAmount());
                        slotWidget = new TankWidget(ingredients, w.getPosition().x, w.getPosition().y, w.getSize().width, w.getSize().height, capacity);
                    }
                    if (slotWidget == null) {
                        slotWidget = new SlotWidget(ingredients, w.getPosition().x, w.getPosition().y);
                    }

                    slotWidget.customBackground(null, w.getPosition().x, w.getPosition().y, w.getSize().width, w.getSize().height)
                            .drawBack(false);
                    if (io == IngredientIO.CATALYST) {
                        slotWidget.catalyst(true);
                    } else if (io == IngredientIO.OUTPUT) {
                        slotWidget.recipeContext(this);
                    }
                    for (Component component : w.getTooltipTexts()) {
                        slotWidget.appendTooltip(component);
                    }
                    slots.add(slotWidget);
                }
            }
        }
        widgets.add(new ModularWrapperWidget(modular, slots));
        slots.forEach(widgets::add);
        widgets.add(new ModularForegroundRenderWidget(modular));
    }

    public void clearSlotWidgetHandler(com.lowdragmc.lowdraglib.gui.widget.SlotWidget slotW, int slotIndex) {
        slotW.setHandlerSlot((IItemHandlerModifiable) EmptyItemHandler.INSTANCE, slotIndex);
        slotW.setDrawHoverOverlay(false).setDrawHoverTips(false);
    }

    public void clearTankWidgetHandler(com.lowdragmc.lowdraglib.gui.widget.TankWidget tankW) {
        tankW.setFluidTank(EmptyFluidHandler.INSTANCE);
        tankW.setDrawHoverOverlay(false).setDrawHoverTips(false);
    }

    public static ModularWrapper<?> TEMP_CACHE = null;
    public void addTempWidgets(WidgetHolder widgets) {
        if (TEMP_CACHE != null) {
            TEMP_CACHE.modularUI.triggerCloseListeners();
            TEMP_CACHE = null;
        }
        var widget = this.widget.get();
        var modular = new ModularWrapper<>(widget);
        modular.setRecipeWidget(0, 0);
        widgets.add(new ModularWrapperWidget(modular, new ArrayList<>()));
        TEMP_CACHE = modular;
    }
}
