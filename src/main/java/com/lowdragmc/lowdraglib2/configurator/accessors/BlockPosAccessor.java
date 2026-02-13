package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaGutter;
import org.jetbrains.annotations.Nullable;

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
    public BlockPos defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return new BlockPos((int) field.getAnnotation(DefaultValue.class).numberValue()[0],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[1],
                    (int) field.getAnnotation(DefaultValue.class).numberValue()[2]);
        }
        return new BlockPos(0, 0, 0);
    }

    @Override
    public Configurator create(String name, Supplier<Vec3i> supplier, Consumer<Vec3i> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);
        NumberConfigurator x, y, z;
        configurator.inlineContainer.addChildren(
                x = new NumberConfigurator("x", () -> supplier.get().getX(),
                        v -> consumer.accept(new BlockPos(v.intValue(), supplier.get().getY(), supplier.get().getZ())),
                        defaultValue(field).getX(), forceUpdate),
                y = new NumberConfigurator("y", () -> supplier.get().getY(),
                        v -> consumer.accept(new BlockPos(supplier.get().getX(), v.intValue(), supplier.get().getZ())),
                        defaultValue(field).getY(), forceUpdate),
                z = new NumberConfigurator("z", () -> supplier.get().getZ(),
                        v -> consumer.accept(new BlockPos(supplier.get().getX(), supplier.get().getY(), v.intValue())),
                        defaultValue(field).getZ(), forceUpdate)
        ).layout(layout -> {
            layout.gapAll(2);
            layout.marginLeft(2);
            layout.flexDirection(FlexDirection.ROW);
            layout.wrap(FlexWrap.WRAP);
        });
        x.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
        });
        y.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
        });
        z.layout(layout -> {
            layout.flex(1);
            layout.minWidth(40);
        });
        if (field != null && field.isAnnotationPresent(ConfigNumber.class)) {
            var config = field.getAnnotation(ConfigNumber.class);
            x.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            y.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
            z.setRange(config.range()[0], config.range()[1]).setWheel(config.wheel());
        }

        configurator.setCopiable(() -> {
            var current = supplier.get();
            return () -> current;
        });
        configurator.setPastable(Vec3i.class, consumer);
        return configurator;
    }

}
