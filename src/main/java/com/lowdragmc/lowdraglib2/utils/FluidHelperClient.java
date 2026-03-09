package com.lowdragmc.lowdraglib2.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public final class FluidHelperClient {
    private FluidHelperClient() {
    }

    public static int getColor(FluidStack fluidStack) {
        return IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack);
    }

    @Nullable
    public static TextureAtlasSprite getStillTexture(FluidStack fluidStack) {
        if (fluidStack.getFluid() == Fluids.EMPTY) return null;
        var texture = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getStillTexture(fluidStack);
        if (texture == null) return null;
        return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(texture);
    }
}
