package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib.gui.ui.UI;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib.math.Range;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.appliedenergistics.yoga.YogaEdge;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

@LDLRegisterClient(name="configurators", registry = "ui_test")
@NoArgsConstructor
public class TestConfigurators implements IUITest, IConfigurable {
    @Configurable
    @ConfigNumber(range = {-5, 5})
    private float numberFloat = 0.0f;
    @Configurable
    @ConfigColor
    private int numberColor = -1;
    @Configurable
    private boolean booleanValue = false;
    @Configurable(tips = "Test tip 0")
    private String stringValue = "default";
    @Configurable
    private ResourceLocation resourceLocation = LDLib.id("test");
    @Configurable
    private Direction enumValue = Direction.NORTH;
    @Configurable
    private Vector3f vector3fValue = new Vector3f(0, 0, 0);
    @Configurable
    private Vector3i vector3iValue = new Vector3i(0, 0, 0);
    @Configurable
    private Quaternionf quaternionfValue = new Quaternionf(0, 0, 0, 1);
    @Configurable
    private BlockPos blockPosValue = BlockPos.ZERO;
    @Configurable
    private AABB aabbValue = new AABB(0, 0, 0, 1, 1, 1);
    @Configurable
    @ConfigNumber(range = {0, 1}, type = ConfigNumber.Type.FLOAT)
    private Range rangeValue = Range.of(0, 1);

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(300);
            layout.setPadding(YogaEdge.ALL, 5);
        }).setId("root");
        root.getStyle().backgroundTexture(Sprites.BORDER);

        var group = new ConfiguratorGroup("root");
        group.setTips("Test tip 0", "Test tip 1", "Test tip 2");
        buildConfigurator(group);

        return new ModularUI(UI.of(root.addChild(group)));
    }
}
