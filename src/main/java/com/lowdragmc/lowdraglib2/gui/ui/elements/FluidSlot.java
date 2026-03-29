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
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.FluidHelper;
import com.lowdragmc.lowdraglib2.utils.XmlUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import dev.architectury.fluid.FluidStack;
import com.lowdragmc.lowdraglib2.utils.fluids.IFluidHandler;
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
    private FluidStack editorFluidDisplay = FluidStack.empty();
    @Configurable(name = "EditorAllowXEILookup")
    private boolean allowXEILookup = true;
    // runtime
    @Getter
    private FluidStack fluid = FluidStack.empty();
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


    private void tryClickContainer(boolean isShiftKeyDown) {
        // TODO: Port NeoForge FluidUtil logic to Fabric for GUI fluid container clicking.
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
            capacity = (int) Math.max(capacity, fluidStack.getAmount());
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
        if (value == null) value = FluidStack.empty();
        if (value.isFluidEqual(fluid)) return this;
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
        return fluid == FluidStack.empty();
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
        this.editorFluidDisplay = FluidStack.empty();
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
        if (fluid != FluidStack.empty()) {
            setEditorFluidDisplay(fluid);
        }

        super.loadXml(element);
    }

}
