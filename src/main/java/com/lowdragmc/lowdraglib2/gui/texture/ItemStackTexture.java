package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@KJSBindings
@LDLRegisterClient(name = "item_stack_texture", registry = "ldlib2:gui_texture")
public class ItemStackTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.items")
    public ItemStack[] items;
    int index = 0;
    int ticks = 0;

    @ConfigColor
    @Configurable(name = "ldlib.gui.editor.name.color")
    int color = -1;
    long lastTick;

    public ItemStackTexture() {
        this(Items.APPLE.asItem());
    }

    public ItemStackTexture(ItemStack... itemStacks) {
        this.items = itemStacks;
    }

    public ItemStackTexture(Item... items) {
        this.items = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            this.items[i] = new ItemStack(items[i]);
        }
    }

    public ItemStackTexture setItems(ItemStack... itemStack) {
        this.items = itemStack;
        this.index = 0;
        return this;
    }

    @Override
    public ItemStackTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public ItemStackTexture copy() {
        var copied = new ItemStackTexture(items);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }
}
