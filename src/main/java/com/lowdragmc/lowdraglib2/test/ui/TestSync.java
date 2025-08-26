package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SearchComponent;
import com.lowdragmc.lowdraglib2.gui.ui.elements.iventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaWrap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@LDLRegister(name="ui_sync", registry = "menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSync implements IMenuTest {
    private final FluidTank fluidTank = new FluidTank(2000);
    private final ItemStackHandler itemHandler = new ItemStackHandler(10);
    @Nullable
    private Block block = null;

    public TestSync() {
        fluidTank.setFluid(new FluidStack(Fluids.WATER, 1400));
        itemHandler.setStackInSlot(0, Items.STONE.getDefaultInstance().copyWithCount(10));
        itemHandler.setStackInSlot(1, Items.BAMBOO.getDefaultInstance().copyWithCount(32));
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
                new ItemSlot().bind(new ItemHandlerSlot(itemHandler, 1).setCanTake(p -> false)),
                new ItemSlot().bind(new ItemHandlerSlot(itemHandler, 2).setCanPlace(itemStack -> itemStack.is(Items.STONE))),
                new FluidSlot().bind(fluidTank, 0),
                new Button().addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                    if (fluidTank.getFluid().getFluid() == Fluids.WATER) {
                        fluidTank.setFluid(new FluidStack(Fluids.LAVA, fluidTank.getFluid().getAmount()));
                    } else {
                        fluidTank.setFluid(new FluidStack(Fluids.WATER, fluidTank.getFluid().getAmount()));
                    }
                }),
                new SearchComponent<>(new SearchComponent.ISearchUI<Block>() {
                    @Override
                    public void search(String word, IResultHandler<Block> searchHandler) {
                        var lowerWord = word.toLowerCase();
                        for (var key : BuiltInRegistries.BLOCK.keySet()) {
                            if (Thread.currentThread().isInterrupted()) return;
                            if (key.toString().toLowerCase().contains(lowerWord)) {
                                searchHandler.acceptResult(BuiltInRegistries.BLOCK.get(key));
                            }
                        }
                    }

                    @Override
                    @Nonnull
                    public String resultDisplay(@NotNull Block value) {
                        return BuiltInRegistries.BLOCK.getKey(value).toString();
                    }

                    @Override
                    public void onResultSelected(@Nullable Block value) {
                        block = value;
                    }
                }).setCandidateUIProvider(UIElementProvider.iconText(
                        block -> new ItemStackTexture(block.asItem()),
                        block -> Component.translatable(block.getDescriptionId())
                )).setSearchOnServer(Block[].class).bind(DataBindingBuilder
                        .create(() -> block, b -> block = b).syncType(Block.class).build())
        );
        return new ModularUI(UI.of(root), player);
    }
}
