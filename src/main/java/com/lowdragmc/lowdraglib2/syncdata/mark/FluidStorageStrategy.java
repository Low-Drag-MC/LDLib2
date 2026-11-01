package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.misc.FluidStorage;
import com.lowdragmc.lowdraglib2.syncdata.utils.StackSnapshots;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

/**
 * What a {@link FluidStorage} saves is its fluid and capacity, and what any other {@link FluidTank} saves is its fluid
 * alone, for as long as the class saves and hands them out the way that base class does.
 */
@SuppressWarnings("removal") // FluidTank is deprecated on 26.1, but still what LDLib2's own FluidStorage extends
final class FluidStorageStrategy implements IMarkStrategy<FluidTank, FluidStorageStrategy.Mark> {
    /** {@code capacity} is -1 for a tank that does not save it, so changing it is no change. */
    record Mark(FluidStack fluid, int capacity) {}

    @Override
    public boolean test(Class<?> type) {
        if (!FluidTank.class.isAssignableFrom(type)
                || !MarkStrategies.isDeclaredBy(type, FluidTank.class, "getFluid")
                || !MarkStrategies.isDeclaredBy(type, FluidTank.class, "getCapacity")) {
            return false;
        }
        return FluidStorage.class.isAssignableFrom(type)
                ? MarkStrategies.isDeclaredBy(type, FluidStorage.class, "serialize", ValueOutput.class)
                : MarkStrategies.isDeclaredBy(type, FluidTank.class, "serialize", ValueOutput.class);
    }

    @Override
    public @NotNull Mark obtainManagedMark(@NotNull FluidTank value) {
        return new Mark(StackSnapshots.of(value.getFluid()), savedCapacity(value));
    }

    @Override
    public boolean areDifferent(@NotNull Mark managedMark, @NotNull FluidTank value) {
        return managedMark.capacity != savedCapacity(value) || !FluidStack.matches(managedMark.fluid, value.getFluid());
    }

    private static int savedCapacity(FluidTank value) {
        return value instanceof FluidStorage ? value.getCapacity() : -1;
    }
}
