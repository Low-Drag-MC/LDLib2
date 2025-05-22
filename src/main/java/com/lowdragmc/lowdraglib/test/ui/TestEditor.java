package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

@LDLRegisterClient(name="editor", registry = "ui_test")
@NoArgsConstructor
public class TestEditor implements IUITest, IConfigurable {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new Editor().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setHeightPercent(100);
        }).setId("editor");

        return new ModularUI(UI.of(root, size -> size));
    }
}
