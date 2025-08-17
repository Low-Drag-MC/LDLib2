package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.BindableUIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemSlot extends BindableUIElement<ItemStack> {
    public final static SpriteTexture ITEM_SLOT_TEXTURE = SpriteTexture.of("ldlib2:textures/gui/slot.png")
            .setSprite(0, 0, 18, 18).setBorder(1, 1, 1, 1);

    @Accessors(chain = true, fluent = true)
    public static class SlotStyle extends Style {
        @Getter
        @Setter
        private boolean canTakeItem = true;
        @Getter
        @Setter
        private boolean canPlaceItem = true;
        @Getter
        @Setter
        private IGuiTexture hoverOverlay = new ColorRectTexture(0x80FFFFFF);

        @Getter @Setter
        private List<Component> tooltips = List.of();

        public SlotStyle(ItemSlot holder) {
            super(holder);
        }
    }
    @Getter
    private final SlotStyle slotStyle = new SlotStyle(this);

    // runtime
    @Getter
    private boolean hovered = false;
    @Getter
    private ItemStack item = ItemStack.EMPTY;

    public ItemSlot() {
        getLayout().setWidth(18);
        getLayout().setHeight(18);
        getLayout().setPadding(YogaEdge.ALL, 1);
        getStyle().backgroundTexture(ITEM_SLOT_TEXTURE);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.CLICK, this::onClick);
        addEventListener(UIEvents.MOUSE_ENTER, this::onMouseEnter, true);
        addEventListener(UIEvents.MOUSE_LEAVE, this::onMouseLeave, true);
    }

    public ItemSlot buttonStyle(Consumer<SlotStyle> style) {
        style.accept(slotStyle);
        onStyleChanged();
        return this;
    }

    @Override
    public void applyStyle(Map<String, StyleValue<?>> values) {
        super.applyStyle(values);
        slotStyle.applyStyles(values);
    }

    public ItemSlot setItem(ItemStack item) {
        return setValue(item, true);
    }

    public ItemSlot setItem(ItemStack itemStack, boolean notify) {
        return setValue(itemStack, notify);
    }

    public List<Component> getFullTooltipTexts() {
        var tips = new ArrayList<>(DrawerHelper.getItemToolTip(item));
        tips.addAll(getStyle().tooltips());
        return tips;
    }

    protected void onHoverTooltips(UIEvent event) {
        if (item.isEmpty()) return;
        event.hoverTooltips = new HoverTooltips(getFullTooltipTexts(), item.getTooltipImage().orElse(null), null, item);
    }

    protected void onMouseLeave(UIEvent event) {
        hovered = false;
    }

    protected void onMouseEnter(UIEvent event) {
        hovered = true;
    }

    protected void onClick(UIEvent event) {
        if (event.button == 0) {
            if (slotStyle.canTakeItem()) {
                if (takeItemToHand(Integer.MAX_VALUE) > 0) {
                    return;
                }
            }
            if (slotStyle.canPlaceItem()){
                if (placeItemFromHand(Integer.MAX_VALUE) > 0) {
                    return;
                }
            }
        }
    }

    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    /**
     * Attempts to place an item from the player's currently carried stack in their hand into this item slot.
     * If the item slot is empty, it accepts the item from the carried stack. If the slot already contains
     * an item, it attempts to merge the stack with the carried stack if they match.
     *
     * @param maxAmount the maximum number of items to transfer from the carried stack to this slot
     * @return the actual number of items successfully transferred into the slot
     */
    public int placeItemFromHand(int maxAmount) {
        if (maxAmount <= 0) return 0;
        var mui = getModularUI();
        if (mui != null && mui.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            var menu = containerScreen.getMenu();
            var carried = menu.getCarried();
            if (!carried.isEmpty() && isItemValid(carried)) {
                if (!item.isEmpty() && !ItemStack.matches(item, carried)) {
                    return 0;
                }
                var validAmount = Math.min(maxAmount, carried.getCount());
                var toMove = item.isEmpty() ? validAmount : Math.min(validAmount, item.getMaxStackSize() - item.getCount());
                if (toMove <= 0) return 0;
                var moved = carried.split(toMove);
                var amount = moved.getCount();
                moved.grow(item.getCount());
                setValue(moved);
                return amount;
            }
        }
        return 0;
    }

    /**
     * Attempts to transfer a specified maximum number of items from this item slot to the player's carried item stack.
     * If the item slot contains items that match the carried stack, it moves up to the specified number of items,
     * or as many as can fit. If there are no matching items or the carried stack has no space, no items are transferred.
     *
     * @param maxAmount the maximum number of items to transfer from this slot to the carried stack
     * @return the actual number of items successfully transferred to the carried stack
     */
    public int takeItemToHand(int maxAmount) {
        if (maxAmount <= 0) return 0;
        var mui = getModularUI();
        if (mui != null && mui.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            var menu = containerScreen.getMenu();
            var carried = menu.getCarried();
            if (!item.isEmpty()) {
                if (!carried.isEmpty() && !ItemStack.matches(item, carried)) {
                    return 0;
                }
                var validAmount = Math.min(maxAmount, item.getCount());
                var toMove = carried.isEmpty() ? validAmount : Math.min(validAmount, carried.getMaxStackSize() - carried.getCount());
                if (toMove <= 0) return 0;
                var moved = item.copyWithCount(toMove);
                menu.setCarried(moved);
                setValue(item.copyWithCount(item.getCount() - toMove));
                return toMove;
            }
        }
        return 0;
    }

    @Override
    public ItemStack getValue() {
        return item;
    }

    @Override
    public ItemSlot setValue(ItemStack value, boolean notify) {
        if (ItemStack.matches(value, item)) return this;
        item = value;
        if (notify) notifyListeners();
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (item.isEmpty() && !hovered) return;
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        graphics.pose().pushPose();
        graphics.pose().scale(contentWidth / 16f, contentHeight / 16f, 1);
        graphics.pose().translate(contentX * 16 / contentWidth, contentY * 16 / contentHeight, -200);
        if (!item.isEmpty()) {
            DrawerHelper.drawItemStack(graphics, item, 0, 0, -1, null);
        }
        if (hovered) {
            slotStyle.hoverOverlay.draw(graphics, mouseX, mouseY, 0, 0, 16, 16, partialTicks);
        }
        graphics.pose().popPose();
    }
}
