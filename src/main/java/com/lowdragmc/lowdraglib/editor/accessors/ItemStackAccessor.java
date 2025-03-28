package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "item_stack", registry = "ldlib:configurator_accessor")
public class ItemStackAccessor extends TypesAccessor<ItemStack> {

    public ItemStackAccessor() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(annotation.stringValue()[0])).asItem(), (int) annotation.numberValue()[0]);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<ItemStack> supplier, Consumer<ItemStack> consumer, boolean forceUpdate, Field field) {
        ConfiguratorGroup group = new ConfiguratorGroup(name);
        if (field.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = field.getAnnotation(Configurable.class);
            group.setCollapse(configurable.collapse());
            group.setCanCollapse(configurable.canCollapse());
            group.setTips(configurable.tips());
        }
        var itemHandler = new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, supplier.get()));
        var slot = new SlotWidget(itemHandler, 0, 0, 0, false, false);
        slot.setClientSideWidget();
        Consumer<ItemStack> updateStack = stack -> {
            consumer.accept(stack);
            itemHandler.setStackInSlot(0, stack);
        };
        group.addConfigurators(new ItemConfigurator("id",
                () -> supplier.get().getItem(),
                item -> {
                    var last = supplier.get();
                    var components = last.getComponents();
                    var count = last.getCount();
                    var newStack = new ItemStack(item, Math.max(count, 1));
                    newStack.applyComponents(components);
                    updateStack.accept(newStack);
                }, Items.AIR, forceUpdate));
        var min = 1;
        var max = 64;
        if (field.isAnnotationPresent(NumberRange.class)) {
            min = (int) field.getAnnotation(NumberRange.class).range()[0];
            max = (int) field.getAnnotation(NumberRange.class).range()[1];
        }
        group.addConfigurators(new NumberConfigurator("ldlib.gui.editor.configurator.count",
                () -> supplier.get().getCount(),
                count -> updateStack.accept(supplier.get().copyWithCount(count.intValue())), 1, forceUpdate)
                .setRange(min, max));
        /*
        group.addConfigurators(new CompoundTagAccessor().create("ldlib.gui.editor.configurator.nbt",
                () -> supplier.get().compo() ? supplier.get().getTag() : new CompoundTag(),
                tag -> {
                    var last = supplier.get();
                    var item = last.getItem();
                    var count = last.getCount();
                    var newStack = new ItemStack(item, Math.max(count, 1));
                    if (tag.isEmpty()) {
                        newStack.setTag(null);
                    } else {
                        newStack.setTag(tag);
                    }
                    updateStack.accept(newStack);
                }, false, field));
        */
        group.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", slot));
        return group;
    }

}
