package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.ui.utils.IModularUIProvider;
import com.lowdragmc.lowdraglib2.integration.xei.jei.ModularUIRecipeCategory;
import com.lowdragmc.lowdraglib2.test.TestItem;
import lombok.Getter;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

public class TestJEIPlugin {
    protected static final IRecipeType<TestRecipe> RECIPE_TYPE = IRecipeType.create(LDLib2.id("test_category"), TestRecipe.class);
    protected static final IRecipeType<JEISlotApiTestRecipe> SLOT_API_RECIPE_TYPE =
            IRecipeType.create(LDLib2.id("jei_slot_api_cases"), JEISlotApiTestRecipe.class);

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        var helpers = registration.getJeiHelpers();
        registration.addRecipeCategories(
                new TestRecipeCategory<>(helpers, RECIPE_TYPE, TestRecipe::createModularUI,
                        "Test Category", TestRecipe.WIDTH, TestRecipe.HEIGHT),
                new TestRecipeCategory<>(helpers, SLOT_API_RECIPE_TYPE, JEISlotApiTestRecipe::createModularUI,
                        "JEI Slot API Cases", JEISlotApiTestRecipe.WIDTH, JEISlotApiTestRecipe.HEIGHT)
        );
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(RECIPE_TYPE, List.of(new TestRecipe()));
        registration.addRecipes(SLOT_API_RECIPE_TYPE, List.of(JEISlotApiTestRecipe.values()));
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    private static class TestRecipeCategory<T> extends ModularUIRecipeCategory<T> {
        @Getter
        private final IDrawable icon;
        private final IRecipeType<T> recipeType;
        private final Component title;
        private final int width;
        private final int height;

        public TestRecipeCategory(IJeiHelpers helpers, IRecipeType<T> recipeType,
                                  IModularUIProvider<T> uiProvider, String title, int width, int height) {
            super(uiProvider);
            this.icon = helpers.getGuiHelper().createDrawableItemLike(TestItem.ITEM);
            this.recipeType = recipeType;
            this.title = Component.literal(title);
            this.width = width;
            this.height = height;
        }

        @Override
        public IRecipeType<T> getRecipeType() {
            return recipeType;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }
}
