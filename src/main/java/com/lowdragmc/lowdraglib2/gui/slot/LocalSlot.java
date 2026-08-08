package com.lowdragmc.lowdraglib2.gui.slot;

import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A slot that never belongs to a menu, backing UI elements that only display a stack.
 * <p>
 * Such elements regularly display an amount instead of a real inventory stack — multiblock part counts, recipe
 * ingredients, ... — so the container behind this slot does not limit the stack size. Vanilla's
 * {@link net.minecraft.world.Container#setItem} shrinks the given stack in place to the item's max stack size, which
 * would silently cap every one of those displays at 64 (or less, e.g. 16 for buckets).
 */
@KJSBindings
public class LocalSlot extends Slot {
    public LocalSlot() {
        super(new UnlimitedContainer(), 0, 0, 0);
    }

    private static class UnlimitedContainer extends SimpleContainer {
        public UnlimitedContainer() {
            super(1);
        }

        @Override
        public int getMaxStackSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Integer.MAX_VALUE;
        }
    }
}
