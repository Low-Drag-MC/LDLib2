package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.SlotAccessor;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.slot.LocalSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.IItemSlotHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.UIStyleRegistries;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@LDLRegister(name = "item_slot", registry = "ldlib2:ui_element")
public class ItemSlot extends BindableUIElement<ItemStack> {
    public final static SpriteTexture ITEM_SLOT_TEXTURE = SpriteTexture.of("ldlib2:textures/gui/slot.png")
            .setSprite(0, 0, 18, 18).setBorder(1, 1, 1, 1);

    @Accessors(chain = true, fluent = true)
    public static class SlotStyle extends Style {
        @Getter
        @Setter
        @Configurable(name = "hoverOverlay")
        private IGuiTexture hoverOverlay = new ColorRectTexture(0x80FFFFFF);
        @Getter @Setter
        @Configurable(name = "showItemTooltips")
        private boolean showItemTooltips = true;
        @Getter @Setter
        @Configurable(name = "isPlayerSlot")
        private boolean isPlayerSlot = false;
        @Getter @Setter
        @Configurable(name = "acceptQuickMove")
        private boolean acceptQuickMove = true;
        @Getter @Setter
        @Configurable(name = "quickMovePriority")
        @ConfigNumber(range = {Integer.MIN_VALUE, Integer.MAX_VALUE})
        private int quickMovePriority = 0;

        public SlotStyle(ItemSlot holder) {
            super(holder);
        }

        @Override
        public void applyStyles(Map<String, StyleValue<?>> values) {
            super.applyStyles(values);

            UIStyleRegistries.HOVER_OVERLAY.parse(values).ifPresent(this::hoverOverlay);
            UIStyleRegistries.SHOW_ITEM_TOOLTIPS.parse(values).ifPresent(this::showItemTooltips);
            UIStyleRegistries.PLAYER_SLOT.parse(values).ifPresent(this::isPlayerSlot);
            UIStyleRegistries.ACCEPT_QUICK_MOVE.parse(values).ifPresent(this::acceptQuickMove);
            UIStyleRegistries.QUICK_MOVE_PRIORITY.parse(values).ifPresent(this::quickMovePriority);
        }
    }

    @Getter
    @Configurable(name = "slotStyle", subConfigurable = true)
    private final SlotStyle slotStyle = new SlotStyle(this);

    // runtime
    @Getter
    private Slot slot;

    public ItemSlot() {
        this(new LocalSlot());
    }

    public ItemSlot(Slot slot) {
        getLayout().setWidth(18);
        getLayout().setHeight(18);
        getLayout().setPadding(YogaEdge.ALL, 1);
        getStyle().backgroundTexture(ITEM_SLOT_TEXTURE);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        bind(slot);
    }

    public ItemSlot bind(IItemHandlerModifiable itemHandlerModifiable, int index) {
        bind(new ItemHandlerSlot(itemHandlerModifiable, index));
        return this;
    }

    public ItemSlot bind(@Nonnull Slot slot) {
        if (this.slot == slot) return this;
        this.slot = slot;
        addSlotToTheMenu();
        return this;
    }

    private void addSlotToTheMenu() {
        if (slot instanceof LocalSlot) return;
        updateSlotPosition();
        var mui = getModularUI();
        if (mui != null) {
            var menu = mui.getMenu();
            if (menu != null) {
                if (!menu.slots.contains(slot)) {
                    if (menu instanceof IItemSlotHolderMenu itemSlotHolderMenu) {
                        itemSlotHolderMenu.addSlot(this);
                    } else {
                        menu.addSlot(slot);
                    }
                }
            }
        }
    }

    public ItemSlot slotStyle(Consumer<SlotStyle> style) {
        style.accept(slotStyle);
        onStyleChanged();
        return this;
    }

    public void updateSlotPosition() {
        var mui = getModularUI();
        if (mui != null && slot instanceof SlotAccessor slotAccessor) {
            slotAccessor.setX((int) (getContentX() - mui.getLeftPos()));
            slotAccessor.setY((int) (getContentY() - mui.getTopPos()));
        }
    }

    @Override
    protected void onLayoutChanged() {
        super.onLayoutChanged();
        updateSlotPosition();
    }

    @Override
    protected void _setModularUIInternal(@Nullable ModularUI mui) {
        super._setModularUIInternal(mui);
        addSlotToTheMenu();
    }

    public ItemSlot setItem(ItemStack item) {
        return setValue(item, true);
    }

    public ItemSlot setItem(ItemStack itemStack, boolean notify) {
        return setValue(itemStack, notify);
    }

    public List<Component> getFullTooltipTexts() {
        var tips = new ArrayList<Component>();
        if (slotStyle.showItemTooltips) {
            tips.addAll(DrawerHelper.getItemToolTip(getValue()));
        }
        tips.addAll(getStyle().tooltips());
        return tips;
    }

    protected void onHoverTooltips(UIEvent event) {
        var item = getValue();
        if (item.isEmpty()) return;
        event.hoverTooltips = new HoverTooltips(getFullTooltipTexts(), item.getTooltipImage().orElse(null), null, item);
    }

    protected void onMouseDown(UIEvent event) {
        event.stopPropagation();
        event.hasHandler = false;
    }

    @Override
    public ItemStack getValue() {
        return slot.getItem();
    }

    @Override
    public ItemSlot setValue(@Nullable ItemStack value, boolean notify) {
        if (value == null) value = ItemStack.EMPTY;
        if (ItemStack.matches(value, getValue())) return this;
        slot.set(value);
        if (notify) notifyListeners();
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var value = getValue();
        var mui = guiContext.modularUI;
        var hovered = isHover() || isChildHover();
        if (mui.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            var carried = containerScreen.getMenu().getCarried();
            if (slot == containerScreen.clickedSlot && !containerScreen.draggingItem.isEmpty() && containerScreen.isSplittingStack && !value.isEmpty()) {
                value = value.copyWithCount(value.getCount() / 2);
            } else if (containerScreen.isQuickCrafting && containerScreen.quickCraftSlots.contains(slot) && !carried.isEmpty()) {
                if (containerScreen.quickCraftSlots.size() == 1) {
                    return;
                }

                if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && containerScreen.getMenu().canDragTo(slot)) {
                    int k = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                    int l = slot.getItem().isEmpty() ? 0 : slot.getItem().getCount();
                    int i1 = AbstractContainerMenu.getQuickCraftPlaceCount(containerScreen.quickCraftSlots, containerScreen.quickCraftingType, carried) + l;
                    if (i1 > k) {
                        i1 = k;
                    }

                    value = carried.copyWithCount(i1);
                } else {
                    containerScreen.quickCraftSlots.remove(slot);
                    containerScreen.recalculateQuickCraftRemaining();
                }
            }
        }

        if (value.isEmpty() && !hovered) return;
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        guiContext.pose.pushPose();
        guiContext.pose.scale(contentWidth / 16f, contentHeight / 16f, 1);
        guiContext.pose.translate(contentX * 16 / contentWidth, contentY * 16 / contentHeight, -200);
        if (!value.isEmpty()) {
            DrawerHelper.drawItemStack(guiContext.graphics, value, 0, 0, -1, null);
        }
        if (hovered) {
            guiContext.drawTexture(slotStyle.hoverOverlay, 0, 0, 16, 16);
        }
        guiContext.pose.popPose();
    }

}
