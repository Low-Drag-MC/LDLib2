package com.lowdragmc.lowdraglib.compass.component;

import com.lowdragmc.lowdraglib.compass.ILayoutComponent;
import com.lowdragmc.lowdraglib.compass.LayoutPageWidget;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.misc.CycleItemStackHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.w3c.dom.Element;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2022/10/21
 * @implNote RecipeComponent
 */
@NoArgsConstructor
public class RecipeComponent extends AbstractComponent {
    public static final ResourceTexture PROGRESS_BAR_ARROW = new ResourceTexture("ldlib:textures/gui/progress_bar_arrow.png");

    private static final List<RecipeViewCreator> RECIPE_VIEW_CREATORS = new ArrayList<>();

    public interface RecipeViewCreator extends Predicate<Recipe<?>> {
        ItemStack getWorkstation(RecipeHolder<?> recipe);

        WidgetGroup getViewWidget(RecipeHolder<?> recipe);
    }

    public static void registerRecipeViewCreator(RecipeViewCreator recipeViewCreator) {
        RECIPE_VIEW_CREATORS.add(recipeViewCreator);
    }

    @Nullable
    protected RecipeHolder<?> recipe;

    @Override
    public ILayoutComponent fromXml(Element element) {
        super.fromXml(element);
        if (element.hasAttribute("id")) {
            var recipeID = ResourceLocation.parse(element.getAttribute("id"));
            for (RecipeHolder<?> recipe : Minecraft.getInstance().getConnection().getRecipeManager().getRecipes()) {
                if (recipe.id().equals(recipeID)) {
                    this.recipe = recipe;
                    return this;
                }
            }
        }
        return this;
    }

    @Override
    protected LayoutPageWidget addWidgets(LayoutPageWidget currentPage) {
        if (recipe == null) return currentPage;
        Int2ObjectMap<Ingredient> inputs = new Int2ObjectArrayMap<>();
        var output = recipe.value().getResultItem(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var ingredients = recipe.value().getIngredients();

        if (recipe.value() instanceof ShapedRecipe shapedRecipe) {
            int w = shapedRecipe.getWidth();
            int h = shapedRecipe.getHeight();
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    inputs.put(i + j * w, ingredients.get(i + j * w));
                }
            }
        } else {
            for (int i = 0; i < ingredients.size(); i++) {
                inputs.put(i, ingredients.get(i));
            }
        }

        WidgetGroup recipeGroup = null;
        ItemStack workstation = ItemStack.EMPTY;
        for (RecipeViewCreator creator : RECIPE_VIEW_CREATORS) {
            if (creator.test(recipe.value())) {
                recipeGroup = creator.getViewWidget(recipe);
                workstation = creator.getWorkstation(recipe);
                break;
            }
        }
        if (recipeGroup == null) {
            if (recipe.value() instanceof CraftingRecipe) {
                recipeGroup = createCraftingRecipeWidget(inputs, output);
                workstation = new ItemStack(Items.CRAFTING_TABLE);
            } else if (recipe.value() instanceof AbstractCookingRecipe) {
                recipeGroup = createSmeltingRecipeWidget(inputs, output);
                workstation = new ItemStack(Items.FURNACE);
            } else {
                recipeGroup = createCraftingRecipeWidget(inputs, output);
            }
        }
        recipeGroup.addWidget(new ImageWidget(-40, recipeGroup.getSize().height / 2 - 15, 30, 30, new ItemStackTexture(workstation)));
        return currentPage.addStreamWidget(wrapper(recipeGroup));
    }

    protected WidgetGroup createSmeltingRecipeWidget(Int2ObjectMap<Ingredient> input, ItemStack output) {
        WidgetGroup widgetGroup = new WidgetGroup(0, 0, 150, 30);
        widgetGroup.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        CycleItemStackHandler itemStackHandler = new CycleItemStackHandler(List.of(
                Arrays.stream(input.getOrDefault(0, Ingredient.EMPTY).getItems()).toList()));
        widgetGroup.addWidget(new SlotWidget(itemStackHandler, 0, 20, 6, false, false));

        var handler = new ItemStackHandler();
        handler.setStackInSlot(0, output);
        widgetGroup.addWidget(new ProgressWidget(ProgressWidget.JEIProgress, 65, 5, 20, 20, new ProgressTexture()));
        widgetGroup.addWidget(new SlotWidget(handler, 0, 120, 6, false, false));
        return widgetGroup;
    }

    protected WidgetGroup createCraftingRecipeWidget(Int2ObjectMap<Ingredient> input, ItemStack output) {
        WidgetGroup widgetGroup = new WidgetGroup(0, 0, 150, 12 + 18 * 3);
        widgetGroup.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                var itemStackHandler = new CycleItemStackHandler(List.of(
                        Arrays.stream(input.getOrDefault(x + y * 3, Ingredient.EMPTY).getItems()).toList()));
                widgetGroup.addWidget(new SlotWidget(itemStackHandler, 0, x * 18 + 20, y * 18 + 6, false, false));
            }
        }

        var handler = new ItemStackHandler();
        handler.setStackInSlot(0, output);
        widgetGroup.addWidget(new ProgressWidget(ProgressWidget.JEIProgress, (3 * 18 + 20) / 2 + 60 - 10, (12 + 18 * 3) / 2 - 10, 20, 20, PROGRESS_BAR_ARROW));
        widgetGroup.addWidget(new SlotWidget(handler, 0, 120, (12 + 18 * 3) / 2 - 9, false, false));
        return widgetGroup;
    }
}
