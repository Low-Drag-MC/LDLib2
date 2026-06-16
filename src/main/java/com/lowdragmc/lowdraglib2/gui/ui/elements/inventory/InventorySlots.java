package com.lowdragmc.lowdraglib2.gui.ui.elements.inventory;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.JustifyContent;
import net.minecraft.world.inventory.Slot;
import org.appliedenergistics.yoga.YogaEdge;

import java.util.function.Consumer;

@KJSBindings
@LDLRegister(name = "inventory-slots", group = "inventory", registry = "ldlib2:ui_element")
public class InventorySlots extends UIElement {
    public final Row[] rows = new Row[3];
    public final Row hotbar = new Row();

    public InventorySlots() {
        rows[0] = new Row();
        rows[1] = new Row();
        rows[2] = new Row();

        var inventory = new UIElement().addClass("__inventory_main__");
        for (var row : rows) {
            inventory.addChild(row);
        }

        addChild(inventory);
        hotbar.getLayout().marginTop(5);
        addChild(hotbar);
        hotbar.addClass("__inventory_hotbar__");

        for (int i = 0; i < hotbar.slots.length; i++) {
            hotbar.slots[i].setId("inventory_%d".formatted(i));
        }
        for (var r = 0; r < rows.length; r++) {
            var row = rows[r];
            for (int c = 0; c < row.slots.length; c++) {
                int slotIndex = r * 9 + c + 9;
                row.slots[c].setId("inventory_%d".formatted(slotIndex));
            }
        }

        addEventListener(UIEvents.MUI_CHANGED, this::onModularUIChanged);
        internalSetup();
    }

    protected void onModularUIChanged(UIEvent event) {
        var mui = getModularUI();
        if (mui != null && event.customData != mui) {
            var menu = mui.getMenu();
            var player = mui.player;
            if (menu != null && player != null) {
                var inventory = player.getInventory();
                for (int i = 0; i < rows.length; i++) {
                    var row = rows[i];
                    for (int j = 0; j < row.slots.length; j++) {
                        var slot = row.slots[j];
                        slot.bind(new Slot(inventory, i * 9 + j + 9, 0, 0));
                    }
                }
                for (int i = 0; i < hotbar.slots.length; i++) {
                    var slot = hotbar.slots[i];
                    slot.bind(new Slot(inventory, i, 0, 0));
                }
            }
        }
    }

    public InventorySlots apply(Consumer<ItemSlot> consumer) {
        for (Row row : rows) {
            row.apply(consumer);
        }
        hotbar.apply(consumer);
        return this;
    }

    public static class Row extends UIElement {
        public final ItemSlot[] slots = new ItemSlot[9];

        public Row() {
            getLayout().flexDirection(FlexDirection.ROW).justifyContent(AlignContent.CENTER);
            addClass("__inventory_row__");

            for (int i = 0; i < slots.length; i++) {
                slots[i] = new ItemSlot().slotStyle(slotStyle -> slotStyle.isPlayerSlot(true));
                addChild(slots[i]);
            }
        }

        public Row apply(Consumer<ItemSlot> consumer) {
            for (ItemSlot slot : slots) {
                consumer.accept(slot);
            }
            return this;
        }
    }
}
