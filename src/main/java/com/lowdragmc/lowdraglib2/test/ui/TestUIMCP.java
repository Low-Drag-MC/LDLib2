package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@LDLRegister(name="ui_mcp", registry = "ldlib2:menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestUIMCP implements IMenuTest {

    @Override
    public ModularUI createUI(Player player) {
        var root = new UIElement().layout(layout ->
                layout.gapAll(3).width(100).height(100)
        ).addClass("panel_bg");
        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }
}
