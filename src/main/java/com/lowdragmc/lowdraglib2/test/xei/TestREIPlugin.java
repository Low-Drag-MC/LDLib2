package com.lowdragmc.lowdraglib2.test.xei;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplay;
import com.lowdragmc.lowdraglib2.integration.xei.rei.ModularUIDisplayCategory;
import com.lowdragmc.lowdraglib2.test.TestItem;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

import javax.swing.*;

import static com.lowdragmc.lowdraglib2.test.xei.TestREIPlugin.TestREIRecipeCategory.IDENTIFIER;

public class TestREIPlugin {
    public static void registerCategories(CategoryRegistry registry) {
        registry.add(new TestREIRecipeCategory());
        registry.addWorkstations(IDENTIFIER, EntryStacks.of(TestItem.ITEM));
    }

    public static void registerDisplays(DisplayRegistry registry) {
        registry.add(new TestREIRecipeDisplay());
    }

    public static class TestREIRecipeCategory extends ModularUIDisplayCategory<TestREIRecipeDisplay> {
        public static final CategoryIdentifier<TestREIRecipeDisplay> IDENTIFIER = CategoryIdentifier.of(LDLib2.MOD_ID, "test_category");

        @Getter
        Renderer icon;

        public TestREIRecipeCategory() {
            super(display -> new TestRecipe().createModularUI());
            icon = EntryStacks.of(TestItem.ITEM);
        }

        @Override
        public CategoryIdentifier<TestREIRecipeDisplay> getCategoryIdentifier() {
            return IDENTIFIER;
        }

        @Override
        public Component getTitle() {
            return Component.literal("Test Category");
        }

        @Override
        public int getDisplayWidth(TestREIRecipeDisplay display) {
            return TestRecipe.WIDTH;
        }

        @Override
        public int getDisplayHeight() {
            return TestRecipe.HEIGHT;
        }
    }

    public static class TestREIRecipeDisplay implements ModularUIDisplay {
        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return IDENTIFIER;
        }
    }
}
