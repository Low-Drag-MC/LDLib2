package com.lowdragmc.lowdraglib.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2023/2/10
 * @implNote FluidHelper
 */
public class FluidHelper {

    public static int getBucket() {
        return FluidType.BUCKET_VOLUME;
    }

    public static int getColor(FluidStack fluidStack) {
        return IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public static TextureAtlasSprite getStillTexture(FluidStack fluidStack) {
        var texture = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getStillTexture(fluidStack);
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
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

    public static boolean canBePlacedInWorld(FluidStack fluidStack, BlockAndTintGetter level, BlockPos pos) {
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
