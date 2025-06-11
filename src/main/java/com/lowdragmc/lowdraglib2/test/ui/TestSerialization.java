package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import net.minecraft.world.entity.player.Player;

public class TestSerialization implements IUITest {
    TestConfigurators data = new TestConfigurators();

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new ScrollerView();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(350);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        data.buildConfigurator(group);

        return new ModularUI(UI.of(root.addScrollViewChild(group)));
    }
}
