package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEvent;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.style.UIStyleRegistries;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.StyleValue;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.utils.FluidHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Accessors(chain = true)
@LDLRegister(name = "fluid_slot", registry = "ldlib2:ui_element")
public class FluidSlot extends BindableUIElement<FluidStack> {
    public final static SpriteTexture FLUID_SLOT_TEXTURE =SpriteTexture.of("ldlib2:textures/gui/fluid_slot.png")
            .setSprite(0, 0, 18, 18).setBorder(1, 1, 1, 1);

    @Accessors(chain = true, fluent = true)
    public static class SlotStyle extends Style {
        @Getter
        @Setter
        @Configurable(name = "hoverOverlay")
        private IGuiTexture hoverOverlay = new ColorRectTexture(0x80FFFFFF);
        @Getter @Setter
        @Configurable(name = "fillDirection")
        private FillDirection fillDirection = FillDirection.DOWN_TO_UP;

        @Getter @Setter
        @Configurable(name = "showFluidTooltips")
        private boolean showFluidTooltips = true;

        public SlotStyle(FluidSlot holder) {
            super(holder);
        }

        @Override
        public void applyStyles(Map<String, StyleValue<?>> values) {
            super.applyStyles(values);

            UIStyleRegistries.HOVER_OVERLAY.parse(values).ifPresent(this::hoverOverlay);
            UIStyleRegistries.FILL_DIRECTION.parse(values).ifPresent(this::fillDirection);
            UIStyleRegistries.SHOW_FLUID_TOOLTIPS.parse(values).ifPresent(this::showFluidTooltips);
        }
    }

    @Getter
    @Configurable(name = "slotStyle", subConfigurable = true)
    private final SlotStyle slotStyle = new SlotStyle(this);
    @Getter @Setter
    private boolean allowClickFilled = true;
    @Getter @Setter
    private boolean allowClickDrained = true;

    // runtime
    @Getter
    private FluidStack fluid = FluidStack.EMPTY;
    @Getter @Setter
    private int capacity = 0;
    private final RPCEvent clickEvent;

    @Nullable
    private IFluidHandler boundHandler;
    private int tankIndex;
    @Nullable
    private ISubscription fluidTankSubscription;

    public FluidSlot() {
        getLayout().setWidth(18);
        getLayout().setHeight(18);
        getLayout().setPadding(YogaEdge.ALL, 1);
        getStyle().backgroundTexture(FLUID_SLOT_TEXTURE);
        addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        addEventListener(UIEvents.MOUSE_DOWN, this::onMouseDown);
        clickEvent = RPCEventBuilder.simple(Boolean.class, this::tryClickContainer);
        addRPCEvent(clickEvent);
    }

    public FluidSlot slotStyle(Consumer<SlotStyle> style) {
        style.accept(slotStyle);
        onStyleChanged();
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
        sendEvent(clickEvent, event.isShiftDown());
    }

    public FluidSlot setFluid(FluidStack fluid) {
        return setValue(fluid, true);
    }

    public FluidSlot setFluid(FluidStack fluid, boolean notify) {
        return setValue(fluid, notify);
    }

    public List<Component> getFullTooltipTexts() {
        var tooltips = new ArrayList<Component>();
        if (slotStyle.showFluidTooltips) {
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
        tooltips.addAll(getStyle().tooltips());
        return tooltips;
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
        var hovered = isHover() || isChildHover();
        if (renderedFluid.isEmpty() && !hovered) return;
        var contentX = getContentX();
        var contentY = getContentY();
        var contentWidth = getContentWidth();
        var contentHeight = getContentHeight();

        if (!renderedFluid.isEmpty()) {
            var fillDirection = slotStyle.fillDirection;
            double progress = renderedFluid.getAmount() * 1.0 / Math.max(Math.max(renderedFluid.getAmount(), capacity), 1);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            DrawerHelper.drawFluidForGui(guiContext.graphics, renderedFluid,
                    contentX + drawnU * contentWidth,
                    contentY + drawnV * contentHeight,
                    contentWidth * drawnWidth,
                    contentHeight * drawnHeight);
        }

        if (hovered) {
            guiContext.drawTexture(slotStyle.hoverOverlay, contentX, contentY, contentWidth, contentHeight);
        }
    }
}
