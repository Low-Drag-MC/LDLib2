package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.gui.util.TextFormattingUtil;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.integration.xei.rei.LDLibREIPlugin;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.FluidHelper;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import dev.emi.emi.api.stack.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.w3c.dom.Element;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Accessors(chain = true)
@KJSBindings
@LDLRegister(name = "fluid-slot", group = "inventory", registry = "ldlib2:ui_element")
public class FluidSlot extends BindableUIElement<FluidStack> {
    @Configurable(name = "SlotStyle")
    public class SlotStyle extends Style {
        private static final Property<?>[] PROPERTIES = new Property[] {
                PropertyRegistry.HOVER_OVERLAY,
                PropertyRegistry.SLOT_OVERLAY,
                PropertyRegistry.SHOW_SLOT_OVERLAY_ONLY_EMPTY,
                PropertyRegistry.FILL_DIRECTION,
                PropertyRegistry.SHOW_FLUID_TOOLTIPS,
        };
        public SlotStyle() {
            super(FluidSlot.this);
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

        public FillDirection fillDirection() {
            return getValueSave(PropertyRegistry.FILL_DIRECTION);
        }

        public SlotStyle fillDirection(FillDirection fillDirection) {
            set(PropertyRegistry.FILL_DIRECTION, fillDirection);
            return this;
        }

        public boolean showFluidTooltips() {
            return getValueSave(PropertyRegistry.SHOW_FLUID_TOOLTIPS);
        }

        public SlotStyle showFluidTooltips(boolean showFluidTooltips) {
            set(PropertyRegistry.SHOW_FLUID_TOOLTIPS, showFluidTooltips);
            return this;
        }
    }

    public final Label amountLabel = new Label();
    @Getter
    private final SlotStyle slotStyle = new SlotStyle();
    @Getter @Setter
    private boolean allowClickFilled = true;
    @Getter @Setter
    private boolean allowClickDrained = true;
    // editor support
    @Configurable(name = "EditorFluidDisplay")
    private FluidStack editorFluidDisplay = FluidStack.EMPTY;
    @Configurable(name = "EditorAllowXEILookup")
    private boolean allowXEILookup = true;
    // runtime
    @Getter
    private FluidStack fluid = FluidStack.EMPTY;
    @Getter @Setter
    @Configurable(name = "Capacity")
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    private int capacity = 0;
    private final RPCEmitter clickEvent;

    @Nullable
    private IFluidHandler boundHandler;
    private int tankIndex;
    @Nullable
    private ISubscription fluidTankSubscription;

    public FluidSlot() {
        getLayout().width(18);
        getLayout().height(18);
        getLayout().paddingAll(1);
        getStyle().backgroundTexture(Sprites.RECT_DARK);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
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
        clickEvent = addRPCEvent(RPCEventBuilder.simple(Boolean.class, this::tryClickContainer));

        amountLabel.addClass("__fluid-slot_amount-label__");
        amountLabel.layout(layout -> layout.widthPercent(100).heightPercent(100));
        amountLabel.textStyle(textStyle -> textStyle
                .textAlignVertical(Vertical.BOTTOM)
                .textAlignHorizontal(Horizontal.RIGHT)
                .fontSize(4.5f)
        );
        amountLabel.bindDataSource(SupplierDataSource.of(this::getFluidAmountText));
        addChild(amountLabel);
        internalSetup();
    }


    public FluidSlot slotStyle(Consumer<SlotStyle> style) {
        style.accept(slotStyle);
        return this;
    }

    public FluidSlot bind(@Nullable IFluidHandler fluidTank, int tankIndex) {
        if (fluidTankSubscription != null) {
            fluidTankSubscription.unsubscribe();
        }
        boundHandler = fluidTank;
        if (boundHandler == null) return this;
        this.tankIndex = tankIndex;
        if (tankIndex < 0 || tankIndex >= boundHandler.getTanks()) throw new IllegalArgumentException("Invalid tank index: " + tankIndex);
        var fluidBinding = DataBindingBuilder.fluidStackS2C(() -> boundHandler.getFluidInTank(this.tankIndex)).build();
        var capacitySyncValue = DataBindingBuilder.intValS2C(() -> boundHandler.getTankCapacity(this.tankIndex))
                .remoteSetter(this::setCapacity).build().getSyncValue();

        bind(fluidBinding);
        addSyncValue(capacitySyncValue);
        fluidTankSubscription = () -> {
            unbind(fluidBinding);
            removeSyncValue(capacitySyncValue);
            fluidTankSubscription = null;
        };

        return this;
    }

