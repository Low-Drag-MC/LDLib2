package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUITooltipComponent implements TooltipComponent {
    public final ModularUI modularUI;

    public ModularUITooltipComponent(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    public ModularUITooltipComponent(UIElement element) {
        this(new ModularUI(UI.of(element)));
        var width = element.getLayout().getWidth().value;
        var height = element.getLayout().getHeight().value;
        this.modularUI.init((int) width, (int) height);
    }

}