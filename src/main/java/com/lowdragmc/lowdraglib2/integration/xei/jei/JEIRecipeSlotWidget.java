package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.google.common.base.Suppliers;
import mezz.jei.api.gui.builder.IIngredientConsumer;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.platform.IPlatformRenderHelper;
import mezz.jei.common.platform.Services;
import mezz.jei.common.util.SafeIngredientUtil;
import mezz.jei.library.gui.ingredients.TagContentTooltipComponent;
import mezz.jei.library.ingredients.DisplayIngredientAcceptor;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * This is a utility class to provide recipe slot under the mouse
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class JEIRecipeSlotWidget implements IRecipeSlotDrawable {
    public final Supplier<Matrix4f> localToWorldSupplier;
    public final BiPredicate<Float, Float> isMouseOver;
    public final Supplier<ITypedIngredient<?>> displayedIngredient;
    @Nullable
    public final Supplier<List<@Nullable ITypedIngredient<?>>> allIngredients;
    @Nullable
    private final IRecipeSlotRichTooltipCallback tooltipCallback;

    /**
     * Constructs a new {@code DirectRecipeSlot}.
     *
     * @param isMouseOver A {@link BiPredicate} that determines if the mouse is over this slot
     *                    based on the given X and Y coordinates.
     * @param displayedIngredient A {@link Supplier} that provides the currently displayed ingredient
     *                            within this slot. May return {@code null}.
     * @param allIngredients A {@link Supplier} that provides a list of all ingredients
     *                       associated with this slot. You'd better list all possible ingredients of this slot.
     *                       For example, if you want to display a tooltip to show tag support. you should list all items of this tag here.
     * @param tooltipCallback A {@link IRecipeSlotRichTooltipCallback} that defines the behavior
     *                        for custom tooltips when interacting with this slot.
     */
    public JEIRecipeSlotWidget(Supplier<Matrix4f> localToWorldSupplier,
                               BiPredicate<Float, Float> isMouseOver,
                               Supplier<ITypedIngredient<?>> displayedIngredient,
                               @Nullable Supplier<List<@Nullable ITypedIngredient<?>>> allIngredients,
                               @Nullable IRecipeSlotRichTooltipCallback tooltipCallback) {
        this.localToWorldSupplier = localToWorldSupplier;
        this.isMouseOver = isMouseOver;
        this.displayedIngredient = displayedIngredient;
        this.allIngredients = allIngredients;
        this.tooltipCallback = tooltipCallback;
    }

    public Vector2f getWorldMouse(float mouseX, float mouseY) {
        var realMouse = localToWorldSupplier.get().transformPosition(new Vector3f(0, 0, 0))
                .mul(-1)
                .add(mouseX, mouseY, 0);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return isMouseOver.test(realMouse.x, realMouse.y);
    }

    @Override
    public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
        return Optional.ofNullable(displayedIngredient.get());
    }

    @Override
    @Deprecated
    public List<Component> getTooltip() {
        return List.of();
    }

    @Override
    public void draw(GuiGraphics guiGraphics) {

    }

    @Override
    public void drawHoverOverlays(GuiGraphics guiGraphics) {

    }

    @Override
    @Deprecated
    public void getTooltip(ITooltipBuilder tooltipBuilder) {}

    @Override
    public void drawTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // necessary ?
        JeiTooltip tooltip = new JeiTooltip();
        getDisplayedIngredient()
                .ifPresent(ingredient -> {
                    getTooltip(tooltip, ingredient);
                    tooltip.draw(guiGraphics, mouseX, mouseY);
                });

    }

    private <T> void getTooltip(ITooltipBuilder tooltip, ITypedIngredient<T> typedIngredient) {
        IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
        IIngredientType<T> ingredientType = typedIngredient.getType();
        IIngredientRenderer<T> ingredientRenderer = getIngredientRenderer(ingredientType);
        SafeIngredientUtil.getRichTooltip(tooltip, ingredientManager, ingredientRenderer, typedIngredient);
        addTagNameTooltip(tooltip, ingredientManager, typedIngredient);
        addIngredientsToTooltip(tooltip, typedIngredient);
        if (tooltipCallback != null) {
            tooltipCallback.onRichTooltip(this, tooltip);
        }
    }

    private <T> void addTagNameTooltip(ITooltipBuilder tooltip, IIngredientManager ingredientManager, ITypedIngredient<T> ingredient) {
        IIngredientType<T> ingredientType = ingredient.getType();
        List<T> ingredients = getIngredients(ingredientType).toList();
        if (ingredients.isEmpty()) {
            return;
        }

        IClientConfig clientConfig = Internal.getJeiClientConfigs().getClientConfig();
        if (clientConfig.getHideSingleTagContentTooltipEnabled() && ingredients.size() == 1) {
            return;
        }

        IIngredientHelper<T> ingredientHelper = ingredientManager.getIngredientHelper(ingredientType);
        ingredientHelper.getTagKeyEquivalent(ingredients)
                .ifPresent(tagKeyEquivalent -> {
                    tooltip.add(
                            Component.translatable("jei.tooltip.recipe.tag", "")
                                    .withStyle(ChatFormatting.GRAY)
                    );
                    IPlatformRenderHelper renderHelper = Services.PLATFORM.getRenderHelper();
                    Component tagName = renderHelper.getName(tagKeyEquivalent);
                    tooltip.add(
                            tagName.copy().withStyle(ChatFormatting.GRAY)
                    );
                });
    }

    private <T> void addIngredientsToTooltip(ITooltipBuilder tooltip, ITypedIngredient<T> displayed) {
        IClientConfig clientConfig = Internal.getJeiClientConfigs().getClientConfig();
        if (clientConfig.isTagContentTooltipEnabled()) {
            IIngredientType<T> type = displayed.getType();

            IJeiRuntime jeiRuntime = Internal.getJeiRuntime();
            IIngredientManager ingredientManager = jeiRuntime.getIngredientManager();
            IIngredientRenderer<T> renderer = ingredientManager.getIngredientRenderer(type);

            List<T> ingredients = getIngredients(type).toList();

            if (ingredients.size() > 1) {
                tooltip.add(new TagContentTooltipComponent<>(renderer, ingredients));
            }
        }
    }

    private <T> IIngredientRenderer<T> getIngredientRenderer(IIngredientType<T> ingredientType) {
        IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
        return ingredientManager.getIngredientRenderer(ingredientType);
    }

    @Override
    public void setPosition(int x, int y) {

    }

    @Nullable
    private DisplayIngredientAcceptor displayOverrides;

    @Override
    public IIngredientConsumer createDisplayOverrides() {
        if (displayOverrides == null) {
            IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
            displayOverrides = new DisplayIngredientAcceptor(ingredientManager);
        }
        return displayOverrides;
    }

    @Override
    public void clearDisplayOverrides() {
        displayOverrides = null;
    }

    @Override
    @Deprecated
    public Rect2i getRect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rect2i getAreaIncludingBackground() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<ITypedIngredient<?>> getAllIngredients() {
        return getAllIngredientsList().stream().filter(Objects::nonNull);
    }

    @Override
    public @Unmodifiable List<@Nullable ITypedIngredient<?>> getAllIngredientsList() {
        return allIngredients == null ? List.of() : allIngredients.get();
    }


    @Override
    public RecipeIngredientRole getRole() {
        return RecipeIngredientRole.RENDER_ONLY;
    }

    @Override
    public void drawHighlight(GuiGraphics guiGraphics, int color) {

    }

    @Override
    public Optional<String> getSlotName() {
        return Optional.empty();
    }
}
