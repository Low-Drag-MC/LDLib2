package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.RegistrySearchComponent;
import com.lowdragmc.lowdraglib2.configurator.ui.SelectorConfigurator;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "block_state", registry = "ldlib2:configurator_accessor")
public class BlockStateAccessor extends TypesAccessor<BlockState> {

    public BlockStateAccessor() {
        super(BlockState.class);
    }

    @Override
    public BlockState defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return parseBlockState(field.getAnnotation(DefaultValue.class).stringValue()[0]);
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public Configurator create(String name, Supplier<BlockState> supplier, Consumer<BlockState> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var group = new ConfiguratorGroup(name);
        var defaultValue = defaultValue(field);
        var slot = new ItemSlot();
        slot.layout(layout -> layout.width(14).height(14));
        slot.bindDataSource(SupplierDataSource.of(() -> blockItem(safeState(supplier))));
        updateSlot(slot, safeState(supplier));
        Consumer<BlockState> updater = state -> {
            var safeState = state == null ? Blocks.AIR.defaultBlockState() : state;
            updateSlot(slot, safeState);
            consumer.accept(safeState);
        };
        var lastBlock = new Block[]{safeState(supplier).getBlock()};
        var propertiesGroup = new ConfiguratorGroup("properties") {
            @Override
            public void screenTick() {
                super.screenTick();
                if (forceUpdate) {
                    updateSlot(slot, safeState(supplier));
                }
                var block = safeState(supplier).getBlock();
                if (forceUpdate && block != lastBlock[0]) {
                    lastBlock[0] = block;
                    rebuildPropertyConfigurators(this, supplier, updater, forceUpdate);
                }
            }
        };
        propertiesGroup.setCollapse(false);
        group.inlineContainer.addChild(slot);

        var blockConfigurator = new RegistrySearchComponent.Block("block",
                () -> safeState(supplier).getBlock(),
                block -> {
                    updater.accept(block.defaultBlockState());
                    lastBlock[0] = block;
                    rebuildPropertyConfigurators(propertiesGroup, supplier, updater, forceUpdate);
                },
                defaultValue.getBlock(), forceUpdate);

        group.addConfigurators(blockConfigurator, propertiesGroup);
        rebuildPropertyConfigurators(propertiesGroup, supplier, updater, forceUpdate);
        return group;
    }

    private static void rebuildPropertyConfigurators(ConfiguratorGroup group, Supplier<BlockState> supplier, Consumer<BlockState> updater, boolean forceUpdate) {
        group.removeAllConfigurators();
        var state = safeState(supplier);
        for (var property : state.getBlock().getStateDefinition().getProperties()) {
            group.addConfigurator(createPropertyConfigurator(property, supplier, updater, state, forceUpdate));
        }
    }

    private static <T extends Comparable<T>> Configurator createPropertyConfigurator(Property<T> property, Supplier<BlockState> supplier, Consumer<BlockState> updater, BlockState defaultState, boolean forceUpdate) {
        var defaultValue = defaultState.getValue(property);
        var selector = new SelectorConfigurator<>(property.getName(),
                () -> safeState(supplier).hasProperty(property) ? safeState(supplier).getValue(property) : defaultValue,
                value -> {
                    var state = safeState(supplier);
                    if (state.hasProperty(property)) {
                        updater.accept(state.setValue(property, value));
                    }
                },
                defaultValue, forceUpdate, List.copyOf(property.getPossibleValues()), property::getName);
        selector.setCopiable(value -> value);
        return selector;
    }

    private static BlockState safeState(Supplier<BlockState> supplier) {
        var state = supplier.get();
        return state == null ? Blocks.AIR.defaultBlockState() : state;
    }

    private static ItemStack blockItem(BlockState state) {
        return state.getBlock().asItem().getDefaultInstance();
    }

    private static void updateSlot(ItemSlot slot, BlockState state) {
        slot.setItem(blockItem(state));
        slot.style(style -> style.tooltips(propertyTooltips(state)));
    }

    private static Component[] propertyTooltips(BlockState state) {
        return state.getProperties().stream()
                .map(property -> propertyTooltip(state, property))
                .toArray(Component[]::new);
    }

    private static <T extends Comparable<T>> Component propertyTooltip(BlockState state, Property<T> property) {
        return Component.literal(property.getName())
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(": " + property.getName(state.getValue(property))));
    }

    private static BlockState parseBlockState(String rawValue) {
        var value = rawValue.trim();
        var propertiesStart = value.indexOf('[');
        var blockName = propertiesStart >= 0 ? value.substring(0, propertiesStart) : value;
        var state = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName)).defaultBlockState();
        if (propertiesStart < 0) return state;

        var propertiesEnd = value.lastIndexOf(']');
        if (propertiesEnd < propertiesStart) {
            throw new IllegalArgumentException("Invalid block state: " + rawValue);
        }

        var properties = value.substring(propertiesStart + 1, propertiesEnd).trim();
        if (properties.isEmpty()) return state;

        for (var entry : properties.split(",")) {
            var pair = entry.split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Invalid block state property: " + entry);
            }
            var property = state.getBlock().getStateDefinition().getProperty(pair[0].trim());
            if (property == null) {
                throw new IllegalArgumentException("Unknown block state property: " + pair[0].trim());
            }
            state = setPropertyValue(state, property, pair[1].trim(), rawValue);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value, String rawValue) {
        var propertyValue = property.getValue(value)
                .orElseThrow(() -> new IllegalArgumentException("Invalid value '" + value + "' for block state " + rawValue));
        return state.setValue(property, propertyValue);
    }
}
