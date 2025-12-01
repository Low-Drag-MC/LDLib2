package com.lowdragmc.lowdraglib2.integration.jei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@JeiPlugin
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LDLibJEIPlugin implements IModPlugin {
    @Nullable
    public static IJeiRuntime jeiRuntime;

    public static Rect2i getArea(UIElement element) {
        return getArea(element, false);
    }

    public static Rect2i getArea(UIElement element, boolean content) {
        if (content) {
            return new Rect2i((int) element.getContentX(), (int) element.getContentY(), (int) element.getContentWidth(), (int) element.getContentHeight());
        } else {
            return new Rect2i((int) element.getPositionX(), (int) element.getPositionY(), (int) element.getSizeWidth(), (int) element.getSizeHeight());
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return LDLib2.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        LDLibJEIPlugin.jeiRuntime = jeiRuntime;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(ModularUIScreen.class, ModularUIJEIHandlers.GHOST_INGREDIENT_HANDLER);
        registration.addGhostIngredientHandler(ModularUIContainerScreen.class, ModularUIJEIHandlers.GHOST_INGREDIENT_HANDLER);
        registration.addGenericGuiContainerHandler(AbstractContainerScreen.class, ModularUIJEIHandlers.GUI_CONTAINER_HANDLER);
    }

    //    @Nullable
//    public static Object getItemIngredient(ItemStack itemStack, int x, int y, int width, int height) {
//        IIngredientManager ingredientManager = jeiHelpers.getIngredientManager();
//        return ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack)
//                .map(typedIngredient -> new ClickableIngredient<>(typedIngredient, x, y, width, height))
//                .orElse(null);
//    }
//
//    public static boolean isJeiEnabled() {
//        return jeiRuntime != null && jeiRuntime.getIngredientListOverlay().isListDisplayed();
//    }
//
//    @Override
//    public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
//        if (LDLib2.isReiLoaded() || LDLib2.isEmiLoaded()) return;
//        registration.addGhostIngredientHandler(ModularUIGuiContainer.class, modularUIGuiHandler);
//        registration.addGenericGuiContainerHandler(ModularUIGuiContainer.class, modularUIGuiHandler);
//    }
//
//    @Override
//    public void registerCategories(IRecipeCategoryRegistration registration) {
//        JEIPlugin.jeiHelpers = registration.getJeiHelpers();
//        if (Platform.isDevEnv()) {
//            TestJEIPlugin.registerCategories(registration);
//        }
//    }
//
//    @Override
//    public void registerRecipes(IRecipeRegistration registration) {
//        if (Platform.isDevEnv()) {
//            TestJEIPlugin.registerRecipes(registration);
//        }
//    }
}
