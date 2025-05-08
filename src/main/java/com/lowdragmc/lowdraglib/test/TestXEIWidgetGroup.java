package com.lowdragmc.lowdraglib.test;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.integration.jei.IngredientIO;
import com.lowdragmc.lowdraglib.misc.TagOrCycleFluidTransfer;
import com.lowdragmc.lowdraglib.misc.TagOrCycleItemStackTransfer;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class TestXEIWidgetGroup extends WidgetGroup {
    public TestXEIWidgetGroup() {
        super(0, 0, 170, 60);
        setClientSideWidget();
        var input1 = new SlotWidget(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.APPLE, 10))), 0, 20, 20, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT)
                .setXEIChance(0);

        List<Either<List<Pair<TagKey<Item>, Integer>>, List<ItemStack>>> itemsList = List.of(Either.left(List.of(Pair.of(ItemTags.AXES, 5), Pair.of(ItemTags.HOES, 5))));
        List<Either<List<Pair<TagKey<Item>, Integer>>, List<ItemStack>>> itemsList2 = List.of(Either.left(List.of(Pair.of(ItemTags.AXES, 5))));
        var input2 = new SlotWidget(new TagOrCycleItemStackTransfer(itemsList), 0, 20, 0, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT);

        var input3 = new SlotWidget(new TagOrCycleItemStackTransfer(itemsList2), 0, 40, 0, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT)
                .setXEIChance(0);


        var input4 = new SlotWidget(new ItemStackHandler(), 0, 40, 20, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT);

        var output = new SlotWidget(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND, 23))), 0, 130, 20, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT);

        var both = new SlotWidget(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, new ItemStack(Items.ANDESITE, 23))), 0, 60, 20, false, false)
                .setBackgroundTexture(SlotWidget.ITEM_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.BOTH);

        FluidTank tank = new FluidTank(1000);
        tank.setFluid(new FluidStack(Fluids.WATER, 1000));
        var inputFluid = new TankWidget(tank, 20, 40, 20, 20, false, false)
                .setBackground(TankWidget.FLUID_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.INPUT);

        tank = new FluidTank(1000);
        tank.setFluid(new FluidStack(Fluids.LAVA, 1000));
        var outputFluid = new TankWidget(tank, 130, 40, 20, 20, false, false)
                .setBackground(TankWidget.FLUID_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.OUTPUT)
                .setXEIChance(0.01f)
                .setOnAddedTooltips((widget, tooltips) -> {
                    tooltips.add(Component.literal("test tooltip").withStyle(ChatFormatting.GREEN));
                })
                .setOverlay((graphics, mouseX, mouseY, x, y, width, height, partialTick) -> {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 400);
                    graphics.pose().scale(0.5f, 0.5f, 1);

                    String s = String.format("%.2f", 0.01f) + "%";
                    int color = 0xFFFF00;
                    Font fontRenderer = Minecraft.getInstance().font;
                    graphics.drawString(fontRenderer, s, (int) ((x + (width / 3f)) * 2 - fontRenderer.width(s) + 23), (int) ((y + (height / 3f) + 6) * 2 - height), color, true);

                    graphics.pose().popPose();
                });

        List<Either<List<Pair<TagKey<Fluid>, Integer>>, List<FluidStack>>> fluidList = List.of(Either.left(List.of(Pair.of(FluidTags.WATER, 100), Pair.of(FluidTags.LAVA, 1000))));
        List<Either<List<Pair<TagKey<Fluid>, Integer>>, List<FluidStack>>> fluidList2 = List.of(Either.left(List.of(Pair.of(FluidTags.WATER, 2333))));
        var catalystFluid = new TankWidget(new TagOrCycleFluidTransfer(fluidList), 0, 110, 40, 20, 20, false, false)
                .setBackground(TankWidget.FLUID_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.CATALYST)
                .setXEIChance(0.01f);

        var inputFluid2 = new TankWidget(new TagOrCycleFluidTransfer(fluidList2), 0, 110, 20, 20, 20, false, false)
                .setBackground(TankWidget.FLUID_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.CATALYST)
                .setXEIChance(0f);

        var inputFluid3 = new TankWidget(new TagOrCycleFluidTransfer(fluidList), 0, 90, 20, 20, 40, false, false)
                .setBackground(TankWidget.FLUID_SLOT_TEXTURE)
                .setIngredientIO(IngredientIO.CATALYST);

        addWidget(input1);
        addWidget(input2);
        addWidget(input3);
        addWidget(input4);
        addWidget(output);
        addWidget(both);
        addWidget(inputFluid);
        addWidget(outputFluid);
        addWidget(catalystFluid);
        addWidget(inputFluid2);
        addWidget(inputFluid3);
    }
}
