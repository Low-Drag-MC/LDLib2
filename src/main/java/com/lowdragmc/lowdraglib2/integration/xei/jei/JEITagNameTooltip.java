package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.integration.xei.jei.handler.JEIRecipeSlotHandler;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.common.Tags;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

/**
 * Restores the {@code Tag: Item} / {@code #minecraft:logs} tooltip lines that JEI drops once a recipe
 * slot has display overrides.
 * <p>
 * JEI builds those lines from {@code RecipeSlot#getVisibleCandidates()}, which is nothing but the display
 * overrides while there are any, and it only recognizes a tag when the candidates are equal to the whole
 * tag. So as soon as {@link JEIRecipeSlotHandler.SlotUpdater} pins the slot to the single ingredient
 * LDLib renders, JEI can no longer see the tag, and the tag name, the tag content grid and the
 * "pause cycling" hint all disappear.
 * <p>
 * JEI 30.x, which the 26.x branches build against, fixed this on its side: an overridden ingredient is
 * mapped back to its display group and the tag is computed from that whole group. JEI 19.x has no such
 * mechanism, so this recomputes the tag from the complete alternatives snapshotted during layout. Only
 * the two text lines can be restored this way, the tag content grid is an internal tooltip component.
 * <p>
 * <b>Delete this once this branch builds against a JEI that has display groups.</b>
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
final class JEITagNameTooltip {
    private JEITagNameTooltip() {
    }

    /**
     * Resolves the tag lines of a binding once, while the recipe is laid out.
     *
     * @return the lines to show while the slot is overridden, empty if its alternatives are not a whole tag.
     */
    static List<Component> resolve(JEIRecipeSlotHandler.Binding binding) {
        // only interactive slots can ever be overridden
        if (binding.slotUpdater() == null) return List.of();
        var ingredients = binding.ingredients();
        // JEI never reports a tag for a single ingredient either
        if (ingredients.size() < 2) return List.of();
        var ingredientManager = LDLibJEIPlugin.ingredientManager;
        if (ingredientManager == null) return List.of();
        return getTagKeyEquivalent(ingredientManager, ingredients)
                .map(JEITagNameTooltip::toTooltipLines)
                .orElseGet(List::of);
    }

    /**
     * Adds the lines resolved by {@link #resolve} if JEI cannot see the whole tag by itself anymore.
     */
    static void append(JEIRecipeSlotHandler.Binding binding, List<Component> tagLines,
                       IRecipeSlotView slotView, ITooltipBuilder tooltip) {
        if (tagLines.isEmpty()) return;
        var slotUpdater = binding.slotUpdater();
        // JEI still adds them itself as long as it sees all the alternatives
        if (slotUpdater == null || !slotUpdater.hasDisplayOverride()) return;
        // an overridden empty slot has no ingredient tooltip to append to
        if (slotView.getDisplayedIngredient().isEmpty()) return;
        for (var line : tagLines) {
            tooltip.add(line);
        }
    }

    private static List<Component> toTooltipLines(TagKey<?> tagKey) {
        var registryName = tagKey.registry().location().getPath().replace('_', ' ');
        return List.of(
                Component.translatable("jei.tooltip.recipe.tag", StringUtils.capitalize(registryName))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatableWithFallback(Tags.getTagTranslationKey(tagKey), "#" + tagKey.location())
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private static Optional<TagKey<?>> getTagKeyEquivalent(IIngredientManager ingredientManager,
                                                           List<ITypedIngredient<?>> ingredients) {
        return ingredients.stream()
                .findFirst()
                .flatMap(first -> getTagKeyEquivalent(ingredientManager, ingredients, first));
    }

    private static <T> Optional<TagKey<?>> getTagKeyEquivalent(IIngredientManager ingredientManager,
                                                              List<ITypedIngredient<?>> ingredients,
                                                              ITypedIngredient<T> first) {
        IIngredientType<T> ingredientType = first.getType();
        List<T> values = ingredients.stream()
                .map(ingredient -> ingredient.getIngredient(ingredientType))
                .flatMap(Optional::stream)
                .toList();
        // mixed types are never a single tag
        if (values.size() != ingredients.size()) return Optional.empty();
        return ingredientManager.getIngredientHelper(ingredientType).getTagKeyEquivalent(values);
    }
}
