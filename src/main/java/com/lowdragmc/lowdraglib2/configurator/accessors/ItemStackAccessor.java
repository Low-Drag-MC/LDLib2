package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "itemstack", registry = "ldlib2:configurator_accessor")
public class ItemStackAccessor extends TypesAccessor<ItemStack> {

    public ItemStackAccessor() {
        super(ItemStack.class);
    }

    @Override
    public ItemStack defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return BuiltInRegistries.ITEM.getValue(Identifier.parse(field.getAnnotation(DefaultValue.class).stringValue()[0])).getDefaultInstance();
        }
        return Items.AIR.getDefaultInstance();
    }

    @Override
    public Configurator create(String name, Supplier<ItemStack> supplier, Consumer<ItemStack> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var group = new ConfiguratorGroup(name);
        var slot = new ItemSlot();
        slot.layout(layout -> layout.width(14).height(14));
        slot.bindDataSource(SupplierDataSource.of(supplier));
        Consumer<ItemStack> updater = itemStack -> {
            slot.setItem(itemStack);
            consumer.accept(itemStack);
        };
        group.inlineContainer.addChild(slot);
        var defaultValue = defaultValue(field);
        var componentsConfigurator = new DataComponentConfigurator(supplier.get().getItem().components(),
                () -> supplier.get().getComponentsPatch(),
                patch -> updater.accept(new ItemStack(supplier.get().getItem().builtInRegistryHolder(), supplier.get().getCount(), patch)), forceUpdate);
        var itemConfigurator = new RegistrySearchComponent.Item("configurator.item",
                () -> supplier.get().getItem(),
                item -> {
                    updater.accept(new ItemStack(item.builtInRegistryHolder(),
                            Math.max(supplier.get().getCount(), 1),
                            supplier.get().getComponentsPatch()));
                    componentsConfigurator.setPrototype(item.components());
                },
                defaultValue.getItem(), forceUpdate);
        var countConfigurator = new NumberConfigurator("ldlib.gui.editor.configurator.count",
                () -> supplier.get().getCount(), count -> updater.accept(supplier.get().copyWithCount(count.intValue())),
                defaultValue.getCount(), forceUpdate)
                .setType(ConfigNumber.Type.INTEGER)
                .setRange(0, Integer.MAX_VALUE)
                .setWheel(1);
        group.addConfigurators(itemConfigurator, countConfigurator, componentsConfigurator);
        if (LDLib2.isJeiLoaded()) {
            RegistrySearchComponent.JEISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                updater.accept(itemStack);
                componentsConfigurator.setPrototype(itemStack.getItem().components());
                group.notifyChanges();
            });
        }
        if (LDLib2.isReiLoaded()) {
            RegistrySearchComponent.REISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                updater.accept(itemStack);
                componentsConfigurator.setPrototype(itemStack.getItem().components());
                group.notifyChanges();
            });
        }
        if (LDLib2.isEmiLoaded()) {
            RegistrySearchComponent.EMISupport.ghostItem(group, Predicates.alwaysTrue(), itemStack -> {
                updater.accept(itemStack);
                componentsConfigurator.setPrototype(itemStack.getItem().components());
                group.notifyChanges();
            });
        }
        return group;
    }
}
