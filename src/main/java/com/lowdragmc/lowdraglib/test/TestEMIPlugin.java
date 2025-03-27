package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.integration.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class TestEMIPlugin {
    public static void register(EmiRegistry registry) {
        var category = new TestEmiRecipeCategory();
        registry.addCategory(category);
        registry.addRecipe(new TestEmiRecipe(category));
        registry.addRecipeHandler(null, new TestEmiRecipeHandler());
        registry.addWorkstation(category, EmiStack.of(TestItem.ITEM));
    }

    protected static class TestEmiRecipeCategory extends EmiRecipeCategory {
        public TestEmiRecipeCategory() {
            super(LDLib.location("modular_ui"), EmiStack.of(Items.APPLE));
        }
    }

    protected static class TestEmiRecipe extends ModularEmiRecipe<WidgetGroup> {
        @Getter
        TestEmiRecipeCategory category;
        public TestEmiRecipe(TestEmiRecipeCategory category) {
            super(TestXEIWidgetGroup::new);
            this.category = category;
        }

        @Override
        public @Nullable ResourceLocation getId() {
            return LDLib.location("test_recipe");
        }
    }
}
