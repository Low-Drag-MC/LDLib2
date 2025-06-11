package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.*;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name="configurators", registry = "ui_test")
@NoArgsConstructor
public class TestConfigurators implements IUITest, IConfigurable, IPersistedSerializable {
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
    private ResourceLocation resourceLocation = LDLib2.id("test");
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
    @Configurable
    private int[] intArray = new int[]{1, 2, 3};
    @Configurable
    private List<Boolean> booleanList = new ArrayList<>(List.of(true, false, true));
    @Configurable
    private Component componentValue = Component.translatable("ldlib.author");
    @Configurable(subConfigurable = true)
    private final TestToggleGroup toggleGroup = new TestToggleGroup();
    @Configurable
    @ConfigList(configuratorMethod="buildTestGroupConfigurator", addDefaultMethod = "addDefaultTestGroup")
    private final List<TestGroup> groupList = new ArrayList<>();

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new ScrollerView();
        root.layout(layout -> {
            layout.setWidth(250);
            layout.setHeight(350);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        group.setTips("Test tip 0", "Test tip 1", "Test tip 2");
        buildConfigurator(group);

        return new ModularUI(UI.of(root.addScrollViewChild(group)));
    }

    public Configurator buildTestGroupConfigurator(Supplier<TestGroup> getter, Consumer<TestGroup> setter) {
        var instance = getter.get();
        if (instance != null) {
            return instance.createDirectConfigurator();
        }
        return new Configurator();
    }

    public TestGroup addDefaultTestGroup() {
        return new TestGroup();
    }

    public static class TestToggleGroup implements IToggleConfigurable {
        @Getter
        @Setter
        private boolean isEnable = false;
        @Configurable
        @ConfigSelector(candidate = {"north", "west", "south", "east"})
        private Direction enumValue = Direction.NORTH;
    }

    public static class TestGroup implements IConfigurable {
        @Configurable
        @ConfigNumber(range = {0, 1}, type = ConfigNumber.Type.FLOAT)
        private Range rangeValue = Range.of(0, 1);
        @Configurable
        private Direction enumValue = Direction.NORTH;
        @Configurable
        private Vector3i vector3iValue = new Vector3i(0, 0, 0);
    }
}
