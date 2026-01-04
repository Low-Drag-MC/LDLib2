package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaFlexDirection;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@LDLRegister(name="slots", registry = "ldlib2:menu_test")
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSlots implements IMenuTest {


    @Override
    public ModularUI createUI(Player player) {
        var z = 15;
        var itemHandler = new ItemStackHandler(9 * z);
        var scrollerView = new ScrollerView();
        var root = new UIElement().layout(layout -> layout.gapAll(3).width(200));
        for (int i = 0; i < z; i++) {
            var row = new UIElement().layout(layout -> layout.width(144).flexDirection(YogaFlexDirection.ROW));
            for (int j = 0; j < 9; j++) {
                row.addChildren(new ItemSlot().bind(itemHandler, i * 9 + j));
            }
            scrollerView.addScrollViewChild(row);
        }

        root.addChildren(
                scrollerView.layout(layout -> layout.height(140)),
                new InventorySlots()
        ).addClass("panel_bg");
        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }
}
