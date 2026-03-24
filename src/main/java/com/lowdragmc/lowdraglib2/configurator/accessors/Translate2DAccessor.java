package com.lowdragmc.lowdraglib2.configurator.accessors;

import com.lowdragmc.lowdraglib2.configurator.annotation.DefaultValue;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.LengthPercentConfigurator;
import com.lowdragmc.lowdraglib2.gui.ui.data.LengthPercent;
import com.lowdragmc.lowdraglib2.gui.ui.data.Translate2D;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegisterClient(name = "translate2d", registry = "ldlib2:configurator_accessor")
public class Translate2DAccessor extends TypesAccessor<Translate2D> {

    public Translate2DAccessor() {
        super(Translate2D.class);
    }

    @Override
    public Translate2D defaultValue(@Nullable Field field, @Nullable Class<?> type) {
        if (field != null && field.isAnnotationPresent(DefaultValue.class)) {
            return Translate2D.px(
                    (float) field.getAnnotation(DefaultValue.class).numberValue()[0],
                    (float) field.getAnnotation(DefaultValue.class).numberValue()[1]);
        }
        return Translate2D.ZERO;
    }

    @Override
    public Configurator create(String name, Supplier<Translate2D> supplier, Consumer<Translate2D> consumer, boolean forceUpdate, @Nullable Field field, @Nullable Object owner) {
        var configurator = new Configurator(name);

        var xConfigurator = new LengthPercentConfigurator("x",
                () -> supplier.get().getX(),
                lp -> consumer.accept(new Translate2D(lp, supplier.get().getY())),
                defaultValue(field).getX(), forceUpdate);

        var yConfigurator = new LengthPercentConfigurator("y",
                () -> supplier.get().getY(),
                lp -> consumer.accept(new Translate2D(supplier.get().getX(), lp)),
                defaultValue(field).getY(), forceUpdate);

        configurator.inlineContainer.addChildren(xConfigurator, yConfigurator)
                .layout(layout -> {
                    layout.gapAll(2);
                    layout.marginLeft(2);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.wrap(FlexWrap.WRAP);
                });
        xConfigurator.layout(layout -> {
            layout.flex(1);
            layout.minWidth(80);
        });
        yConfigurator.layout(layout -> {
            layout.flex(1);
            layout.minWidth(80);
        });

        configurator.setCopiable(() -> {
            var current = supplier.get();
            return () -> current;
        });
        configurator.setPastable(Translate2D.class, consumer);
        return configurator;
    }
}
