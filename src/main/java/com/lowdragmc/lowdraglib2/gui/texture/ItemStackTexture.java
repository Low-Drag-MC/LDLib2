package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegisterClient(name = "item_stack_texture", registry = "ldlib2:gui_texture")
public class ItemStackTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.items")
    public ItemStack[] items;
    private int index = 0;
    private int ticks = 0;
    @ConfigColor
    @Configurable(name = "ldlib.gui.editor.name.color")
    private int color = -1;
    private long lastTick;

    public ItemStackTexture() {
        this(Items.APPLE.asItem());
    }


    public ItemStackTexture(ItemStack... itemStacks) {
        this.items = itemStacks;
    }

    public ItemStackTexture(Item... items) {
        this.items = new ItemStack[items.length];
        for(int i = 0; i < items.length; i++) {
            this.items[i] = new ItemStack(items[i]);
        }
    }

    public ItemStackTexture setItems(ItemStack... itemStack) {
        this.items = itemStack;
        return this;
    }

    @Override
    public ItemStackTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void updateTick() {
        if (Minecraft.getInstance().level != null) {
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == lastTick) return;
            lastTick = tick;
            if(items.length > 1 && ++ticks % 20 == 0)
                if(++index == items.length)
                    index = 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        if (items.length == 0) return;
        updateTick();
        graphics.pose().pushPose();
        graphics.pose().scale(width / 16f, height / 16f, 1);
        graphics.pose().translate(x * 16 / width, y * 16 / height, -200);
        DrawerHelper.drawItemStack(graphics, items[index], 0, 0, color, null);
        graphics.pose().popPose();
    }
}
