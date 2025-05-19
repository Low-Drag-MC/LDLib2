package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "block_state", registry = "ldlib:configurator_accessor")
public class BlockStateAccessor extends TypesAccessor<BlockState> {

    public BlockStateAccessor() {
        super(BlockState.class);
    }

    @Override
    public BlockState defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            return BuiltInRegistries.BLOCK.get(ResourceLocation.parse(annotation.stringValue()[0])).defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public Configurator create(String name, Supplier<BlockState> supplier, Consumer<BlockState> consumer, boolean forceUpdate, Field field) {
        ConfiguratorGroup group = new ConfiguratorGroup(name);
        if (field.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = field.getAnnotation(Configurable.class);
            group.setCollapse(configurable.collapse());
            group.setCanCollapse(configurable.canCollapse());
            group.setTips(configurable.tips());
        }
        var propertyGroup = new ConfiguratorGroup("ldlib.gui.editor.blockstate.properties");
        var itemHandler = new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, supplier.get().getBlock().asItem().getDefaultInstance()));
        var slot = new SlotWidget(itemHandler, 0, 0, 0, false, false);
        slot.setClientSideWidget();
        Runnable updateProperty = () -> {
            propertyGroup.removeAllConfigurators();
            for (Property property : supplier.get().getProperties()) {
                var propertySelector = new SelectorConfigurator<>(
                        property.getName(),
                        () -> supplier.get().getValue(property),
                        value -> {
                            var state = supplier.get();
                            state = state.setValue(property, value);
                            consumer.accept(state);
                        }, supplier.get().getValue(property), forceUpdate,
                        property.getPossibleValues().stream().toList(),
                        property::getName);
                propertyGroup.addConfigurators(propertySelector);
            }
            propertyGroup.computeLayout();
        };
        updateProperty.run();
        Consumer<BlockState> updateState = state -> {
            consumer.accept(state);
            itemHandler.setStackInSlot(0, state.getBlock().asItem().getDefaultInstance());
            updateProperty.run();
        };
        group.addConfigurators(new BlockConfigurator("id",
                () -> supplier.get().getBlock(),
                block -> updateState.accept(block.defaultBlockState()), Blocks.AIR, forceUpdate));
        group.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", slot));
        group.addConfigurators(propertyGroup);
        return group;
    }

}
