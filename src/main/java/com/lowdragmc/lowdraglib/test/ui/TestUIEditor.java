package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.ui.UIEditor;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

@LDLRegisterClient(name="ui_editor", registry = "ui_test")
@NoArgsConstructor
public class TestUIEditor implements IUITest {
    @Override
    public ModularUI createUI(IUIHolder holder, Player entityPlayer) {
        return IUITest.super.createUI(holder, entityPlayer)
                .widget(new UIEditor(LDLib.getLDLibDir()));
    }
}
