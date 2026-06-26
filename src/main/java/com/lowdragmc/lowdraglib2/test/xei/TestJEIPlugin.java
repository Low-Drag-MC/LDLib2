package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib2.test.TestItem;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class TestJEIPlugin {
    protected static final IRecipeType<TestRecipe> RECIPE_TYPE = IRecipeType.create(LDLib2.id("test_category"), TestRecipe.class);

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TestModularUIRecipeCategory(registration.getJeiHelpers()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RECIPE_TYPE, List.of(new TestRecipe()));
    }

    @ParametersAreNonnullByDefault
    private static class TestModularUIRecipeCategory extends ModularUIRecipeCategory<TestRecipe> {
        @Getter
        private final IDrawable icon;

        public TestModularUIRecipeCategory(IJeiHelpers helpers) {
            super(TestRecipe::createModularUI);
            this.icon = helpers.getGuiHelper().createDrawableItemLike(TestItem.ITEM);
        }

        @Override
        public IRecipeType<TestRecipe> getRecipeType() {
            return RECIPE_TYPE;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Test Category");
        }

        @Override
        public int getWidth() {
            return TestRecipe.WIDTH;
        }

        @Override
        public int getHeight() {
            return TestRecipe.HEIGHT;
        }
    }
}
