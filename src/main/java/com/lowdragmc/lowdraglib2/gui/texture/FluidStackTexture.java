package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;

@KJSBindings
@LDLRegisterClient(name = "fluid_stack_texture", registry = "ldlib2:gui_texture")
public class FluidStackTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.fluids")
    public FluidStack[] fluids;
    private int index = 0;
    private int ticks = 0;

    @ConfigColor
    @Configurable(name = "ldlib.gui.editor.name.color")
    private int color = -1;
    private long lastTick;

    public FluidStackTexture() {
        this(Fluids.WATER);
    }

    public FluidStackTexture(FluidStack... fluidStacks) {
        this.fluids = fluidStacks;
    }

    public FluidStackTexture(Fluid... fluids) {
        this.fluids = new FluidStack[fluids.length];
        for(int i = 0; i < fluids.length; i++) {
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
        var copied= new FluidStackTexture(fluids);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }

    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        if (Minecraft.getInstance().level != null) {
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == lastTick) return;
            lastTick = tick;
            if(fluids.length > 1 && ++ticks % 20 == 0)
                if(++index == fluids.length)
                    index = 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        if (fluids.length == 0) return;
        updateTick();
        if (index >= fluids.length) {
            index = 0;
        }
        if (fluids[index].isEmpty()) return;

        DrawerHelper.drawFluidForGui(graphics, fluids[index], x, y, width, height, color);
    }
}
