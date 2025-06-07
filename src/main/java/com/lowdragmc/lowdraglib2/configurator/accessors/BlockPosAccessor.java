package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaWrap;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2023/5/27
 * @implNote BlockPosAccessor
 */
@LDLRegisterClient(name = "block_pos", registry = "ldlib2:configurator_accessor")
public class BlockPosAccessor extends TypesAccessor<Vec3i> {

    public BlockPosAccessor() {
        super(BlockPos.class, Vec3i.class);
    }

    @Override
    public boolean test(Class<?> type) {
        return Vec3i.class.isAssignableFrom(type);
    }

    @Override
    public BlockPos defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new BlockPos((int) field.getAnnotation(DefaultValue.class).numberValue()[0],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[1],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new BlockPos(0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vec3i> supplier, Consumer<Vec3i> consumer, boolean forceUpdate, Field field) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z;
        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().getX(),
                        v -> consumer.accept(new BlockPos(v.intValue(), supplier.get().getY(), supplier.get().getZ())),
                        defaultValue(field, field.getType()).getX(), forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().getY(),
                        v -> consumer.accept(new BlockPos(supplier.get().getX(), v.intValue(), supplier.get().getZ())),
                        defaultValue(field, field.getType()).getY(), forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().getZ(),
                        v -> consumer.accept(new BlockPos(supplier.get().getX(), supplier.get().getY(), v.intValue())),
                        defaultValue(field, field.getType()).getZ(), forceUpdate)
        ).layout(layout -> {
            layout.setGap(YogaGutter.ALL, 2);
            layout.setMargin(YogaEdge.LEFT, 2);
            layout.setFlexDirection(YogaFlexDirection.ROW);
            layout.setWrap(YogaWrap.WRAP);
        });
        x.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        y.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        z.layout(layout -> {
            layout.setFlex(1);
            layout.setMinWidth(40);
        });
        if (field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }
        return configurator;
    }

}
