package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.DelegatingUIElementRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@LDLRegisterClient(name = "item_slot", registry = "ldlib2:ui_element_renderer")
public final class ItemSlotRenderer extends DelegatingUIElementRenderer<ItemSlot, ItemSlotRenderer> {
    @Override
    public Class<ItemSlot> type() {
        return ItemSlot.class;
    }

    @Override
    public void drawBackgroundAdditional(ItemSlot itemSlot, IGUIContext context) {
        if (!(context instanceof GUIContext guiContext)) {
            drawParentBackgroundAdditional(itemSlot, context);
            return;
        }
        drawBackgroundAdditional(itemSlot, guiContext);
    }

    static void drawBackgroundAdditional(ItemSlot itemSlot, GUIContext context) {
        var value = itemSlot.getValue();
        var mui = itemSlot.getModularUI();
        if (mui == null) return;
        var hovered = itemSlot.isHover() || itemSlot.isSelfOrChildHover();
        var drawDraggingBackground = false;
        if (ModularUIClientAccess.getScreen(mui) instanceof AbstractContainerScreen<?> containerScreen) {
            var carried = containerScreen.getMenu().getCarried();
            if (itemSlot.getSlot() == containerScreen.clickedSlot && !containerScreen.draggingItem.isEmpty() && containerScreen.isSplittingStack && !value.isEmpty()) {
                value = value.copyWithCount(value.getCount() / 2);
                drawDraggingBackground = true;
            } else if (containerScreen.isQuickCrafting && containerScreen.quickCraftSlots.contains(itemSlot.getSlot()) && !carried.isEmpty()) {
                if (containerScreen.quickCraftSlots.size() == 1) {
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(itemSlot.getSlot(), carried, true) && containerScreen.getMenu().canDragTo(itemSlot.getSlot())) {
                    int k = Math.min(carried.getMaxStackSize(), itemSlot.getSlot().getMaxStackSize(carried));
                    int l = itemSlot.getSlot().getItem().isEmpty() ? 0 : itemSlot.getSlot().getItem().getCount();
                    int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(containerScreen.quickCraftSlots.size(), containerScreen.quickCraftingType, carried) + l;
                    if (i1 > k) {
                        i1 = k;
                    }

                    value = carried.copyWithCount(i1);
                    drawDraggingBackground = true;
                } else {
                    containerScreen.quickCraftSlots.remove(itemSlot.getSlot());
                    containerScreen.recalculateQuickCraftRemaining();
                }
            }
        }

        var drawSlotOverlay = value.isEmpty() || !itemSlot.getSlotStyle().showSlotOverlayOnlyEmpty();
        if (value.isEmpty() && !hovered && !drawDraggingBackground && !drawSlotOverlay) return;

        var contentX = itemSlot.getContentX();
        var contentY = itemSlot.getContentY();
        var contentWidth = itemSlot.getContentWidth();
        var contentHeight = itemSlot.getContentHeight();

        context.pose.pushPose();
        context.pose.scale(contentWidth / 16f, contentHeight / 16f);
        context.pose.translate(contentX * 16 / contentWidth, contentY * 16 / contentHeight);

        if (drawDraggingBackground) {
            drawDraggingBackground(context);
        }
        if (drawSlotOverlay) {
            drawSlotOverlay(itemSlot, context);
        }
        if (!value.isEmpty()) {
            drawItemStack(context, value);
        }
        if (hovered) {
            drawHover(itemSlot, context);
        }
        context.pose.popPose();
    }

    private static void drawDraggingBackground(GUIContext context) {
        context.drawTexture(ItemSlot.DRAGGING_BG, 0, 0, 16, 16);
    }

    private static void drawSlotOverlay(ItemSlot itemSlot, GUIContext context) {
        context.drawTexture(itemSlot.getSlotStyle().slotOverlay(), 0, 0, 16, 16);
    }

    private static void drawItemStack(GUIContext context, ItemStack itemStack) {
        DrawerHelperClient.drawItemStack(context, itemStack, 0, 0, 0);
    }

    private static void drawHover(ItemSlot itemSlot, GUIContext context) {
        context.drawTexture(itemSlot.getSlotStyle().hoverOverlay(), 0, 0, 16, 16);
    }
}
