package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.CodeEditorWidget;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

@LDLRegisterClient(name="code_editor", registry = "ui_test")
@NoArgsConstructor
public class TestCodeEditorView implements IUITest {
    @Override
    public ModularUI createUI(IUIHolder holder, Player entityPlayer) {
        return IUITest.super.createUI(holder, entityPlayer)
                .widget(new CodeEditorWidget(0, 0, 200, 100));
    }
}
