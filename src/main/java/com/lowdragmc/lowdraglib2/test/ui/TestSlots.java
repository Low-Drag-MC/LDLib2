package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.iventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaWrap;

import javax.annotation.ParametersAreNonnullByDefault;

@LDLRegister(name="ui_slots", registry = "menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSlots implements IMenuTest {
    private final FluidTank fluidTank = new FluidTank(2000);
    private final ItemStackHandler itemHandler = new ItemStackHandler(10);

    public TestSlots() {
        fluidTank.setFluid(new FluidStack(Fluids.WATER, 1400));
        itemHandler.setStackInSlot(0, Items.STONE.getDefaultInstance().copyWithCount(10));
    }

    @Override
    public ModularUI createUI(Player player) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(400);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root");
        root.getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(
                new UIElement().layout(layout -> {
                    layout.setFlexDirection(YogaFlexDirection.ROW);
                    layout.setWrap(YogaWrap.WRAP);
                }).addChildren(
                        new ItemSlot(),
                        new ItemSlot().setItem(Items.APPLE.getDefaultInstance()),
                        new ItemSlot().setItem(Items.CHEST.getDefaultInstance().copyWithCount(64)),
                        new FluidSlot(),
                        new FluidSlot().setFluid(new FluidStack(Fluids.LAVA, 1000)),
                        new FluidSlot().setFluid(new FluidStack(Fluids.WATER, 1000))
                ),
                new InventorySlots(),
                new ItemSlot().bind(itemHandler, 0),
                new FluidSlot().bind(fluidTank, 0)
        );
        return new ModularUI(UI.of(root), player);
    }
}
