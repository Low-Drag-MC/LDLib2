//package com.lowdragmc.lowdraglib2.integration.xei.emi;
//
//import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
//import dev.emi.emi.api.stack.EmiIngredient;
//import dev.emi.emi.api.stack.EmiStack;
//import dev.emi.emi.api.widget.Bounds;
//import dev.emi.emi.api.widget.SlotWidget;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
//import net.minecraft.network.chat.Component;
//import net.minecraft.util.FormattedCharSequence;
//import net.minecraft.world.inventory.tooltip.TooltipComponent;
//import org.jetbrains.annotations.Nullable;
//import org.joml.Matrix4f;
//import org.joml.Vector2f;
//import org.joml.Vector3f;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.BiPredicate;
//import java.util.function.Supplier;
//
//public class EMIRecipeSlotWidget extends SlotWidget {
//    public final Supplier<EmiIngredient> ingredientProvider;
//    public final Supplier<Matrix4f> localToWorldSupplier;
//    public final BiPredicate<Float, Float> isMouseOver;
//    public final Supplier<Bounds> boundsProvider;
//    @Nullable
//    private final Supplier<HoverTooltips> tooltipProvider;
//
//    public EMIRecipeSlotWidget(Supplier<EmiIngredient> ingredientProvider,
//                               Supplier<Matrix4f> localToWorldSupplier,
//                               BiPredicate<Float, Float> isMouseOver,
//                               Supplier<Bounds> boundsProvider) {
//        this(ingredientProvider, localToWorldSupplier, isMouseOver, boundsProvider, null);
//    }
//
//    public EMIRecipeSlotWidget(Supplier<EmiIngredient> ingredientProvider,
//                               Supplier<Matrix4f> localToWorldSupplier,
//                               BiPredicate<Float, Float> isMouseOver,
//                               Supplier<Bounds> boundsProvider,
//                               @Nullable Supplier<HoverTooltips> tooltipProvider) {
//        super(EmiStack.EMPTY, 0, 0);
//        this.localToWorldSupplier = localToWorldSupplier;
//        this.isMouseOver = isMouseOver;
//        this.ingredientProvider = ingredientProvider;
//        this.boundsProvider = boundsProvider;
//        this.tooltipProvider = tooltipProvider;
//    }
//
//    public Vector2f getWorldMouse(float mouseX, float mouseY) {
//        var realMouse = localToWorldSupplier.get().transformPosition(new Vector3f(0, 0, 0))
//                .mul(-1)
//                .add(mouseX, mouseY, 0);
//        return new Vector2f(realMouse.x, realMouse.y);
//    }
//
//    @Override
//    public EmiIngredient getStack() {
//        return ingredientProvider.get();
//    }
//
//    @Override
//    public Bounds getBounds() {
//        var bounds = boundsProvider.get();
//        var transform = localToWorldSupplier.get();
//        var pos = transform.transformPosition(new Vector3f(bounds.x(), bounds.y(), 0));
//        var size = transform.transformDirection(new Vector3f(bounds.width(), bounds.height(), 0));
//        return new Bounds((int) pos.x, (int) pos.y, (int) size.x, (int) size.y);
//    }
//
//    @Override
//    public List<ClientTooltipComponent> getTooltip(int mouseX, int mouseY) {
//        var realMouse = getWorldMouse(mouseX, mouseY);
//        if (!isMouseOver.test(realMouse.x, realMouse.y)) return List.of();
//        var tooltip = new ArrayList<>(super.getTooltip(mouseX, mouseY));
//        if (tooltipProvider != null) {
//            var hoverTooltips = tooltipProvider.get();
//            if (hoverTooltips != null) {
//                for (var entry : hoverTooltips.tooltips()) {
//                    if (entry instanceof Component component) {
//                        tooltip.add(ClientTooltipComponent.create(component.getVisualOrderText()));
//                    } else if (entry instanceof FormattedCharSequence sequence) {
//                        tooltip.add(ClientTooltipComponent.create(sequence));
//                    } else if (entry instanceof ClientTooltipComponent component) {
//                        tooltip.add(component);
//                    } else if (entry instanceof TooltipComponent component) {
//                        tooltip.add(ClientTooltipComponent.create(component));
//                    }
//                }
//            }
//        }
//        return tooltip;
//    }
//
//    @Override
//    public boolean mouseClicked(int mouseX, int mouseY, int button) {
//        var realMouse = getWorldMouse(mouseX, mouseY);
//        if (!isMouseOver.test(realMouse.x, realMouse.y)) return false;
//        return super.mouseClicked(mouseX, mouseY, button);
//    }
//
//    @Override
//    public void drawStack(GuiGraphics draw, int mouseX, int mouseY, float delta) {
//        // do not draw stack yourself
//    }
//
//    @Override
//    public void drawBackground(GuiGraphics draw, int mouseX, int mouseY, float delta) {
//        // do not draw background yourself
//    }
//
//    @Override
//    public void drawOverlay(GuiGraphics draw, int mouseX, int mouseY, float delta) {
//        // do not draw overlay yourself
//    }
//
//    @Override
//    public boolean shouldDrawSlotHighlight(int mouseX, int mouseY) {
//        return false;
//    }
//}
