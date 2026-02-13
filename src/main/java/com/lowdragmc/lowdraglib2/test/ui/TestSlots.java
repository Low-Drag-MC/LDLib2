package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.items.ItemStackHandler;

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
        var root = new UIElement().layout(layout -> layout.gapAll(3));
        for (int i = 0; i < z; i++) {
            var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
            for (int j = 0; j < 9; j++) {
                var slot = new ItemSlot();
                slot.bind(itemHandler, i * 9 + j);
                if (i == 0) {
                    slot.getSlotStyle().slotOverlay(Icons.UP_ARROW_NO_BAR);
                }
                row.addChildren(slot);
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
