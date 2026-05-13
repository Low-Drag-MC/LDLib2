package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
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

    public static final class FluidStackTextureClientSupport {
        private FluidStackTextureClientSupport() {
        }

        public static void updateTick(FluidStackTexture texture) {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == texture.lastTick) {
                return;
            }
            texture.lastTick = tick;
            if (texture.fluids.length > 1 && ++texture.ticks % 20 == 0 && ++texture.index == texture.fluids.length) {
                texture.index = 0;
            }
        }
    }

    @LDLRegisterClient(name = "fluid_stack_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredFluidStackTextureRenderer implements RegisteredGuiTextureRenderer<FluidStackTexture, RegisteredFluidStackTextureRenderer> {
        @Override
        public Class<FluidStackTexture> type() {
            return FluidStackTexture.class;
        }

        @Override
        public void draw(FluidStackTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(FluidStackTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (texture.fluids.length == 0) {
                return;
            }
            FluidStackTextureClientSupport.updateTick(texture);
            if (texture.fluids[texture.index].isEmpty()) {
                return;
            }
            DrawerHelperClient.drawFluidForGui(context, texture.fluids[texture.index], x, y, width, height, texture.color);
        }
    }
}
