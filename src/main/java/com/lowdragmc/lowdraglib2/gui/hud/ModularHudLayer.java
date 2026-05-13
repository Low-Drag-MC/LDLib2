package com.lowdragmc.lowdraglib2.gui.hud;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.math.Size;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface ModularHudLayer extends GuiLayer {

    @Nullable ModularUI getModularUI();

    /**
     * Retrieves the current screen size of the game window as a {@link Size} object.
     *
     * @return a {@link Size} instance representing the width and height of the game screen.
     */
    default Size getScreenSize() {
        return Size.of(Minecraft.getInstance().getWindow().getScreenWidth(),
                Minecraft.getInstance().getWindow().getScreenHeight());
    }

    /**
     * Validates the specified {@code ModularUI} by ensuring its dimensions match the current screen size.
     * If the dimensions do not match, it reinitializes the {@code ModularUI} to adapt to the screen size.
     *
     * @param mui the {@link ModularUI} instance to be validated and possibly reinitialized.
     * @return {@code true} if the validation process is completed successfully.
     */
    default boolean validModularUI(ModularUI mui) {
        // always update tick while rendering
        mui.setTickWhileRending(true);
        // check screen size
        var size = getScreenSize();
        if (mui.getScreenWidth() != size.getWidth() || mui.getScreenHeight() != size.getHeight()) {
            mui.init(size.width, size.height);
        }
        return true;
    }

    @Override
    default void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        var mui = getModularUI();
        if (mui == null) return;
        if (validModularUI(mui)) {
            var partialTicks = deltaTracker.getGameTimeDeltaPartialTick(false);
            ModularUIClientAccess.getWidget(mui).extractRenderState(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);
        }
    }
}
