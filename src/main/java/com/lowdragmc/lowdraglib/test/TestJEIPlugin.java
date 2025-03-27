package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.integration.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib.integration.jei.ModularWrapper;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class TestJEIPlugin {
    public static void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TestJEIRecipeCategory(registration.getJeiHelpers()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TestJEIRecipeCategory.RECIPE_TYPE, List.of(new TestJEIRecipe()));
    }

    private static class TestJEIRecipeCategory extends ModularUIRecipeCategory<TestJEIRecipe> {
        private static RecipeType<TestJEIRecipe> RECIPE_TYPE = new RecipeType<>(LDLib.location("test_category"), TestJEIRecipe.class);

        @Getter
        private final IDrawable background;
        @Getter
        private final IDrawable icon;

        public TestJEIRecipeCategory(IJeiHelpers helpers) {
            this.background = helpers.getGuiHelper().createBlankDrawable(170, 60);
            this.icon = helpers.getGuiHelper().createDrawableItemStack(new ItemStack(Items.APPLE));
        }

        @Override
        public RecipeType<TestJEIRecipe> getRecipeType() {
            return RECIPE_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Test Category");
        }

    }

    private static class TestJEIRecipe extends ModularWrapper<WidgetGroup> {
        public TestJEIRecipe() {
            super(new TestXEIWidgetGroup());
        }
    }
}
