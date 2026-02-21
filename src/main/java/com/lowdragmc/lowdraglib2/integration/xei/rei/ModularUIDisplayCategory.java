package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEventDispatcher;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIRecipeIngredientHandler;
import com.lowdragmc.lowdraglib2.integration.xei.rei.handler.REIRecipeWidgetHandler;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ModularUIDisplayCategory<T extends ModularUIDisplay> implements DisplayCategory<T> {
    public final IModularUIProvider<T> uiProvider;
    // runtime
    protected final LoadingCache<T, ModularUI> uiCache;
    protected final LoadingCache<T, REIRecipeIngredientHandler> ingredientCache;

    protected ModularUIDisplayCategory(IModularUIProvider<T> provider) {
        this.uiProvider = provider;
        this.uiCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10)
                .removalListener((RemovalNotification<T, ModularUI> notification) -> {
                    var value = notification.getValue();
                    if (value != null) {
                        value.onRemoved();
                    }
                })
                .build(new CacheLoader<>() {
                    @Override
                    public ModularUI load(T display) {
                        var mui = uiProvider.createModularUI(display);
                        mui.setTickWhileRending(true);
                        mui.setAllowDebugMode(false);
                        mui.setDrawTooltips(false);
                        mui.init(getDisplayWidth(display), getDisplayHeight());
                        return mui;
                    }
                });
        this.ingredientCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .maximumSize(10)
                .build(new CacheLoader<>() {
                    @Override
                    public REIRecipeIngredientHandler load(T display) {
                        var mui = getUIForDisplay(display);
                        var recipeIngredient = new REIRecipeIngredientHandler();
                        var event = UIEvent.create(REIUIEvents.RECIPE_INGREDIENT);
                        event.target = mui.ui.rootElement;
                        event.customData = recipeIngredient;
                        UIEventDispatcher.dispatchAllChildren(event);
                        return recipeIngredient;
                    }
                });
    }

    public ModularUI getUIForDisplay(T recipe) {
        return uiCache.getUnchecked(recipe);
    }

    public REIRecipeIngredientHandler getIngredients(T recipe) {
        return ingredientCache.getUnchecked(recipe);
    }

    @Override
    public List<Widget> setupDisplay(T display, Rectangle bounds) {
        var widgets = new ArrayList<Widget>();
        var mui = getUIForDisplay(display);
        var widget = new ModularUIREIWidget(mui, bounds);

        // additional widgets
        var widgetHandler = new REIRecipeWidgetHandler(bounds, widget::getLocalToWorld);
        var event = UIEvent.create(REIUIEvents.RECIPE_WIDGET);
        event.target = mui.ui.rootElement;
        event.customData = widgetHandler;
        UIEventDispatcher.dispatchAllChildren(event);

        // we add slots first to make sure their events are handled first.
        widgets.addAll(widgetHandler.slots);
        widgets.add(widget);
        return widgets;
    }
}
