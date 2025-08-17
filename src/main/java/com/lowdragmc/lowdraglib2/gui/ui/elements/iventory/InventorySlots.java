package com.lowdragmc.lowdraglib2.gui.ui.elements.iventory;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;

public class InventorySlots extends UIElement {
    public final Row[] rows = new Row[3];
    public final Row hotbar = new Row();

    public InventorySlots() {
        rows[0] = new Row();
        rows[1] = new Row();
        rows[2] = new Row();
        for (Row row : rows) {
            addChild(row);
        }
        hotbar.getLayout().setMargin(YogaEdge.TOP, 5);
        addChild(hotbar);

        for (int i = 0; i < hotbar.slots.length; i++) {
            hotbar.slots[i].setId("@inventory_%d".formatted(i));
        }
        for (var r = 0; r < rows.length; r++) {
            var row = rows[r];
            for (int c = 0; c < row.slots.length; c++) {
                int slotIndex = r * 9 + c + 9;
                row.slots[c].setId("@inventory_%d".formatted(slotIndex));
            }
        }
    }

    public static class Row extends UIElement {
        public final ItemSlot[] slots = new ItemSlot[9];

        public Row() {
            getLayout().setFlexDirection(YogaFlexDirection.ROW);

            for (int i = 0; i < slots.length; i++) {
                slots[i] = new ItemSlot();
                addChild(slots[i]);
            }
        }
    }
}
