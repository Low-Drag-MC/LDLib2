package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.Platform;
import dev.architectury.fluid.FluidStack;
import lombok.experimental.UtilityClass;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class FluidHelper {

    public static int getBucket() {
        return (int) FluidConstants.BUCKET;
    }

    public static int getColor(FluidStack fluidStack) {
        if (Platform.isClient()) {
            return getColorClient(fluidStack);
        }
        return -1;
    }

    @Environment(EnvType.CLIENT)
    private static int getColorClient(FluidStack fluidStack) {
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidStack.getFluid());
        if (handler != null) {
            return handler.getFluidColor(null, null, fluidStack.getFluid().defaultFluidState());
        }
        return -1;
    }

    @Nullable
    @Environment(EnvType.CLIENT)
    public static TextureAtlasSprite getStillTexture(FluidStack fluidStack) {
        if (fluidStack.getFluid() == Fluids.EMPTY) return null;
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidStack.getFluid());
        if (handler == null) return null;
        var sprites = handler.getFluidSprites(null, null, fluidStack.getFluid().defaultFluidState());
        if (sprites == null || sprites.length == 0) return null;
        return sprites[0];
    }

    public static Component getDisplayName(FluidStack fluidStack) {
        return Component.translatable(fluidStack.getFluid().defaultFluidState().createLegacyBlock().getBlock().getDescriptionId());
    }

    public static int getTemperature(FluidStack fluidStack) {
        // Fabric doesn't have a direct equivalent for FluidType.getTemperature in the base API
        // For now, return a default or use a custom property if needed
        return 300; 
    }

    public static boolean isLighterThanAir(FluidStack fluidStack) {
        return false;
    }

    public static boolean canBePlacedInWorld(FluidStack fluidStack, BlockAndTintGetter level, BlockPos pos) {
        return true; 
    }

    public static boolean doesVaporize(FluidStack fluidStack, Level level, BlockPos pos) {
        return false;
    }

    public static SoundEvent getEmptySound(FluidStack fluidStack) {
        return fluidStack.getFluid().getPickupSound().orElse(null);
    }

    public static SoundEvent getFillSound(FluidStack fluidStack) {
        return fluidStack.getFluid().getPickupSound().orElse(null);
    }

    public static Object toRealFluidStack(FluidStack fluidStack) {
        return fluidStack;
    }

    public static String getUnit() {
        return "mB";
    }
}
