package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.xei.emi.ModularUIEMIRecipe;
import com.lowdragmc.lowdraglib2.test.TestItem;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

public class TestEMIPlugin {
    public static void register(EmiRegistry registry) {
        var category = new TestEmiRecipeCategory();
        registry.addCategory(category);
        registry.addRecipe(new TestEmiRecipe(category));
        registry.addWorkstation(category, EmiStack.of(TestItem.ITEM));
    }

    protected static class TestEmiRecipeCategory extends EmiRecipeCategory {
        public TestEmiRecipeCategory() {
            super(LDLib2.id("test_category"), EmiStack.of(TestItem.ITEM));
        }
    }

    protected static class TestEmiRecipe extends ModularUIEMIRecipe {
        @Getter
        protected TestEmiRecipeCategory category;

        public TestEmiRecipe(TestEmiRecipeCategory category) {
            super(recipe -> new TestRecipe().createModularUI());
            this.category = category;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return LDLib2.id("/test_recipe");
        }

        @Override
        public int getDisplayWidth() {
            return TestRecipe.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return TestRecipe.HEIGHT;
        }
    }
}
