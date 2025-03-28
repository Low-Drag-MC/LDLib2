package com.lowdragmc.lowdraglib.editor.accessors;

import com.lowdragmc.lowdraglib.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.editor.annotation.NumberRange;
import com.lowdragmc.lowdraglib.editor.configurator.*;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "fluid_stack", registry = "ldlib:configurator_accessor")
public class FluidStackAccessor extends TypesAccessor<FluidStack> {

    public FluidStackAccessor() {
        super(FluidStack.class);
    }

    @Override
    public FluidStack defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            return new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(annotation.stringValue()[0])), (int) annotation.numberValue()[0]);

        }
        return FluidStack.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<FluidStack> supplier, Consumer<FluidStack> consumer, boolean forceUpdate, Field field) {
        ConfiguratorGroup group = new ConfiguratorGroup(name);
        if (field.isAnnotationPresent(Configurable.class)) {
            Configurable configurable = field.getAnnotation(Configurable.class);
            group.setCollapse(configurable.collapse());
            group.setCanCollapse(configurable.canCollapse());
            group.setTips(configurable.tips());
        }
        FluidStack fluidStack = supplier.get();
        var fluidStorage = new FluidTank(fluidStack.getAmount());
        fluidStorage.setFluid(fluidStack);
        var tank = new TankWidget(fluidStorage, 0, 0, 18, 18, false, false);
        tank.setBackground(TankWidget.FLUID_SLOT_TEXTURE);
        tank.setClientSideWidget();
        Consumer<FluidStack> updateStack = stack -> {
            consumer.accept(stack);
            fluidStorage.setFluid(stack);
        };
        group.addConfigurators(new FluidConfigurator("id",
                () -> supplier.get().getFluid(),
                item -> {
                    var last = supplier.get();
                    var tag = last.getComponents();
                    var count = last.getAmount();
                    var newStack = new FluidStack(item, Math.max(count, 1));
                    newStack.applyComponents(tag);
                    updateStack.accept(newStack);
                }, Fluids.EMPTY, forceUpdate));
        var min = 1;
        var max = 64;
        if (field.isAnnotationPresent(NumberRange.class)) {
            min = (int) field.getAnnotation(NumberRange.class).range()[0];
            max = (int) field.getAnnotation(NumberRange.class).range()[1];
        }
        group.addConfigurators(new NumberConfigurator("ldlib.gui.editor.configurator.amount",
                () -> supplier.get().getAmount(),
                count -> {
                    FluidStack copy = supplier.get().copy();
                    copy.setAmount(count.intValue());
                    updateStack.accept(copy);
                }, 1, forceUpdate)
                .setRange(min, max));
        /*
        group.addConfigurators(new CompoundTagAccessor().create("ldlib.gui.editor.configurator.nbt",
                () -> supplier.get().hasTag() ? supplier.get().getTag() : new CompoundTag(),
                tag -> {
                    var last = supplier.get();
                    var fluid = last.getFluid();
                    var count = last.getAmount();
                    var newStack = new FluidStack(fluid, Math.max(count, 1));
                    if (tag.isEmpty()) {
                        newStack.setTag(null);
                    } else {
                        newStack.setTag(tag);
                    }
                    updateStack.accept(newStack);
                }, false, field));
        */
        group.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", tank));
        return group;
    }

}
