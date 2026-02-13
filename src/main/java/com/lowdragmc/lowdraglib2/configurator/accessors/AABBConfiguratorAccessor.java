package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "aabb", registry = "ldlib2:configurator_accessor")
public class AABBConfiguratorAccessor extends TypesAccessor<AABB> {

    public AABBConfiguratorAccessor() {
        super(AABB.class);
    }

    @Override
    public AABB defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            var annotation = field.getAnnotation(DefaultValue.class);
            return new AABB(annotation.numberValue()[0], annotation.numberValue()[1], annotation.numberValue()[2], annotation.numberValue()[3], annotation.numberValue()[4], annotation.numberValue()[5]);
        }
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public Configurator create(String name, Supplier<AABB> supplier, Consumer<AABB> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator minX, minY, minZ;
        NumberConfigurator maxX, maxY, maxZ;

        configurator.inlineContainer.addChildren(
                new UIElement().addChildren(
                        minX = new NumberConfigurator("minX", () -> supplier.get().minX,
                                v -> consumer.accept(new AABB(v.floatValue(), supplier.get().minY, supplier.get().minZ,
                                        supplier.get().maxX, supplier.get().maxY, supplier.get().maxZ)),
                                defaultValue(field).minX, forceUpdate),
                        minY = new NumberConfigurator("minY", () -> supplier.get().minY,
                                v -> consumer.accept(new AABB(supplier.get().minX, v.floatValue(), supplier.get().minZ,
                                        supplier.get().maxX, supplier.get().maxY, supplier.get().maxZ)),
                                defaultValue(field).minY, forceUpdate),
                        minZ = new NumberConfigurator("minZ", () -> supplier.get().minZ,
                                v -> consumer.accept(new AABB(supplier.get().minX, supplier.get().minY, v.floatValue(),
                                        supplier.get().maxX, supplier.get().maxY, supplier.get().maxZ)),
                                defaultValue(field).minZ, forceUpdate)).layout(layout -> {
                    layout.gapAll(2);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.wrap(FlexWrap.WRAP);
                }),
                new UIElement().addChildren(
                        maxX = new NumberConfigurator("maxX", () -> supplier.get().maxX,
                                v -> consumer.accept(new AABB(supplier.get().minX, supplier.get().minY, supplier.get().minZ,
                                        v.floatValue(), supplier.get().maxY, supplier.get().maxZ)),
                                defaultValue(field).minX, forceUpdate),
                        maxY = new NumberConfigurator("maxY", () -> supplier.get().maxY,
                                v -> consumer.accept(new AABB(supplier.get().minX, supplier.get().minY, supplier.get().minZ,
                                        supplier.get().maxX, v.floatValue(), supplier.get().maxZ)),
                                defaultValue(field).minY, forceUpdate),
                        maxZ = new NumberConfigurator("maxZ", () -> supplier.get().maxZ,
                                v -> consumer.accept(new AABB(supplier.get().minX, supplier.get().minY, supplier.get().minZ,
                                        supplier.get().maxX, supplier.get().maxY, v.floatValue())),
                                defaultValue(field).minZ, forceUpdate)).layout(layout -> {
                    layout.gapAll(2);
                    layout.marginLeft(2);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.wrap(FlexWrap.WRAP);
                })
        ).layout(layout -> {
            layout.gapAll(2);
            layout.marginLeft(2);
        });
        minX.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        minY.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        minZ.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        maxX.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        maxY.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        maxZ.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
            layout.height(14);
        });
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            minX.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            minY.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            minZ.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            maxX.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            maxY.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            maxZ.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = supplier.get();
            return () -> current;
        });
        configurator.setPastable(AABB.class, consumer);
        return configurator;
    }
}