    public FluidSlot xeiPhantom() {
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

    public FluidSlot xeiRecipeIngredient(IngredientIO io) {
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

    public FluidSlot xeiRecipeIngredient(IngredientIO io, Supplier<Stream<FluidStack>> allPossibleFluids) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeIngredient(this, io, allPossibleFluids);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeIngredient(this, io, allPossibleFluids);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeIngredient(this, io, allPossibleFluids);
        }
        return this;
    }

    public FluidSlot xeiRecipeSlot() {
        return xeiRecipeSlot(IngredientIO.NONE, 1);
    }

    public FluidSlot xeiRecipeSlot(IngredientIO io, float chance) {
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

    public FluidSlot xeiRecipeSlot(IngredientIO io, float chance, int amount, Supplier<Stream<FluidStack>> allPossibleFluids) {
        if (LDLib2.isJeiLoaded()) {
            JEISupport.recipeSlot(this, allPossibleFluids);
        }
        if (LDLib2.isReiLoaded()) {
            REISupport.recipeSlot(this, io, allPossibleFluids);
        }
        if (LDLib2.isEmiLoaded()) {
            EMISupport.recipeSlot(this, () -> chance, () -> amount, allPossibleFluids);
        }
        return this;
    }

    private void tryClickContainer(boolean isShiftKeyDown) {
        if (boundHandler == null) return;
        if (tankIndex < 0 || tankIndex >= boundHandler.getTanks()) return;
        var mui = getModularUI();
        if (mui == null || mui.getMenu() == null) return;
        var player = mui.player;
        if (player == null) return;
        var menu = mui.getMenu();
        var carried = menu.getCarried();
        var handler = FluidUtil.getFluidHandler(carried);
        if (handler.isEmpty()) return;
        int maxAttempts = isShiftKeyDown ? carried.getCount() : 1;
        var initialFluid = boundHandler.getFluidInTank(tankIndex);
        if (allowClickFilled && initialFluid.getAmount() > 0) {
            var performedFill = false;
            for (int i = 0; i < maxAttempts; i++) {
                var result = FluidUtil.tryFillContainer(carried, boundHandler, Integer.MAX_VALUE, null, false);
                if (!result.isSuccess()) break;
                ItemStack remainingStack = FluidUtil.tryFillContainer(carried, boundHandler, Integer.MAX_VALUE, null, true).getResult();
                carried.shrink(1);
                performedFill = true;
                if (!remainingStack.isEmpty() && !player.addItem(remainingStack)) {
                    Block.popResource(player.level(), player.getOnPos(), remainingStack);
                    break;
                }
            }
            if (performedFill) {
                SoundEvent soundevent = FluidHelper.getFillSound(initialFluid);
                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                menu.setCarried(carried);
                return;
            }
        }

        if (allowClickDrained) {
            var performedEmptying = false;
            for (int i = 0; i < maxAttempts; i++) {
                var result = FluidUtil.tryEmptyContainer(carried, boundHandler, Integer.MAX_VALUE, null, false);
                if (!result.isSuccess()) break;
                ItemStack remainingStack = FluidUtil.tryEmptyContainer(carried, boundHandler, Integer.MAX_VALUE, null, true).getResult();
                carried.shrink(1);
                performedEmptying = true;
                if (!remainingStack.isEmpty() && !player.getInventory().add(remainingStack)) {
                    Block.popResource(player.level(), player.getOnPos(), remainingStack);
                    break;
                }
            }
            var filledFluid = boundHandler.getFluidInTank(tankIndex);
            if (performedEmptying) {
                SoundEvent soundevent = FluidHelper.getEmptySound(filledFluid);
                if (soundevent != null) {
                    player.level().playSound(null, player.position().x, player.position().y + 0.5, player.position().z, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                menu.setCarried(carried);
            }
        }
    }


    protected void onMouseDown(UIEvent event) {
        clickEvent.send(event.isShiftDown());
    }

    public FluidSlot  setFluid(FluidStack fluid) {
        return setValue(fluid, true);
    }

    public FluidSlot setFluid(FluidStack fluid, boolean notify) {
        return setValue(fluid, notify);
    }

    public List<Component> getFullTooltipTexts() {
        var tooltips = new ArrayList<Component>();
        if (slotStyle.showFluidTooltips()) {
            var fluidStack = getFluid();
            capacity = Math.max(capacity, fluidStack.getAmount());
            if (!fluidStack.isEmpty()) {
                tooltips.add(FluidHelper.getDisplayName(fluidStack));
                tooltips.add(Component.translatable("ldlib.fluid.amount", fluidStack.getAmount(), capacity).append(" " + FluidHelper.getUnit()));
                tooltips.add(Component.translatable("ldlib.fluid.temperature", FluidHelper.getTemperature(fluidStack)));
                tooltips.add(Component.translatable(FluidHelper.isLighterThanAir(fluidStack) ? "ldlib.fluid.state_gas" : "ldlib.fluid.state_liquid"));
            } else {
                tooltips.add(Component.translatable("ldlib.fluid.empty"));
                tooltips.add(Component.translatable("ldlib.fluid.amount", 0, capacity).append(" " + FluidHelper.getUnit()));
            }
        }
        tooltips.addAll(getStyle().tooltips().asList());
        return tooltips;
    }

    public Component getFluidAmountText() {
        var renderedFluid = getValue();
        if (renderedFluid.isEmpty()) return Component.empty();
        return Component.literal(TextFormattingUtil.formatLongToCompactStringBuckets(renderedFluid.getAmount(), 3) + "B");
    }

    protected void onHoverTooltips(UIEvent event) {
        var item = getValue();
        if (item.isEmpty()) return;
        event.hoverTooltips = new HoverTooltips(getFullTooltipTexts(), null, null, null);
    }

    @Override
    public FluidStack getValue() {
        return fluid;
    }

    @Override
    public FluidSlot setValue(@Nullable FluidStack value, boolean notify) {
        if (value == null) value = FluidStack.EMPTY;
        if (FluidStack.matches(value, fluid)) return this;
        this.fluid = value;
        if (notify) notifyListeners();
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var renderedFluid = getValue();
        var hovered = isHover() || isSelfOrChildHover();
        var drawSlotOverlay = slotStyle.showSlotOverlayOnlyEmpty() || !renderedFluid.isEmpty();

        if (renderedFluid.isEmpty() && !hovered && !drawSlotOverlay) return;

        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        if (renderedFluid.isEmpty() || !slotStyle.showSlotOverlayOnlyEmpty()) {
            drawSlotOverlay(guiContext, contentX, contentY, contentWidth, contentHeight);
        }

        if (!renderedFluid.isEmpty()) {
            drawFluid(guiContext, renderedFluid, contentX, contentY, contentWidth, contentHeight);
        }

        if (hovered) {
            drawHover(guiContext, contentX, contentY, contentWidth, contentHeight);
        }
    }

    protected void drawSlotOverlay(GUIContext guiContext, float contentX, float contentY, float contentWidth, float contentHeight) {
        guiContext.drawTexture(slotStyle.slotOverlay(), contentX, contentY, contentWidth, contentHeight);
    }

    protected void drawFluid(GUIContext guiContext, FluidStack renderedFluid, float contentX, float contentY, float contentWidth, float contentHeight) {
        var fillDirection = slotStyle.fillDirection();
        double progress = renderedFluid.getAmount() * 1.0 / Math.max(Math.max(renderedFluid.getAmount(), capacity), 1);
        float drawnU = (float) fillDirection.getDrawnU(progress);
        float drawnV = (float) fillDirection.getDrawnV(progress);
        float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
        float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
        DrawerHelper.drawFluidForGui(guiContext.graphics, renderedFluid,
                contentX + drawnU * contentWidth,
                contentY + drawnV * contentHeight,
                contentWidth * drawnWidth,
                contentHeight * drawnHeight, -1);
    }

    protected void drawHover(GUIContext guiContext, float contentX, float contentY, float contentWidth, float contentHeight) {
        guiContext.drawTexture(slotStyle.hoverOverlay(), contentX, contentY, contentWidth, contentHeight);
    }


    /// Editor Support
    @ConfigSetter(field = "editorFluidDisplay")
    private void setEditorFluidDisplay(FluidStack fluidStack) {
        this.editorFluidDisplay = fluidStack;
        setValue(fluidStack, false);
        amountLabel.setValue(getFluidAmountText());
    }

    @SkipPersistedValue(field = "editorFluidDisplay")
    private boolean skipEditorFluidDisplay(FluidStack fluid) {
        return fluid == FluidStack.EMPTY;
    }

    @ConfigSetter(field = "allowXEILookup")
    private void setAllowXEILookup(boolean allowXEILookup) {
        this.allowXEILookup = allowXEILookup;
    }

    @SkipPersistedValue(field = "allowXEILookup")
    private boolean skipAllowXEILookup(boolean allowXEILookup) {
        return allowXEILookup;
    }
    
    @SkipPersistedValue(field = "capacity")
    private boolean skipCapacity(int capacity) {
        return capacity == 0;
    }

    @Override
    public void beforeDeserialize() {
        super.beforeDeserialize();
        this.editorFluidDisplay = FluidStack.EMPTY;
    }

    @Override
    public void afterDeserialize() {
        super.afterDeserialize();
        if (!editorFluidDisplay.isEmpty()) {
            setValue(editorFluidDisplay, false);
        }
    }

    @Override
    public void loadXml(Element element) {
        // capacity
        if (element.hasAttribute("capacity")) {
            setCapacity(XmlUtils.getAsInt(element, "capacity", capacity));
        }
        // allow xei lookup
        if (element.hasAttribute("allow-xei-lookup")) {
            setAllowXEILookup(XmlUtils.getAsBoolean(element, "allow-xei-Lookup", allowXEILookup));
        }
        // fluid display
        var fluid = XmlUtils.getFluidStack(element);
        if (fluid != FluidStack.EMPTY) {
            setEditorFluidDisplay(fluid);
        }

        super.loadXml(element);
    }

    // region XEI Support
    public static class JEISupport {
        public static void clickableIngredient(FluidSlot fluidSlot) {
            LDLibJEIPlugin.clickableIngredient(fluidSlot, () -> {
                if (!fluidSlot.allowXEILookup) return null;
                var current = fluidSlot.getValue();
                if (current.isEmpty()) return null;
                return LDLibJEIPlugin.createTypedIngredient(NeoForgeTypes.FLUID_STACK, current)
                        .orElse(null);
            });
        }

        public static void ghostIngredient(FluidSlot fluidSlot) {
            LDLibJEIPlugin.ghostIngredient(fluidSlot, NeoForgeTypes.FLUID_STACK,
                    ingredient -> true,
                    fluidSlot::setValue);
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io) {
            recipeIngredient(fluidSlot, io, () -> Stream.of(fluidSlot.getFluid()));
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibJEIPlugin.recipeIngredient(fluidSlot, io, () -> allPossibleFluids.get()
                    .map(fluidStack -> LDLibJEIPlugin.createTypedIngredient(NeoForgeTypes.FLUID_STACK, fluidStack))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList()));
        }

        public static void recipeSlot(FluidSlot fluidSlot) {
            recipeSlot(fluidSlot, () -> Stream.of(fluidSlot.getFluid()));
        }

        public static void recipeSlot(FluidSlot fluidSlot, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibJEIPlugin.recipeSlot(fluidSlot, () -> {
                var fluid = fluidSlot.getValue();
                return fluid.isEmpty() ? null : LDLibJEIPlugin
                        .createTypedIngredient(NeoForgeTypes.FLUID_STACK, fluidSlot.getFluid())
                        .orElse(null);
            }, () -> allPossibleFluids.get().map(fluid -> LDLibJEIPlugin.createTypedIngredient(NeoForgeTypes.FLUID_STACK, fluid).orElseThrow()).collect(Collectors.toList()));
        }
    }

    public static class REISupport {
        public static void focusedStack(FluidSlot fluidSlot) {
            LDLibREIPlugin.focusedStack(fluidSlot, () -> {
                if (!fluidSlot.allowXEILookup) return null;
                var fluid = fluidSlot.getValue();
                if (fluid.isEmpty()) return null;
                return EntryStacks.of(FluidStackHooksForge.fromForge(fluid));
            });
        }

        public static void draggableStackBounds(FluidSlot fluidSlot) {
            LDLibREIPlugin.draggableStackBounds(fluidSlot,
                    VanillaEntryTypes.FLUID,
                    stack -> true);
        }

        public static void acceptDraggableStack(FluidSlot fluidSlot) {
            LDLibREIPlugin.acceptDraggableStack(fluidSlot,
                    VanillaEntryTypes.FLUID,
                    stack -> true,
                    stack -> fluidSlot.setValue(FluidStackHooksForge.toForge(stack.getValue())));
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io) {
            recipeIngredient(fluidSlot, io, () -> Stream.of(fluidSlot.getFluid()));
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibREIPlugin.recipeIngredient(fluidSlot, io, () -> allPossibleFluids.get()
                    .map(fluidStack -> EntryIngredients.of(FluidStackHooksForge.fromForge(fluidStack)))
                    .toList()
            );
        }

        public static void recipeSlot(FluidSlot fluidSlot, IngredientIO io) {
            recipeSlot(fluidSlot, io, () -> Stream.of(fluidSlot.getFluid()));
        }

        public static void recipeSlot(FluidSlot fluidSlot, IngredientIO io, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibREIPlugin.recipeSlot(fluidSlot, io,
                    () -> EntryStacks.of(FluidStackHooksForge.fromForge(fluidSlot.getValue())),
                    () -> allPossibleFluids.get().map(fluid -> EntryStacks.of(FluidStackHooksForge.fromForge(fluid))).collect(Collectors.toList()));
        }
    }

    public static class EMISupport {
        public static void stackProvider(FluidSlot fluidSlot) {
            LDLibEMIPlugin.stackProvider(fluidSlot, () -> {
                if (!fluidSlot.allowXEILookup) return null;
                var fluid = fluidSlot.getValue();
                if (fluid.isEmpty()) return null;
                return new EmiStackInteraction(EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()), null, false);
            });
        }

        public static void renderDragHandler(FluidSlot fluidSlot) {
            LDLibEMIPlugin.renderDragHandler(fluidSlot, dragged -> dragged instanceof FluidEmiStack);
        }

        public static void dropStackHandler(FluidSlot fluidSlot) {
            LDLibEMIPlugin.dropStackHandler(fluidSlot,
                    dragged -> dragged instanceof FluidEmiStack,
                    dragged -> {
                        if (dragged instanceof FluidEmiStack fluid) {
                            var fluidStack = new FluidStack(
                                    ((Fluid) fluid.getKey()).builtInRegistryHolder(),
                                    Math.max(1000, (int) fluid.getAmount()),
                                    fluid.getComponentChanges());
                            fluidSlot.setValue(fluidStack);
                        }
                    });
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io) {
            recipeIngredient(fluidSlot, io, () -> Stream.of(fluidSlot.getFluid()));
        }

        public static void recipeIngredient(FluidSlot fluidSlot, IngredientIO io, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibEMIPlugin.recipeIngredient(fluidSlot, io, () -> allPossibleFluids.get()
                    .map(fluid -> EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()))
                    .collect(Collectors.toList())
            );
        }

        public static void recipeSlot(FluidSlot fluidSlot, float chance) {
            LDLibEMIPlugin.recipeSlot(fluidSlot, () -> {
                var fluid = fluidSlot.getValue();
                return EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()).setChance(chance);
            });
        }

        public static void recipeSlot(FluidSlot fluidSlot, Supplier<Float> chance, IntSupplier amount, Supplier<Stream<FluidStack>> allPossibleFluids) {
            LDLibEMIPlugin.recipeSlot(fluidSlot, () ->
                    new ListEmiIngredient(
                            allPossibleFluids.get().map(fluid -> EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()))
                                    .map(e -> e.setChance(chance.get())).collect(Collectors.toList()), amount.getAsInt())
                            .setChance(chance.get()));
        }
    }
    // endregion
}
