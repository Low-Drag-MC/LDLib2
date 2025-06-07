package com.lowdragmc.lowdraglib2.integration.rei;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author KilaBash
 * @date: 2022/11/27
 * @implNote ModularUIDisplayCategory
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public abstract class ModularUIDisplayCategory<T extends ModularDisplay<?>> implements DisplayCategory<T> {

    @Override
    public List<Widget> setupDisplay(T display, Rectangle bounds) {
        return display.createWidget(bounds);
    }
}
