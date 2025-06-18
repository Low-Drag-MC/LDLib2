package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

@LDLRegisterClient(name="editor", registry = "ui_test")
@NoArgsConstructor
public class TestEditor implements IUITest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new EditorWindow(com.lowdragmc.lowdraglib2.test.TestEditor::new);
        return new ModularUI(UI.of(root, size -> size))
//                .shouldCloseOnEsc(false).shouldCloseOnKeyInventory(false)
                ;
    }
}
