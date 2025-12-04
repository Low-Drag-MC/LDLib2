package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.SlotAccessor;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.slot.LocalSlot;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.holder.IItemSlotHolderMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.emi.EMIDragDropHandlers;
import com.lowdragmc.lowdraglib2.integration.emi.EMIUIEvents;
import com.lowdragmc.lowdraglib2.integration.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.jei.JEITarget;
import com.lowdragmc.lowdraglib2.integration.jei.JEITargetsTyped;
import com.lowdragmc.lowdraglib2.integration.jei.JEIUIEvents;
import com.lowdragmc.lowdraglib2.integration.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.integration.rei.LDLibREIPlugin;
import com.lowdragmc.lowdraglib2.integration.rei.REIDraggableStackBounds;
import com.lowdragmc.lowdraglib2.integration.rei.REIUIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import dev.architectury.event.CompoundEventResult;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.stack.ItemEmiStack;
import lombok.Getter;
import me.shedaniel.math.Point;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
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
import java.util.Optional;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "item-slot", group = "inventory", registry = "ldlib2:ui_element")
public class ItemSlot extends BindableUIElement<ItemStack> {
    public final static IGuiTexture ITEM_SLOT_TEXTURE = Sprites.RECT_RD_T.copy().setColor(0xffbbbbbb);

