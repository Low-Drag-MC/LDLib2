package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

@KJSBindings
@LDLRegisterClient(name = "fluid_stack_texture", registry = "ldlib2:gui_texture")
public class FluidStackTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.fluids")
    public FluidStack[] fluids;
    int index = 0;
    int ticks = 0;

    @ConfigColor
    @Configurable(name = "ldlib.gui.editor.name.color")
    int color = -1;
    long lastTick;

    public FluidStackTexture() {
        this(Fluids.WATER);
    }

    public FluidStackTexture(FluidStack... fluidStacks) {
        this.fluids = fluidStacks;
    }

    public FluidStackTexture(Fluid... fluids) {
        this.fluids = new FluidStack[fluids.length];
        for (int i = 0; i < fluids.length; i++) {
            this.fluids[i] = new FluidStack(fluids[i], 1000);
        }
    }

    public FluidStackTexture setFluids(FluidStack... fluidStacks) {
        this.fluids = fluidStacks;
        this.index = 0;
        return this;
    }

    @Override
    public FluidStackTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public FluidStackTexture copy() {
        var copied = new FluidStackTexture(fluids);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }
}
