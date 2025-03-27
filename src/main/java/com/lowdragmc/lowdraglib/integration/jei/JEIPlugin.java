package com.lowdragmc.lowdraglib.integration.jei;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.test.TestJEIPlugin;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/04/30
 * @implNote jei plugin
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {

    public static IJeiRuntime jeiRuntime;
    public static IJeiHelpers jeiHelpers;
    private static final ModularUIJeiHandler modularUIGuiHandler = new ModularUIJeiHandler();

    public JEIPlugin() {
        LDLib.LOGGER.debug("LDLib JEI Plugin created");
    }

    @Nullable
    public static Object getItemIngredient(ItemStack itemStack, int x, int y, int width, int height) {
        IIngredientManager ingredientManager = jeiHelpers.getIngredientManager();
        return ingredientManager.createTypedIngredient(VanillaTypes.ITEM_STACK, itemStack)
            .map(typedIngredient -> new ClickableIngredient<>(typedIngredient, x, y, width, height))
            .orElse(null);
    }

    public static boolean isJeiEnabled() {
        return jeiRuntime != null && jeiRuntime.getIngredientListOverlay().isListDisplayed();
    }

    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime) {
        JEIPlugin.jeiRuntime = jeiRuntime;
    }

    @Override
    public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
        if (LDLib.isReiLoaded() || LDLib.isEmiLoaded()) return;
        registration.addGhostIngredientHandler(ModularUIGuiContainer.class, modularUIGuiHandler);
        registration.addGenericGuiContainerHandler(ModularUIGuiContainer.class, modularUIGuiHandler);
    }

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(LDLib.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        JEIPlugin.jeiHelpers = registration.getJeiHelpers();
        if (Platform.isDevEnv()) {
            TestJEIPlugin.registerCategories(registration);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Platform.isDevEnv()) {
            TestJEIPlugin.registerRecipes(registration);
        }
    }
}
