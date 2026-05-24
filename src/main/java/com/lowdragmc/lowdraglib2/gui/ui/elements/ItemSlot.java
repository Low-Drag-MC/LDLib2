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
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.*;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.integration.xei.rei.LDLibREIPlugin;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import lombok.Getter;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.library.ingredients.itemStacks.TypedItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
@LDLRegister(name = "item-slot", group = "inventory", registry = "ldlib2:ui_element")
public class ItemSlot extends BindableUIElement<ItemStack> {
    public final static IGuiTexture ITEM_SLOT_TEXTURE = Sprites.RECT_RD_T.copy().setColor(0xffbbbbbb);
    public final static IGuiTexture DRAGGING_BG = new ColorRectTexture(0x80FFFFFF);

    @Configurable(name = "SlotStyle")
    public class SlotStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.HOVER_OVERLAY,
                PropertyRegistry.SLOT_OVERLAY,
                PropertyRegistry.SHOW_SLOT_OVERLAY_ONLY_EMPTY,
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

        public IGuiTexture slotOverlay() {
            return getValueSave(PropertyRegistry.SLOT_OVERLAY);
        }

        public SlotStyle slotOverlay(IGuiTexture texture) {
            set(PropertyRegistry.SLOT_OVERLAY, texture);
            return this;
        }

        public boolean showSlotOverlayOnlyEmpty() {
            return getValueSave(PropertyRegistry.SHOW_SLOT_OVERLAY_ONLY_EMPTY);
        }

        public SlotStyle showSlotOverlayOnlyEmpty(boolean value) {
            set(PropertyRegistry.SHOW_SLOT_OVERLAY_ONLY_EMPTY, value);
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
    @Configurable(name = "EditorAllowXEILookup")
    private boolean allowXEILookup = true;
    // runtime
    @Getter
    private Slot slot;

    public ItemSlot() {
        this(new LocalSlot());
    }

    public ItemSlot(Slot slot) {
        getLayout().width(18);
        getLayout().height(18);
        getLayout().paddingAll(1);
        getStyle().backgroundTexture(ITEM_SLOT_TEXTURE);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        addEventListener(UIEvents.MUI_CHANGED, this::onModularUIChanged);
        if (LDLib2.isClient() && !LDLib2.isServer()) {
            if (LDLib2.isJeiLoaded()) {
                JEISupport.clickableIngredient(this);
            }
            if (LDLib2.isReiLoaded()) {
                REISupport.focusedStack(this);
            }
            if (LDLib2.isEmiLoaded()) {
                EMISupport.stackProvider(this);
            }
        }
        bind(slot);
        internalSetup();
    }

    protected void onModularUIChanged(UIEvent event) {
        addSlotToTheMenu();
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
            JEISupport.ghostIngredient(this);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.draggableStackBounds(this);
            REISupport.acceptDraggableStack(this);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.renderDragHandler(this);
            EMISupport.dropStackHandler(this);
        }
        return this;
    }

