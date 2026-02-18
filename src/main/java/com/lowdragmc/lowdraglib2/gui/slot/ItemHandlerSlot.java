package com.lowdragmc.lowdraglib2.gui.slot;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@KJSBindings
public class ItemHandlerSlot extends Slot {
    private static final Container emptyInventory = new SimpleContainer(0);
    @Getter @Setter @Accessors(chain = true)
    private Predicate<ItemStack> canPlace = Predicates.alwaysTrue();
    @Getter @Setter @Accessors(chain = true)
    private Predicate<Player> canTake = Predicates.alwaysTrue();
    @Getter
    private final IItemHandlerModifiable itemHandler;
    private final int index;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public ItemHandlerSlot(IItemHandlerModifiable itemHandler, int index) {
        this(itemHandler, index, 0, 0);
    }

    public ItemHandlerSlot(IItemHandlerModifiable itemHandler, int index, int xPosition, int yPosition) {
        super(emptyInventory, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public ItemHandlerSlot addChangeListener(Runnable listener) {
        changeListeners.add(listener);
        return this;
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return canPlace.test(stack) && (!stack.isEmpty() && this.itemHandler.isItemValid(this.index, stack));
    }

    @Override
    public boolean mayPickup(@Nullable Player playerIn) {
        return canTake.test(playerIn) && !this.itemHandler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    @Nonnull
    public ItemStack getItem() {
        return this.itemHandler.getStackInSlot(index);
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        this.itemHandler.setStackInSlot(index, stack);
        this.setChanged();
    }

    @Override
    public void onQuickCraft(@Nonnull ItemStack oldStackIn, @Nonnull ItemStack newStackIn) {

    }

    @Override
    public int getMaxStackSize() {
        return this.itemHandler.getSlotLimit(this.index);
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        var maxAdd = stack.copy();
        int maxInput = super.getMaxStackSize(stack);
        maxAdd.setCount(maxInput);
        var currentStack = this.itemHandler.getStackInSlot(index);
        this.itemHandler.setStackInSlot(index, ItemStack.EMPTY);
        ItemStack remainder = this.itemHandler.insertItem(index, maxAdd, true);
        this.itemHandler.setStackInSlot(index, currentStack);
        return maxInput - remainder.getCount();
    }

    @NotNull
    @Override
    public ItemStack remove(int amount) {
        var result = this.itemHandler.extractItem(index, amount, false);
        this.setChanged();
        return result;
    }

    @Override
    public void setChanged() {
        changeListeners.forEach(Runnable::run);
    }

}
