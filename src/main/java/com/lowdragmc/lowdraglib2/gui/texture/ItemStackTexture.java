package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.TransformTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelperClient;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.client.Minecraft;
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

    public static final class ItemStackTextureClientSupport {
        private ItemStackTextureClientSupport() {
        }

        public static void updateTick(ItemStackTexture texture) {
            if (Minecraft.getInstance().level == null) {
                return;
            }
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == texture.lastTick) {
                return;
            }
            texture.lastTick = tick;
            if (texture.items.length > 1 && ++texture.ticks % 20 == 0 && ++texture.index == texture.items.length) {
                texture.index = 0;
            }
        }
    }

    @LDLRegisterClient(name = "item_stack_texture", registry = "ldlib2:gui_texture_renderer")
    public static final class RegisteredItemStackTextureRenderer implements RegisteredGuiTextureRenderer<ItemStackTexture, RegisteredItemStackTextureRenderer> {
        @Override
        public Class<ItemStackTexture> type() {
            return ItemStackTexture.class;
        }

        @Override
        public void draw(ItemStackTexture texture, GUIContext context, float x, float y, float width, float height) {
            TransformTextureRenderer.draw(texture, context, x, y, width, height, this::drawInternal);
        }

        private void drawInternal(ItemStackTexture texture, GUIContext context, float x, float y, float width, float height) {
            if (texture.items.length == 0) {
                return;
            }
            ItemStackTextureClientSupport.updateTick(texture);
            if (texture.index >= texture.items.length) {
                texture.index = 0;
            }
            if (texture.items[texture.index].isEmpty()) {
                return;
            }
            context.pose.pushPose();
            context.pose.scale(width / 16f, height / 16f);
            context.pose.translate(x * 16 / width, y * 16 / height);
            DrawerHelperClient.drawItemStack(context, texture.items[texture.index], 0, 0, 0);
            context.pose.popPose();
        }
    }
}