    public ItemSlot xeiRecipeIngredient(IngredientIO io) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeIngredient(this, io);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeIngredient(this, io);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeIngredient(this, io);
        }
        return this;
    }

    public ItemSlot xeiRecipeIngredient(IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeIngredient(this, io, allPossibleItems);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeIngredient(this, io, allPossibleItems);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeIngredient(this, io, allPossibleItems);
        }
        return this;
    }

    public ItemSlot xeiRecipeSlot() {
        return xeiRecipeSlot(IngredientIO.NONE, 1);
    }

    public ItemSlot xeiRecipeSlot(IngredientIO io, float chance) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeSlot(this);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeSlot(this, io);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeSlot(this, chance);
        }
        return this;
    }

    public ItemSlot xeiRecipeSlot(IngredientIO io, float chance, int amount, Supplier<Stream<ItemStack>> allPossibleItems) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeSlot(this, allPossibleItems);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeSlot(this, io, allPossibleItems);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeSlot(this, () -> chance, () -> amount, allPossibleItems);
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
        var hovered = isHover() || isSelfOrChildHover();
        var drawDraggingBackground = false;
        // splitting
        if (mui.getScreen() instanceof AbstractContainerScreen<?> containerScreen) {
            var carried = containerScreen.getMenu().getCarried();
            if (slot == containerScreen.clickedSlot && !containerScreen.draggingItem.isEmpty() && containerScreen.isSplittingStack && !value.isEmpty()) {
                value = value.copyWithCount(value.getCount() / 2);
                drawDraggingBackground = true;
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
                    drawDraggingBackground = true;
                } else {
                    containerScreen.quickCraftSlots.remove(slot);
                    containerScreen.recalculateQuickCraftRemaining();
                }
            }
        }

        var drawSlotOverlay = value.isEmpty() || !slotStyle.showSlotOverlayOnlyEmpty();

        if (value.isEmpty() && !hovered && !drawDraggingBackground && !drawSlotOverlay) return;

        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        guiContext.pose.pushPose();
        guiContext.pose.scale(contentWidth / 16f, contentHeight / 16f, 1);
        guiContext.pose.translate(contentX * 16 / contentWidth, contentY * 16 / contentHeight, -200);

        if (drawDraggingBackground) {
            drawDraggingBackground(guiContext);
        }

        if (drawSlotOverlay) {
            drawSlotOverlay(guiContext);
        }
        if (!value.isEmpty()) {
            drawItemStack(guiContext, value);
        }
        if (hovered) {
            drawHover(guiContext);
        }
        guiContext.pose.popPose();
    }

    protected void drawDraggingBackground(GUIContext guiContext) {
        guiContext.drawTexture(DRAGGING_BG, 0, 0, 16, 16);
    }

    protected void drawSlotOverlay(GUIContext guiContext) {
        guiContext.drawTexture(slotStyle.slotOverlay(), 0, 0, 16, 16);
    }

    protected void drawItemStack(GUIContext guiContext, ItemStack itemStack) {
        DrawerHelper.drawItemStack(guiContext.graphics, itemStack, 0, 0, guiContext.elementColor, null);
    }

    protected void drawHover(GUIContext guiContext) {
        guiContext.drawTexture(slotStyle.hoverOverlay(), 0, 0, 16, 16);
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

    @ConfigSetter(field = "allowXEILookup")
    private void setAllowXEILookup(boolean allowXEILookup) {
        this.allowXEILookup = allowXEILookup;
    }

    @SkipPersistedValue(field = "allowXEILookup")
    private boolean skipAllowXEILookup(boolean allowXEILookup) {
        return allowXEILookup;
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

    @Override
    public void loadXml(Element element) {
        // allow xei lookup
        if (element.hasAttribute("allow-xei-lookup")) {
            setAllowXEILookup(XmlUtils.getAsBoolean(element, "allow-xei-Lookup", allowXEILookup));
        }
        // item display
        var item = XmlUtils.getItemStack(element);
        if (item != ItemStack.EMPTY) {
            setEditorItemDisplay(item);
        }
        super.loadXml(element);
    }

    // region XEI Supports
    public static class JEISupport {
        public static void clickableIngredient(ItemSlot itemSlot) {
            LDLibJEIPlugin.clickableIngredient(itemSlot, () -> {
                if (!itemSlot.allowXEILookup) return null;
                var current = itemSlot.getValue();
                if (current.isEmpty()) return null;
                return TypedItemStack.create(current);
            });
        }

        public static void ghostIngredient(ItemSlot itemSlot) {
            LDLibJEIPlugin.ghostIngredient(itemSlot, VanillaTypes.ITEM_STACK,
                    ingredient -> itemSlot.getSlot().mayPlace(ingredient.getIngredient()),
                    itemSlot::setValue);
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io) {
            recipeIngredient(itemSlot, io, () -> Stream.of(itemSlot.getValue()));
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibJEIPlugin.recipeIngredient(itemSlot, io, () -> allPossibleItems.get()
                    .map(TypedItemStack::create)
                    .collect(Collectors.toList()));
        }

        public static void recipeSlot(ItemSlot itemSlot) {
            recipeSlot(itemSlot, () -> Stream.of(itemSlot.getValue()));
        }

        public static void recipeSlot(ItemSlot itemSlot, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibJEIPlugin.recipeSlot(itemSlot, () -> {
                var item = itemSlot.getValue();
                return item.isEmpty() ? null : TypedItemStack.create(item);
            }, () ->allPossibleItems.get().map(TypedItemStack::create).collect(Collectors.toList()));
        }
    }

    public static class REISupport {
        public static void focusedStack(ItemSlot itemSlot) {
            LDLibREIPlugin.focusedStack(itemSlot, () -> {
                if (!itemSlot.allowXEILookup) return null;
                var item = itemSlot.getValue();
                if (item.isEmpty()) return null;
                return EntryStacks.of(item);
            });
        }

        public static void draggableStackBounds(ItemSlot itemSlot) {
            LDLibREIPlugin.draggableStackBounds(itemSlot,
                    VanillaEntryTypes.ITEM,
                    stack -> itemSlot.getSlot().mayPlace(stack.getValue()));
        }

        public static void acceptDraggableStack(ItemSlot itemSlot) {
            LDLibREIPlugin.acceptDraggableStack(itemSlot,
                    VanillaEntryTypes.ITEM,
                    stack -> itemSlot.getSlot().mayPlace(stack.getValue()),
                    stack -> itemSlot.setValue(stack.getValue()));
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io) {
            recipeIngredient(itemSlot, io, () -> Stream.of(itemSlot.getValue()));
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibREIPlugin.recipeIngredient(itemSlot, io, () -> allPossibleItems.get()
                    .map(EntryIngredients::of)
                    .toList()
            );
        }

        public static void recipeSlot(ItemSlot itemSlot, IngredientIO io) {
            recipeSlot(itemSlot, io, () -> Stream.of(itemSlot.getValue()));
        }

        public static void recipeSlot(ItemSlot itemSlot, IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibREIPlugin.recipeSlot(itemSlot, io,
                    () -> EntryStacks.of(itemSlot.getValue()),
                    () -> allPossibleItems.get().map(EntryStacks::of).collect(Collectors.toList()));
        }
    }

    public static class EMISupport {
        public static void stackProvider(ItemSlot itemSlot) {
            LDLibEMIPlugin.stackProvider(itemSlot, () -> {
                if (!itemSlot.allowXEILookup) return null;
                var item = itemSlot.getValue();
                if (item.isEmpty()) return null;
                return new EmiStackInteraction(EmiStack.of(item), null, false);
            });
        }

        public static void renderDragHandler(ItemSlot itemSlot) {
            LDLibEMIPlugin.renderDragHandler(itemSlot,
                    dragged -> dragged instanceof ItemEmiStack item && itemSlot.getSlot().mayPlace(item.getItemStack()));
        }

        public static void dropStackHandler(ItemSlot itemSlot) {
            LDLibEMIPlugin.dropStackHandler(itemSlot,
                    dragged -> dragged instanceof ItemEmiStack item && itemSlot.getSlot().mayPlace(item.getItemStack()),
                    dragged -> {
                        if (dragged instanceof ItemEmiStack item) {
                            itemSlot.setValue(item.getItemStack());
                        }
                    });
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io) {
            recipeIngredient(itemSlot, io, () -> Stream.of(itemSlot.getValue()));
        }

        public static void recipeIngredient(ItemSlot itemSlot, IngredientIO io, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibEMIPlugin.recipeIngredient(itemSlot, io, () -> allPossibleItems.get()
                    .map(EmiStack::of)
                    .collect(Collectors.toList())
            );
        }

        public static void recipeSlot(ItemSlot itemSlot, float chance) {
            LDLibEMIPlugin.recipeSlot(itemSlot, () -> EmiStack.of(itemSlot.getValue()).setChance(chance));
        }

        public static void recipeSlot(ItemSlot itemSlot, Supplier<Float> chance, IntSupplier amount, Supplier<Stream<ItemStack>> allPossibleItems) {
            LDLibEMIPlugin.recipeSlot(itemSlot, () ->
                    new ListEmiIngredient(
                            allPossibleItems.get().map(EmiStack::of)
                            .map(e -> e.setChance(chance.get())).collect(Collectors.toList()), amount.getAsInt())
                            .setChance(chance.get()));
        }
    }
    // endregion
}
