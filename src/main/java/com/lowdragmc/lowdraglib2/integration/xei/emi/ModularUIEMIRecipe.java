package com.lowdragmc.lowdraglib2.integration.xei.emi;

import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeIngredientHandler;
import com.lowdragmc.lowdraglib2.integration.xei.emi.handler.EMIRecipeWidgetHandler;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ModularUIEMIRecipe implements EmiRecipe {
    public final IModularUIProvider<ModularUIEMIRecipe> uiProvider;
    // runtime
    protected final LoadingCache<ModularUIEMIRecipe, ModularUI> uiCache;
    protected final Supplier<EMIRecipeIngredientHandler> ingredientCache;

    public ModularUIEMIRecipe(IModularUIProvider<ModularUIEMIRecipe> provider) {
        this.uiProvider = provider;
        this.uiCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10)
                .removalListener((RemovalNotification<ModularUIEMIRecipe, ModularUI> notification) -> {
                    var value = notification.getValue();
                    if (value != null) {
                        value.onRemoved();
                    }
                })
                .build(new CacheLoader<>() {
                    @Override
                    public ModularUI load(ModularUIEMIRecipe key) {
                        var mui = uiProvider.createModularUI(key);
                        mui.setTickWhileRending(true);
                        mui.setAllowDebugMode(false);
                        mui.setDrawTooltips(false);
                        mui.init(getDisplayWidth(), getDisplayHeight());
                        return mui;
                    }
                });
        this.ingredientCache = Suppliers.memoizeWithExpiration(() -> {var recipeIngredient = new EMIRecipeIngredientHandler();
            var mui = getModularUI();
            var event = UIEvent.create(EMIUIEvents.RECIPE_INGREDIENT);
            event.target = mui.ui.rootElement;
            event.customData = recipeIngredient;
            UIEventDispatcher.dispatchAllChildren(event);
            return recipeIngredient;
        }, 10, TimeUnit.SECONDS);
    }

    public ModularUI getModularUI() {
        return uiCache.getUnchecked(this);
    }

    private EMIRecipeIngredientHandler getIngredients() {
        return ingredientCache.get();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return getIngredients().inputs;
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return getIngredients().catalysts;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return getIngredients().outputs;
    }

    @Override
    public void addWidgets(WidgetHolder widgetHolder) {
        var mui = getModularUI();
        var widget = new ModularUIEMIWidget(mui);

        var recipeSlot = new EMIRecipeWidgetHandler(widget::getLocalToWorld);
        var event = UIEvent.create(EMIUIEvents.RECIPE_WIDGET);
        event.target = mui.ui.rootElement;
        event.customData = recipeSlot;
        UIEventDispatcher.dispatchAllChildren(event);

        // append additional slots first, which should be handled first
        for (var slot : recipeSlot.slots) {
            widgetHolder.add(slot);
            if (slot instanceof SlotWidget slotWidget) {
                slotWidget.recipeContext(this);
            }
        }

        // add modular ui widget
        widgetHolder.add(widget);
    }
}
