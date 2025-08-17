package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.registry.ILDLRegisterClient;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public interface IScreenTest extends ILDLRegisterClient<IScreenTest, Supplier<IScreenTest>> {

    ModularUI createUI(Player entityPlayer);

}
