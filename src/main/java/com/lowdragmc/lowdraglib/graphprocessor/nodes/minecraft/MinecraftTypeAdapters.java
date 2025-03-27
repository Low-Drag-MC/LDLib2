package com.lowdragmc.lowdraglib.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

@LDLRegister(name = "minecraft_type_adapters", group = "type_adapter")
public class MinecraftTypeAdapters implements TypeAdapter.ITypeAdapter {

    @Override
    public void onRegister() {
        TypeAdapter.registerAdapter(Item.class, ItemStack.class, o -> o == null ? null : new ItemStack(o));
        TypeAdapter.registerAdapter(ItemStack.class, Item.class, o -> o == null ? null : o.getItem());
        TypeAdapter.registerAdapter(Fluid.class, FluidStack.class, o -> o == null ? null : new FluidStack(o, 1000));
        TypeAdapter.registerAdapter(FluidStack.class, Fluid.class, o -> o == null ? null : o.getFluid());
        TypeAdapter.registerAdapter(Block.class, BlockState.class, o -> o == null ? null : o.defaultBlockState());
        TypeAdapter.registerAdapter(BlockState.class, Block.class, o -> o == null ? null : o.getBlock());
        TypeAdapter.registerAdapter(ResourceLocation.class, String.class, o -> o == null ? null : o.toString());
        TypeAdapter.registerAdapter(String.class, ResourceLocation.class, o -> o == null ? null : ResourceLocation.tryParse(o));
    }
}
