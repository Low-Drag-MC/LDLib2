package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.UISyncManager;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.iventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import lombok.NoArgsConstructor;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.ParametersAreNonnullByDefault;

@LDLRegister(name="ui_slots", registry = "menu_test")
@NoArgsConstructor
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSlots implements IMenuTest {

    @Override
    public ModularUI createUI(Player player, UISyncManager syncManager) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(400);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root");
        var inventory = new InventorySlots();
        root.getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(
                new ItemSlot(),
                new ItemSlot().setItem(Items.APPLE.getDefaultInstance()),
                new ItemSlot().setItem(Items.STONE.getDefaultInstance().copyWithCount(64)),
                new ItemSlot().setItem(Items.CHEST.getDefaultInstance()),
                inventory
        );
        return new ModularUI(UI.of(root));
    }

    @Override
    public UISyncManager createUISyncManager(Player player) {
        var syncManager = new UISyncManager(player);
        syncManager.addDataBindings(DataBindingBuilder.inventory(player.getInventory()));
        return syncManager;
    }
}
