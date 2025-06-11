package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.IToggleConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.*;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.value.TextWrap;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name="serialization", registry = "ui_test")
@NoArgsConstructor
public class TestSerialization implements IUITest {
    public class TestData implements IConfigurable, IPersistedSerializable {
        @Configurable
        @ConfigNumber(range = {-5, 5})
        private float numberFloat = 0.0f;
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
        private int[] intArray = new int[]{1, 2, 3};
        @Configurable
        private List<Boolean> booleanList = new ArrayList<>(List.of(true, false, true));
        @Configurable
        private Component componentValue = Component.translatable("ldlib.author");
        @Configurable(subConfigurable = true)
        private final TestToggleGroup toggleGroup = new TestToggleGroup();
        @Configurable
        @ConfigList(configuratorMethod = "buildTestGroupConfigurator", addDefaultMethod = "addDefaultTestGroup")
        @ReadOnlyManaged(serializeMethod = "testGroupSerialize", deserializeMethod = "testGroupDeserialize")
        private final List<TestGroup> groupList = new ArrayList<>();

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

        public IntTag testGroupSerialize(List<TestGroup> groups) {
            return IntTag.valueOf(groups.size());
        }

        public List<TestGroup> testGroupDeserialize(IntTag tag) {
            var groups = new ArrayList<TestGroup>();
            for (int i = 0; i < tag.getAsInt(); i++) {
                groups.add(addDefaultTestGroup());
            }
            return groups;
        }

        public static class TestToggleGroup implements IToggleConfigurable {
            @Getter
            @Setter
            private boolean isEnable = false;
            @Configurable
            @ConfigSelector(candidate = {"north", "west", "south", "east"})
            private Direction enumValue = Direction.NORTH;
        }

        public static class TestGroup implements IConfigurable, IPersistedSerializable {
            @Configurable
            @ConfigNumber(range = {0, 1}, type = ConfigNumber.Type.FLOAT)
            private Range rangeValue = Range.of(0, 1);
            @Configurable
            private Direction enumValue = Direction.NORTH;
            @Configurable
            private Vector3i vector3iValue = new Vector3i(0, 0, 0);
        }

    }

    TestData data = new TestData();
    CompoundTag serialized = new CompoundTag();

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWidth(350);
            layout.setHeight(300);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        data.buildConfigurator(group);
        var text = new TextElement();
        root.addChildren(
                new ScrollerView().addScrollViewChild(group).layout(layout -> {
                    layout.setFlex(1);
                    layout.setHeightPercent(100);
                }),
                new UIElement().layout(layout -> {
                    layout.setFlex(1);
                    layout.setHeightPercent(100);
                }).addChildren(
                        new Button().setText("serialize").setOnClick(e -> {
                            serialized = data.serializeNBT(Platform.getFrozenRegistry());
                            text.setText(NbtUtils.toPrettyComponent(serialized));
                        }),
                        new Button().setText("deserialize").setOnClick(e -> {
                            data.deserializeNBT(Platform.getFrozenRegistry(), serialized);
                        }),
                        new ScrollerView().addScrollViewChild(text.textStyle(style -> {
                            style.adaptiveHeight(true);
                            style.textWrap(TextWrap.WRAP);
                        }).layout(layout -> {
                            layout.setWidthPercent(100);
                        })).layout(layout -> {
                            layout.setFlex(1);
                            layout.setWidthPercent(100);
                        })));

        return new ModularUI(UI.of(root));
    }
}
