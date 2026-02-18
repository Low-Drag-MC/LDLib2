package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings({"unchecked", "rawtypes"})
public interface ModularUIDisplay extends Display {

    @Nullable
    default ModularUIDisplayCategory<?> getCategory() {
        return CategoryRegistry.getInstance().tryGet(getCategoryIdentifier()).map(categoryConfiguration -> {
            if (categoryConfiguration.getCategory() instanceof ModularUIDisplayCategory<?> category) {
                return category;
            }
            return null;
        }).orElse(null);
    }

    @Nullable
    default ModularUI getModularUI() {
        if (getCategory() instanceof ModularUIDisplayCategory category) {
            return category.getUIForDisplay(this);
        }
        return null;
    }

    @Override
    default List<EntryIngredient> getInputEntries() {
        if (getCategory() instanceof ModularUIDisplayCategory category) {
            return category.getIngredients(this).inputs;
        }
        return List.of();
    }

    @Override
    default List<EntryIngredient> getOutputEntries() {
        if (getCategory() instanceof ModularUIDisplayCategory category) {
            return category.getIngredients(this).outputs;
        }
        return List.of();
    }
}
