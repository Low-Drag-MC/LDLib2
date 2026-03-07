package com.lowdragmc.lowdraglib2.gui.ui.utils;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import dev.vfyjxf.taffy.style.TaffyDimension;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@KJSBindings
public class ModularUITooltipComponent implements TooltipComponent {
    public final ModularUI modularUI;

    public ModularUITooltipComponent(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    public ModularUITooltipComponent(UIElement element) {
        this(new ModularUI(UI.of(element)));
        var width = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.WIDTH))
                .orElseGet(TaffyDimension::auto)
                .getValue();
        var height = Optional.ofNullable(element.getStyleBag().computeCandidate(LayoutProperties.HEIGHT))
                .orElseGet(TaffyDimension::auto)
                .getValue();
        this.modularUI.init((int) width, (int) height);
    }

}