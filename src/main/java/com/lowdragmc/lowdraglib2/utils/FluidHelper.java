package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote FluidHelper
 */
@UtilityClass
public final class FluidHelper {

    public static int getBucket() {
        return FluidType.BUCKET_VOLUME;
    }

    public static Component getDisplayName(FluidStack fluidStack) {
        return fluidStack.getFluid().getFluidType().getDescription(fluidStack);
    }

    public static int getTemperature(FluidStack fluidStack) {
        return fluidStack.getFluid().getFluidType().getTemperature(fluidStack);
    }

    public static boolean isLighterThanAir(FluidStack fluidStack) {
        return fluidStack.getFluid().getFluidType().isLighterThanAir();
    }

    public static boolean canBePlacedInWorld(FluidStack fluidStack, BlockAndLightGetter level, BlockPos pos) {
        return fluidStack.getFluid().getFluidType().canBePlacedInLevel(level, pos, fluidStack);
    }

    public static boolean doesVaporize(FluidStack fluidStack, Level level, BlockPos pos) {
        return fluidStack.getFluid().getFluidType().isVaporizedOnPlacement(level, pos, fluidStack);
    }

    public static SoundEvent getEmptySound(FluidStack fluidStack) {
        return fluidStack.getFluid().getFluidType().getSound(fluidStack, SoundActions.BUCKET_EMPTY);
    }

    public static SoundEvent getFillSound(FluidStack fluidStack) {
        return fluidStack.getFluid().getFluidType().getSound(fluidStack, SoundActions.BUCKET_FILL);
    }

    public static Object toRealFluidStack(FluidStack fluidStack) {
        return fluidStack;
    }

    public static String getUnit() {
        return "mB";
    }
}
