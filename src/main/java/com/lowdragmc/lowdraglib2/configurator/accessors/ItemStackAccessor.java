package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.inventory.InventorySlots;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0])).getDefaultInstance();
        }
        return ItemStack.EMPTY;
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
        var inventoryButton = new Button();
        inventoryButton.style(style -> style.tooltips("ldlib.gui.editor.configurator.select_item.tooltip"));
        inventoryButton.noText();
        inventoryButton.addPreIcon(new ItemStackTexture(Items.CHEST));
        group.inlineContainer.getLayout().flexDirection(FlexDirection.ROW);
        group.inlineContainer.addChildren(slot, new UIElement().layout(l -> l.flex(1)), inventoryButton);
        var defaultValue = defaultValue(field);
        var componentsConfigurator = new DataComponentConfigurator(supplier.get().getItem().components(),
                () -> supplier.get().getComponentsPatch(),
                patch -> updater.accept(new ItemStack(supplier.get().getItem().builtInRegistryHolder(), supplier.get().getCount(), patch)), forceUpdate);
        inventoryButton.setOnClick(event -> {
            // open player inventory to select item
            if (!LDLib2.isClient()) return;
            var mui = event.currentElement.getModularUI();
            if (mui == null || Minecraft.getInstance().player == null) return;
            var inventory = Minecraft.getInstance().player.getInventory();
            var dialog = new Dialog().setTitle("ldlib.gui.editor.configurator.select_item");
            dialog.width(TaffyDimension.length(180));

            var selected = new ItemSlot[]{null};
            var selectedStack = new ItemStack[]{ItemStack.EMPTY};

            var picker = new UIElement().layout(layout -> layout.alignItems(AlignItems.CENTER));

            var main = new UIElement();
            for (int r = 0; r < 3; r++) {
                var row = new UIElement().layout(layout -> layout.flexDirection(FlexDirection.ROW));
                for (int c = 0; c < 9; c++) {
                    row.addChild(createPickerSlot(inventory.getItem(r * 9 + c + 9).copy(), selected, selectedStack));
                }
                main.addChild(row);
            }
            picker.addChild(main);

            var hotbar = new UIElement().layout(layout -> layout
                    .flexDirection(FlexDirection.ROW)
                    .marginTop(5));
            for (int c = 0; c < 9; c++) {
                hotbar.addChild(createPickerSlot(inventory.getItem(c).copy(), selected, selectedStack));
            }
            picker.addChild(hotbar);

            dialog.addContent(picker);
            dialog.addButton(new Button()
                    .setOnClick(e -> {
                        updater.accept(selectedStack[0].copy());
                        componentsConfigurator.setPrototype(selectedStack[0].getItem().components());
                        group.notifyChanges();
                        dialog.close();
                    })
                    .setText("ldlib.gui.tips.confirm")
                    .addClass("__confirm-button__"));
            dialog.addButton(new Button()
                    .setOnClick(e -> dialog.close())
                    .setText("ldlib.gui.tips.cancel")
                    .addClass("__cancel-button__"));
            dialog.show(mui);
            event.stopImmediatePropagation();
        });
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

    private static ItemSlot createPickerSlot(ItemStack stack, ItemSlot[] selected, ItemStack[] selectedStack) {
        var itemSlot = new ItemSlot();
        itemSlot.setItem(stack, false);
        itemSlot.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (selected[0] == itemSlot) return;
            if (selected[0] != null) {
                selected[0].getStyle().overlayTexture(IGuiTexture.EMPTY);
            }
            selected[0] = itemSlot;
            selectedStack[0] = itemSlot.getValue();
            itemSlot.getStyle().overlayTexture(ColorPattern.T_BLUE.rectTexture());
        });
        return itemSlot;
    }
}
