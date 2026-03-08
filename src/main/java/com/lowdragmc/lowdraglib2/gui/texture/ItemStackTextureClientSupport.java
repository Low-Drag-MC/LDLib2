package com.lowdragmc.lowdraglib2.gui.texture;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ItemStackTextureClientSupport {
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
