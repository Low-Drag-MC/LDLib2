package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@LDLRegister(name="doc", registry = "ldlib2:menu_test")
@NoArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestMenuDoc implements IMenuTest {
    private boolean bool = true;
    private String string = "hello";
    private float number = 0.5f;

    @Override
    public ModularUI createUI(Player player) {
        return step2(player);
    }

    private ModularUI step1(Player player) {
        // create a root element
        var root = new UIElement();
        root.addChildren(
                new Label().setText("Menu UI"),

                new InventorySlots()
        ).addClass("panel_bg");

        var ui = UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC));
        return new ModularUI(ui, player);
    }

    private ModularUI step2(Player player) {
        ItemStackHandler itemHandler = new ItemStackHandler(2);
        FluidTank fluidTank = new FluidTank(2000);
        // create a root element
        var root = new UIElement();
        root.addChildren(
                // add a label to display text
                new Label().setText("Data Between Screen and Menu"),
                // bind storage to slots
                new UIElement().addChildren(
                        new ItemSlot().bind(itemHandler, 0),
                        new ItemSlot().bind(new ItemHandlerSlot(itemHandler, 1).setCanTake(p -> false)),
                        new FluidSlot().bind(fluidTank, 0)
                ).layout(l -> l.gapAll(2).flexDirection(FlexDirection.ROW)),
                // bind value to the components
                new UIElement().addChildren(
                        new Switch().bind(DataBindingBuilder.bool(() -> bool, value -> bool = value).build()),
                        new TextField().bind(DataBindingBuilder.string(() -> string, value -> string = value).build()),
                        new Scroller.Horizontal().bind(DataBindingBuilder.floatVal(() -> number, value -> number = value).build())
                                .layout(l -> l.widthPercent(100)), // taffy bug?
                        // read-only (s->c), always get data from the server and display on the client
                        new Label().bind(DataBindingBuilder.componentS2C(() -> Component.literal("s->c only: ")
                                .append(Component.literal(String.valueOf(bool)).withStyle(ChatFormatting.AQUA)).append(" ")
                                .append(Component.literal(string).withStyle(ChatFormatting.RED)).append(" ")
                                .append(Component.literal("%.2f".formatted(number)).withStyle(ChatFormatting.YELLOW)))
                                .build())
                ).layout(l -> l.gapAll(2)),
                // trigger ui events on the server side
                new Button().addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (fluidTank.getFluid().getFluid() == Fluids.WATER) {
                        fluidTank.setFluid(new FluidStack(Fluids.LAVA, 1000));
                    } else {
                        fluidTank.setFluid(new FluidStack(Fluids.WATER, 1000));
                    }
                }),
                // you could also use button.setOnServerClick(e -> { ... })
                new InventorySlots()
        );
        root.addClass("panel_bg");

        // pass the player to the Modular UI
        return new ModularUI(UI.of(root, StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MODERN)), player);
    }

}
