package com.lowdragmc.lowdraglib2.gui.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ItemTooltipTextHelper {
    private static volatile Function<ItemStack, List<Component>> provider = stack -> List.of(stack.getHoverName());

    private ItemTooltipTextHelper() {
    }

    public static List<Component> getTooltip(ItemStack stack) {
        return provider.apply(stack);
    }

    public static void setProvider(Function<ItemStack, List<Component>> provider) {
        ItemTooltipTextHelper.provider = Objects.requireNonNull(provider);
    }
}
