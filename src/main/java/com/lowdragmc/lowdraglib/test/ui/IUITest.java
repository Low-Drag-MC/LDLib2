package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public interface IUITest extends ILDLRegisterClient<IUITest, Supplier<IUITest>> {

    ModularUI createUI(Player entityPlayer);

}
