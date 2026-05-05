package com.lowdragmc.lowdraglib2.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.neoforged.neoforge.fluids.FluidStack;

public final class FluidHelperClient {
    private FluidHelperClient() {
    }

    public static FluidModel getFluidModel(FluidStack fluidStack) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidStack.getFluid().defaultFluidState());
    }

    public static int getColor(FluidStack fluidStack) {
        var model = getFluidModel(fluidStack);
        var tintSource = model.fluidTintSource();
        if (tintSource != null) return tintSource.colorAsStack(fluidStack);
        return -1;
    }

    public static Material.Baked getStillMaterial(FluidStack fluidStack) {
        var model = getFluidModel(fluidStack);
        return model.stillMaterial();
    }
}
