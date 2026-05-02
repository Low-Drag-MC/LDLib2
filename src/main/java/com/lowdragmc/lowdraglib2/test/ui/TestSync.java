package com.lowdragmc.lowdraglib2.test.ui;

import com.google.common.reflect.TypeToken;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.slot.ItemHandlerSlot;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.SyncStrategy;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.function.Consumers;
import org.appliedenergistics.yoga.YogaEdge;

import org.jetbrains.annotations.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LDLRegister(name="ui_sync", registry = "ldlib2:menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSync implements IMenuTest {
    private final FluidTank fluidTank = new FluidTank(2000);
    private final FluidTank phantomTank = new FluidTank(2000);
    private final FluidTank phantomTank2 = new FluidTank(2000);
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
            layout.width(250);
            layout.height(400);
            layout.paddingAll(10);
        }).setId("root");
        root.getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(
                new UIElement().layout(layout -> {
                    layout.flexDirection(FlexDirection.ROW);
                    layout.wrap(FlexWrap.WRAP);
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
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        new ItemSlot().xeiPhantom().bind(DataBindingBuilder.itemStack(
                                () -> itemHandler.getStackInSlot(3),
                                itemStack -> itemHandler.setStackInSlot(3, itemStack)
                        ).build()),
                        new ItemSlot().xeiPhantom().bind(DataBindingBuilder.itemStack(
                                () -> itemHandler.getStackInSlot(4),
                                itemStack -> itemHandler.setStackInSlot(4, itemStack)
                        ).build())
                ),
                new FluidSlot().bind(fluidTank, 0),
                new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW)).addChildren(
                        new FluidSlot().xeiPhantom().bind(DataBindingBuilder.fluidStack(phantomTank::getFluid, phantomTank::setFluid).build()),
                        new FluidSlot().xeiPhantom().bind(DataBindingBuilder.fluidStack(phantomTank2::getFluid, phantomTank2::setFluid).build())
                ),
                new Button().selfCall( self -> {
                    var button = (Button)self;
                    var s2cEvent = button.addRPCEvent(RPCEventBuilder.simple(Fluid.class, fluid -> {
                        // execute from client
                        assert (LDLib2.isRemote());
                        button.setText(fluid.getFluidType().getDescription());
                    }));
                    button.addServerEventListener(UIEvents.MOUSE_DOWN, e -> {
                        if (fluidTank.getFluid().getFluid() == Fluids.WATER) {
                            fluidTank.setFluid(new FluidStack(Fluids.LAVA, fluidTank.getFluid().getAmount()));
                            s2cEvent.send(Fluids.LAVA); // send to client
                        } else {
                            fluidTank.setFluid(new FluidStack(Fluids.WATER, fluidTank.getFluid().getAmount()));
                            s2cEvent.send(Fluids.WATER); // send to client
                        }
                    });
                }),
                new Button().setOnServerClick(e -> {
                    e.currentElement.sendMessage("test_message", TagBuilder.compound().add("text", "Message from server!").build());
                }).onMessage("test_message", (button, message) -> {
                    assert (LDLib2.isRemote());
                    ((Button)button).setText(message.getString("text"));
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
                    public String resultText(Block value) {
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

        var serverCandidates1 = List.of("a", "b", "c", "d");
        var selector1 = new Selector<String>();
        selector1.addChild(
                // a placeholder element value to sync candidates, it won't affect layout
                new BindableValue<String[]>().bind(DataBindingBuilder.create(
                        () -> serverCandidates1.toArray(String[]::new), Consumers.nop())
                        .c2sStrategy(SyncStrategy.NONE) // only s -> c
                        .remoteSetter(candidates -> {
                            selector1.setCandidates(Arrays.stream(candidates).toList());
                        })
                        .build()
                )
        );

        var serverCandidates2 = List.of("a", "b", "c", "d");
        var clientCandidates = new ArrayList<String>();
        var selector2 = new Selector<String>();
        Type type = new TypeToken<List<String>>(){}.getType();
        selector2.addChild(
                // a placeholder element value to sync candidates, it won't affect layout
                new BindableValue<List<String>>().bind(DataBindingBuilder.create(
                                () -> LDLib2.isRemote() ? clientCandidates : serverCandidates2, Consumers.nop())
                        .syncType(type)
                        .initialValue(LDLib2.isRemote() ? clientCandidates : serverCandidates2)
                        .c2sStrategy(SyncStrategy.NONE) // only s -> c
                        .remoteSetter(selector2::setCandidates)
                        .build()
                )
        );

        root.addChildren(selector1, selector2);

        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }
}
