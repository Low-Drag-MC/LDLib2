package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.syncdata.utils.StackSnapshots;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * What a {@link StacksResourceHandler} saves is its stacks, and a stack is exactly its resource and amount, so comparing
 * those per index tells the same as comparing saves, for as long as the class saves and hands them out the way the base
 * class does. Covers NeoForge's item, fluid and resource stack handlers and whatever extends them without changing that.
 */
final class StacksHandlerStrategy implements IMarkStrategy<StacksResourceHandler<?, ?>, StacksHandlerStrategy.Mark> {
    record Mark(Resource[] resources, long[] amounts) {}

    @Override
    public boolean test(Class<?> type) {
        return StacksResourceHandler.class.isAssignableFrom(type)
                && MarkStrategies.isDeclaredBy(type, StacksResourceHandler.class, "serialize", ValueOutput.class)
                && MarkStrategies.isDeclaredBy(type, StacksResourceHandler.class, "size")
                && MarkStrategies.isDeclaredBy(type, StacksResourceHandler.class, "getResource", int.class)
                && MarkStrategies.isDeclaredBy(type, StacksResourceHandler.class, "getAmountAsLong", int.class);
    }

    @Override
    public @NotNull Mark obtainManagedMark(@NotNull StacksResourceHandler<?, ?> value) {
        int size = value.size();
        var resources = new Resource[size];
        var amounts = new long[size];
        for (int i = 0; i < size; i++) {
            resources[i] = snapshot(value.getResource(i));
            amounts[i] = value.getAmountAsLong(i);
        }
        return new Mark(resources, amounts);
    }

    @Override
    public boolean areDifferent(@NotNull Mark managedMark, @NotNull StacksResourceHandler<?, ?> value) {
        int size = value.size();
        if (managedMark.resources.length != size) return true;
        for (int i = 0; i < size; i++) {
            if (managedMark.amounts[i] != value.getAmountAsLong(i) || !managedMark.resources[i].equals(value.getResource(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resources are immutable, but one with components holds a stack that shares its component map with the handler's
     * until either writes, and taken off the server thread that map may go on changing under the mark (see
     * {@link StackSnapshots}). Those get a map of their own; the rest are kept as they are.
     */
    private static Resource snapshot(Resource resource) {
        if (resource instanceof ItemResource item && !item.isComponentsPatchEmpty()) {
            return ItemResource.of(StackSnapshots.of(item.toStack()));
        }
        if (resource instanceof FluidResource fluid && !fluid.isComponentsPatchEmpty()) {
            return FluidResource.of(StackSnapshots.of(fluid.toStack(1)));
        }
        return resource;
    }
}
