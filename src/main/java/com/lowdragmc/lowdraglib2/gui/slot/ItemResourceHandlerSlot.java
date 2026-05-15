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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@KJSBindings
public class ItemResourceHandlerSlot extends Slot {
    private static final Container emptyInventory = new SimpleContainer(0);
    @Getter @Setter @Accessors(chain = true)
    private Predicate<ItemStack> canPlace = Predicates.alwaysTrue();
    @Getter @Setter @Accessors(chain = true)
    private Predicate<Player> canTake = Predicates.alwaysTrue();
    @Getter
    private final ResourceHandler<ItemResource> itemHandler;
    private final int index;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public ItemResourceHandlerSlot(ResourceHandler<ItemResource> itemHandler, int index) {
        this(itemHandler, index, 0, 0);
    }

    public ItemResourceHandlerSlot(ResourceHandler<ItemResource> itemHandler, int index, int xPosition, int yPosition) {
        super(emptyInventory, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    public ItemResourceHandlerSlot addChangeListener(Runnable listener) {
        changeListeners.add(listener);
        return this;
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return canPlace.test(stack) && (!stack.isEmpty() && this.itemHandler.isValid(this.index, ItemResource.of(stack)));
    }

    @Override
    public boolean mayPickup(@Nullable Player playerIn) {
        if (!canTake.test(playerIn)) return false;
        var resource = itemHandler.getResource(index);
        if (resource.isEmpty()) {
            return false;
        }
        try (var transaction = Transaction.openRoot()) {
            return this.itemHandler.extract(index, this.itemHandler.getResource(index), 1, transaction) > 0;
        }
    }

    @Override
    @Nonnull
    public ItemStack getItem() {
        return this.itemHandler.getResource(index).toStack(this.itemHandler.getAmountAsInt(index));
    }

    @Override
    public void set(@Nonnull ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            var resource = itemHandler.getResource(index);
            if (!resource.isEmpty()) {
                this.itemHandler.extract(index, this.itemHandler.getResource(index), this.itemHandler.getAmountAsInt(index), transaction);
            }
            if (!stack.isEmpty()) {
                this.itemHandler.insert(index, ItemResource.of(stack), stack.getCount(), transaction);
            }
            transaction.commit();
        }
        this.setChanged();
    }

    @Override
    public void onQuickCraft(@Nonnull ItemStack oldStackIn, @Nonnull ItemStack newStackIn) {

    }

    @Override
    public int getMaxStackSize() {
        return this.itemHandler.getCapacityAsInt(this.index, ItemResource.EMPTY);
    }

    @Override
    public int getMaxStackSize(@Nonnull ItemStack stack) {
        var maxAdd = stack.copy();
        var maxInput = super.getMaxStackSize(stack);
        maxAdd.setCount(maxInput);
        if (maxAdd.isEmpty()) return 0;

        try (var transaction = Transaction.openRoot()) {
            var resource = this.itemHandler.getResource(index);
            if (!resource.isEmpty()) {
                this.itemHandler.extract(index, this.itemHandler.getResource(index), this.itemHandler.getAmountAsInt(index), transaction);
            }
            return this.itemHandler.insert(index, ItemResource.of(maxAdd), maxInput, transaction);
        }
    }

    @NotNull
    @Override
    public ItemStack remove(int amount) {
        try (var transaction = Transaction.openRoot()) {
            var resource = this.itemHandler.getResource(index);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }
            var extracted = this.itemHandler.extract(index, resource, amount, transaction);
            transaction.commit();
            this.setChanged();
            return resource.toStack(extracted);
        }
    }

    @Override
    public void setChanged() {
        changeListeners.forEach(Runnable::run);
    }

    @Override
    public boolean isSameInventory(@NotNull Slot other) {
        return other instanceof ItemResourceHandlerSlot rhs && rhs.itemHandler == this.itemHandler;
    }

}
