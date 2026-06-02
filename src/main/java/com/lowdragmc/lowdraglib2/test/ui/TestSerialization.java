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
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.ReadOnlyManaged;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name="serialization", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestSerialization implements IScreenTest {
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
        private ItemStack itemStack = ItemStack.EMPTY;
        @Configurable
        private Vector3f vector3fValue = new Vector3f(0, 0, 0);
        @Configurable
        private int[] intArray = new int[]{1, 2, 3};
        @Configurable
        private List<Boolean> booleanList = new ArrayList<>(List.of(true, false, true));
        @Configurable
        private List<BlockState> blockstates = new ArrayList<>();
        @Configurable
        private Component componentValue = Component.translatable("ldlib.author");
        @Configurable(subConfigurable = true)
        private final TestToggleGroup toggleGroup = new TestToggleGroup();
        @Configurable(subConfigurable = true, subFlattenPersisted = true)
        private final TestFlattenGroup flattenGroup = new TestFlattenGroup();
        @Configurable
        @ConfigList(configuratorMethod = "buildTestGroupConfigurator", addDefaultMethod = "addDefaultTestGroup")
        @ReadOnlyManaged(serializeMethod = "testGroupSerialize", deserializeMethod = "testGroupDeserialize")
        private final List<TestGroup> groupList = new ArrayList<>();
        @Persisted
        private final INBTSerializable<CompoundTag> stackHandler = new ItemStackHandler(5);
        @Persisted(subPersisted = true)
        private final TestContainer testContainer = new TestContainer();

        public TestData() {
            itemStack = new ItemStack(Items.DIAMOND_PICKAXE);
            itemStack.enchant(Platform.getFrozenRegistry().lookup(Registries.ENCHANTMENT).get().get(Enchantments.POWER).orElseThrow(), 1);
        }

        public Configurator buildTestGroupConfigurator(Supplier<TestGroup> getter, Consumer<TestGroup> setter) {
            var instance = getter.get();
            if (instance != null && instance.createDirectConfigurator() instanceof ConfiguratorGroup group) {
                group.setCollapse(false);
                group.lineContainer.setDisplay(false);
                return group;
            }
            return new Configurator();
        }

        public static class TestContainer {
            @Persisted
            private Vector3f vector3fValue = new Vector3f(0, 0, 0);
            @Persisted
            private int[] intArray = new int[]{1, 2, 3};
        }

        @SkipPersistedValue(field = "vector3fValue")
        public boolean skipTest(Vector3f value) {
            return value.x == 0f && value.y == 0f && value.z == 0f;
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

        public static class TestFlattenGroup implements IPersistedSerializable, IConfigurable {
            @Configurable
            private Direction flattenEnum = Direction.NORTH;
            @Configurable
            @ConfigNumber(range = {0, 100}, type = ConfigNumber.Type.INTEGER)
            private Range flattenRange = Range.of(0, 1);
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
    CompoundTag serializedNbt = new CompoundTag();
    byte[] serializedBuf = new byte[0];

    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        root.layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.width(350);
            layout.height(300);
        }).setId("root");

        var group = new ConfiguratorGroup("root");
        group.setCollapse(false);
        data.buildConfigurator(group);
        var text = new TextElement();
        root.addChildren(
                new ScrollerView().addScrollViewChild(group).layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                }),
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                }).addChildren(
                        new UIElement().addChildren(
                                new Button().setText("S nbt").setOnClick(e -> {
                                    serializedNbt = data.serializeNBT(Platform.getFrozenRegistry());
                                    text.setText(NbtUtils.toPrettyComponent(serializedNbt));
                                }).layout(layout -> layout.flex(1)),
                                new Button().setText("D nbt").setOnClick(e -> {
                                    data.deserializeNBT(Platform.getFrozenRegistry(), serializedNbt);
                                }).layout(layout -> layout.flex(1))
                        ).layout(layout -> layout.flexDirection(FlexDirection.ROW)),
                        new UIElement().addChildren(
                                new Button().setText("S buf").setOnClick(e -> {
                                    serializedBuf = ByteBufUtil.writeCustomData(buf -> data.writeToBuff(buf), Platform.getFrozenRegistry());
                                    text.setText(serializedBuf.length + " bytes");
                                }).layout(layout -> layout.flex(1)),
                                new Button().setText("D buf").setOnClick(e -> {
                                    try {
                                        ByteBufUtil.readCustomData(serializedBuf,
                                                buf -> data.readFromBuff(buf),
                                                Platform.getFrozenRegistry());
                                    } catch (Exception ignored) {
                                    }
                                }).layout(layout -> layout.flex(1))
                        ).layout(layout -> layout.flexDirection(FlexDirection.ROW)),
                        new ScrollerView().addScrollViewChild(text.textStyle(style -> {
                            style.adaptiveHeight(true);
                            style.textWrap(TextWrap.WRAP);
                        }).layout(layout -> {
                            layout.widthPercent(100);
                        })).layout(layout -> {
                            layout.flex(1);
                            layout.widthPercent(100);
                        })));

        return new ModularUI(UI.of(root));
    }
}
