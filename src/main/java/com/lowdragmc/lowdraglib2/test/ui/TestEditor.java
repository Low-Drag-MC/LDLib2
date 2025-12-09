package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.SplitView;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

@LDLRegisterClient(name="editor", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestEditor implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new EditorWindow(com.lowdragmc.lowdraglib2.test.TestEditor::new);
        return new ModularUI(UI.of(root))
                .shouldCloseOnEsc(false).shouldCloseOnKeyInventory(false)
                ;
    }
}