    @Configurable(name = "SlotStyle")
    public class SlotStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.HOVER_OVERLAY,
                PropertyRegistry.SHOW_ITEM_TOOLTIPS,
                PropertyRegistry.IS_PLAYER_SLOT,
                PropertyRegistry.ACCEPT_QUICK_MOVE,
                PropertyRegistry.QUICK_MOVE_PRIORITY,
        };

        public SlotStyle() {
            super(ItemSlot.this);
            setDefault(PropertyRegistry.HOVER_OVERLAY, new ColorRectTexture(0x80FFFFFF));
        }

        @Override
        protected Property<?>[] getProperties() {
            return PROPERTIES;
        }

        public IGuiTexture hoverOverlay() {
            return getValueSave(PropertyRegistry.HOVER_OVERLAY);
        }

        public SlotStyle hoverOverlay(IGuiTexture texture) {
            set(PropertyRegistry.HOVER_OVERLAY, texture);
            return this;
        }

        public boolean showItemTooltips() {
            return getValueSave(PropertyRegistry.SHOW_ITEM_TOOLTIPS);
        }

        public SlotStyle showItemTooltips(boolean show) {
            set(PropertyRegistry.SHOW_ITEM_TOOLTIPS, show);
            return this;
        }

        public boolean isPlayerSlot() {
            return getValueSave(PropertyRegistry.IS_PLAYER_SLOT);
        }

        public SlotStyle isPlayerSlot(boolean playerSlot) {
            set(PropertyRegistry.IS_PLAYER_SLOT, playerSlot);
            return this;
        }

        public int quickMovePriority() {
            return getValueSave(PropertyRegistry.QUICK_MOVE_PRIORITY);
        }

        public SlotStyle quickMovePriority(int priority) {
            set(PropertyRegistry.QUICK_MOVE_PRIORITY, priority);
            return this;
        }

        public boolean acceptQuickMove() {
            return getValueSave(PropertyRegistry.ACCEPT_QUICK_MOVE);
        }

        public SlotStyle acceptQuickMove(boolean accept) {
            set(PropertyRegistry.ACCEPT_QUICK_MOVE, accept);
            return this;
        }

    }

    @Getter
    private final SlotStyle slotStyle = new SlotStyle();
    // editor support
    @Configurable(name = "EditorItemDisplay")
    private ItemStack editorItemDisplay = ItemStack.EMPTY;
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
        if (LDLib2.isJeiLoaded()) {
            addEventListener(JEIUIEvents.CLICKABLE_INGREDIENT, JEISupport::onClickableIngredient);
        }
        if (LDLib2.isReiLoaded()) {
            addEventListener(REIUIEvents.FOCUSED_STACK, REISupport::onFocusedStack);
        }
        if (LDLib2.isEmiLoaded()) {
            addEventListener(EMIUIEvents.STACK_PROVIDER, EMISupport::onStackProvider);
        }
        bind(slot);
        internalSetup();
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

    public ItemSlot xeiPhantom() {
        if (LDLib2.isJeiLoaded()) {
            addEventListener(JEIUIEvents.VALID_TARGETS_TYPED, JEISupport::onTargetsTyped);
            addEventListener(JEIUIEvents.EXECUTE_TARGETS_TYPED, JEISupport::onTargetsTyped);
        }
        if (LDLib2.isReiLoaded()) {
            addEventListener(REIUIEvents.DRAGGABLE_STACK_BOUNDS, REISupport::onDraggableStackBounds);
            addEventListener(REIUIEvents.ACCEPT_DRAGGABLE_STACK, REISupport::onAcceptDraggableStack);
        }
        if (LDLib2.isEmiLoaded()) {
            addEventListener(EMIUIEvents.RENDER_DRAG_HANDLER, EMISupport::onRenderDragHandler);
            addEventListener(EMIUIEvents.DROP_STACK_HANDLER, EMISupport::onDropStackHandler);
        }
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
        if (slotStyle.showItemTooltips()) {
            tips.addAll(DrawerHelper.getItemToolTip(getValue()));
        }
        tips.addAll(getStyle().tooltips().asList());
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
            guiContext.drawTexture(slotStyle.hoverOverlay(), 0, 0, 16, 16);
        }
        guiContext.pose.popPose();
    }

    /// Editor Support
    @ConfigSetter(field = "editorItemDisplay")
    private void setEditorItemDisplay(ItemStack itemStack) {
        this.editorItemDisplay = itemStack;
        setValue(itemStack, false);
    }

    @SkipPersistedValue(field = "editorItemDisplay")
    private boolean skipEditorItemDisplay(ItemStack itemStack) {
        return itemStack == ItemStack.EMPTY;
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        this.editorItemDisplay = ItemStack.EMPTY;
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (!editorItemDisplay.isEmpty()) {
            setValue(editorItemDisplay, false);
        }
    }

    /// XEI Support
    public static class JEISupport {
        public static void onClickableIngredient(UIEvent event) {
            if (LDLib2.isJeiLoaded() && event.currentElement instanceof ItemSlot itemSlot && itemSlot.isMouseOverElement(event.x, event.y)) {
                if (event.customData instanceof IClickableIngredientFactory factory) {
                    var item = itemSlot.getValue();
                    if (item.isEmpty()) return;
                    event.customData = factory.createBuilder(item).buildWithArea(LDLibJEIPlugin.getArea(itemSlot));
                    event.stopPropagation();
                }
            }
        }

        public static void onTargetsTyped(UIEvent event) {
            if (LDLib2.isJeiLoaded() &&
                    event.currentElement instanceof ItemSlot itemSlot &&
                    event.customData instanceof JEITargetsTyped(var ingredient, var targets)) {
                Optional.ofNullable(ingredient.cast(VanillaTypes.ITEM_STACK)).ifPresent(typedIngredient -> {
                    var item = typedIngredient.getIngredient();
                    if (itemSlot.getSlot().mayPlace(item)) {
                        targets.add(cast(new JEITarget<ItemStack>(LDLibJEIPlugin.getArea(itemSlot, true), itemSlot::setValue)));
                    }
                });
            }
        }

        @SuppressWarnings("unchecked")
        public static <T> T cast(Object input) {
            return (T) input;
        }
    }

    // region XEI Supports
    public static class REISupport {
        public static void onFocusedStack(UIEvent event) {
            if (LDLib2.isReiLoaded() && event.currentElement instanceof ItemSlot itemSlot && itemSlot.isMouseOverElement(event.x, event.y)) {
                var item = itemSlot.getValue();
                if (item.isEmpty()) return;
                event.customData = CompoundEventResult.interruptTrue(EntryStacks.of(item));
                event.stopPropagation();
            }
        }

        public static void onDraggableStackBounds(UIEvent event) {
            if (LDLib2.isReiLoaded() &&
                    event.currentElement instanceof ItemSlot itemSlot &&
                    event.customData instanceof REIDraggableStackBounds(var context, var stack, var bounds)) {
                var target = stack.get();
                if (target.getType() == VanillaEntryTypes.ITEM) {
                    ItemStack item = target.castValue();
                    if (itemSlot.getSlot().mayPlace(item)) {
                        bounds.add(DraggableStackVisitor.BoundsProvider.ofRectangle(LDLibREIPlugin.getRectangle(itemSlot, true)));
                    }
                }
            }
        }

        public static void onAcceptDraggableStack(UIEvent event) {
            if (LDLib2.isReiLoaded() &&
                    event.currentElement instanceof ItemSlot itemSlot &&
                    event.customData instanceof REIDraggableStackBounds(var context, var stack, var bounds) &&
                    context.getCurrentPosition() instanceof Point point &&
                    itemSlot.isMouseOverElement(point.x, point.y)
            ) {
                var target = stack.get();
                if (target.getType() == VanillaEntryTypes.ITEM) {
                    ItemStack item = target.castValue();
                    if (itemSlot.getSlot().mayPlace(item)) {
                        itemSlot.setValue(item);
                        event.stopPropagation();
                    }
                }
            }
        }
    }

    public static class EMISupport {
        public static void onStackProvider(UIEvent event) {
            if (LDLib2.isEmiLoaded() && event.currentElement instanceof ItemSlot itemSlot && itemSlot.isMouseOverElement(event.x, event.y)) {
                var item = itemSlot.getValue();
                if (item.isEmpty()) return;
                event.customData = new EmiStackInteraction(EmiStack.of(item), null, false);
                event.stopPropagation();
            }
        }

        public static void onRenderDragHandler(UIEvent event) {
            if (LDLib2.isEmiLoaded() &&
                    event.currentElement instanceof ItemSlot itemSlot &&
                    event.customData instanceof EMIDragDropHandlers(var dragged, var bounds)) {
                if (dragged instanceof ItemEmiStack item) {
                    if (itemSlot.getSlot().mayPlace(item.getItemStack())) {
                        bounds.add(LDLibEMIPlugin.getBounds(itemSlot, true));
                    }
                }
            }
        }

        public static void onDropStackHandler(UIEvent event) {
            if (LDLib2.isEmiLoaded() &&
                    event.currentElement instanceof ItemSlot itemSlot &&
                    event.customData instanceof EmiIngredient dragged &&
                    itemSlot.isMouseOverElement(event.x, event.y)
            ) {
                if (dragged instanceof ItemEmiStack item) {
                    if (itemSlot.getSlot().mayPlace(item.getItemStack())) {
                        itemSlot.setValue(item.getItemStack());
                        event.stopPropagation();
                    }
                }
            }
        }
    }
    // endregion
}
