package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.FluidSlot;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "fluidstack", registry = "ldlib2:configurator_accessor")
public class FluidStackAccessor extends TypesAccessor<FluidStack> {

    public FluidStackAccessor() {
        super(FluidStack.class);
    }

    @Override
    public FluidStack defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(field.getAnnotation(DefaultValue.class).stringValue()[0])), 1000);
        }
        return new FluidStack(Fluids.WATER, 1000);
    }

    @Override
    public Configurator create(String name, Supplier<FluidStack> supplier, Consumer<FluidStack> consumer, boolean forceUpdate, Field field, Object owner) {
        var group = new ConfiguratorGroup(name);
        var slot = new FluidSlot();
        slot.layout(layout -> layout.setWidth(14).setHeight(14));
        slot.setFluid(supplier.get());
        Consumer<FluidStack> updater = fluidStack -> {
            slot.setFluid(fluidStack);
            consumer.accept(fluidStack);
        };
        group.inlineContainer.addChild(slot);
        var defaultValue = defaultValue(field, field.getType());
        var componentsConfigurator = new DataComponentConfigurator(DataComponentMap.EMPTY,
                () -> supplier.get().getComponentsPatch(),
                patch -> updater.accept(new FluidStack(supplier.get().getFluid().builtInRegistryHolder(), supplier.get().getAmount(), patch)), forceUpdate);
        var fluidConfigurator = new RegistrySearchComponent.Fluid("configurator.fluid",
                () -> supplier.get().getFluid(),
                fluid -> updater.accept(new FluidStack(fluid.builtInRegistryHolder(),
                        Math.max(supplier.get().getAmount(), 1),
                        supplier.get().getComponentsPatch())),
                defaultValue.getFluid(), forceUpdate);
        var countConfigurator = new NumberConfigurator("ldlib.gui.editor.configurator.amount",
                () -> supplier.get().getAmount(), count -> updater.accept(supplier.get().copyWithAmount(count.intValue())),
                defaultValue.getAmount(), forceUpdate)
                .setType(ConfigNumber.Type.INTEGER)
                .setRange(0, Integer.MAX_VALUE)
                .setWheel(1);
        group.addConfigurators(fluidConfigurator, countConfigurator, componentsConfigurator);
        return group;
    }
}
