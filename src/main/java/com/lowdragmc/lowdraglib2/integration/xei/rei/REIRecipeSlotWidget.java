package com.lowdragmc.lowdraglib2.integration.xei.rei;

import dev.emi.emi.api.widget.Bounds;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import me.shedaniel.rei.impl.client.util.OriginalRetainingCyclingList;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class REIRecipeSlotWidget extends EntryWidget {
    public final BiPredicate<Double, Double> isMouseOver;
    public Supplier<Rectangle> boundsProvider;
    public final Supplier<EntryStack<?>> displayedIngredient;
    @Nullable
    public final Supplier<List<EntryStack<?>>> allIngredients;
    @Nullable
    private final Consumer<Tooltip> tooltipCallback;

    public REIRecipeSlotWidget(Rectangle containerBounds,
                               BiPredicate<Double, Double> isMouseOver,
                               Supplier<Rectangle> boundsProvider,
                               Supplier<EntryStack<?>> displayedIngredient,
                               @Nullable Supplier<List<EntryStack<?>>> allIngredients,
                               @Nullable Consumer<Tooltip> tooltipCallback) {
        super(containerBounds);
        this.isMouseOver = isMouseOver;
        this.boundsProvider = boundsProvider;
        this.displayedIngredient = displayedIngredient;
        this.allIngredients = allIngredients;
        this.tooltipCallback = tooltipCallback;

        noHighlight();
        noBackground();
    }

    @Override
    public OriginalRetainingCyclingList<EntryStack<?>> getCyclingEntries() {
        // TODO: it will be used to scroll candidates. This slot doesn't allow it.
        return super.getCyclingEntries();
    }

    @Override
    public EntryStack<?> getCurrentEntry() {
        return displayedIngredient.get();
    }

    @Override
    public List<EntryStack<?>> getEntries() {
        return allIngredients == null ? List.of(displayedIngredient.get()) : allIngredients.get();
    }

    @Override
    public Rectangle getBounds() {
        // to get the correct global bounds;
        var containerBounds = super.getBounds();
        var localBounds = boundsProvider.get();
        localBounds.translate(containerBounds.x, containerBounds.y);
        return localBounds;
    }

    @Override
    public Rectangle getInnerBounds() {
        var bounds = getBounds();
        // shall we return content here? what does the method use for?
        return new Rectangle(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2);
    }

    @Override
    protected void drawCurrentEntry(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // do not draw entry here delegate to ldlib2
    }

    @Override
    public @Nullable Tooltip getCurrentTooltip(TooltipContext context) {
        var tooltip = super.getCurrentTooltip(context);
        if (tooltip != null && tooltipCallback != null) {
            tooltipCallback.accept(tooltip);
        }
        return tooltip;
    }

    @Override
    public boolean containsMouse(double mouseX, double mouseY) {
        var containerBounds = super.getBounds();
        return isMouseOver.test(mouseX - containerBounds.x, mouseY - containerBounds.y);
    }
}
