package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleSlot;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleTransition;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.TransitionValue;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.utils.animation.Animation;
import com.lowdragmc.lowdraglib2.utils.search.IResultHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TransitionProperty extends Property<Transition> {
    public TransitionProperty(String name, Transition initialValue) {
        super(name, Transition.class, Transition.CODEC, initialValue, TransitionValue::new);
    }

    public static <T> boolean shouldTriggerTransition(Property<T> p, @Nullable StyleSlot<T> oldSlot, @Nullable StyleSlot<T> newSlot) {
        if (!p.isAllowTransition()) return false;
        if (oldSlot != null && oldSlot.origin() == StyleOrigin.ANIMATION) return false;
        if (newSlot != null && newSlot.origin() == StyleOrigin.ANIMATION) return false;
        return true;
    }

    @Override
    public Configurator createConfiguratorInternal(String propertyName, Supplier<Transition> getter, Consumer<Transition> setter) {
        var arrayConfigurator = new ArrayConfiguratorGroup<>(propertyName, true,
                () -> getter.get().animations().entrySet().stream().map(entry ->
                        new StyleTransition(entry.getKey(), entry.getValue())).toList(),
                (transitionGetter, transitionSetter) -> {
                    var configurator = new ConfiguratorGroup().hideTitle();
                    configurator.setCollapse(false);
                    configurator.addConfigurators(
                            new SearchComponentConfigurator<>("property.name", () -> transitionGetter.get().property(), property -> {
                                transitionSetter.accept(new StyleTransition(property, transitionGetter.get().animation()));
                            }, new SearchComponentConfigurator.ISearchConfigurator<Property<?>>() {
                                @Override
                                @Nonnull
                                public Property<?> defaultValue() {
                                    return PropertyRegistry.OPACITY;
                                }

                                @Override
                                @Nonnull
                                public String resultText(@NotNull Property<?> value) {
                                    return value.name;
                                }

                                @Override
                                public void search(String word, IResultHandler<Property<?>> searchHandler) {
                                    var lowerWord = word.toLowerCase();
                                    for (Property<?> property : PropertyRegistry.all()) {
                                        if (Thread.currentThread().isInterrupted()) return;
                                        if (property.isAllowTransition() && !getter.get().animations().containsKey(property)) {
                                            if(property.name.toLowerCase().contains(lowerWord)) {
                                                searchHandler.acceptResult(property);
                                            }
                                        }
                                    }
                                }
                            }, true),
                            new NumberConfigurator("animation.duration", () -> transitionGetter.get().animation().duration(),
                                    duration -> {
                                        var current = transitionGetter.get();
                                        transitionSetter.accept(new StyleTransition(current.property(), new Animation(
                                                duration.floatValue(),
                                                current.animation().delay(),
                                                current.animation().ease())));
                                    }, 1, true).setType(ConfigNumber.Type.FLOAT).setRange(0, Float.MAX_VALUE).setWheel(0.1f),
                            new NumberConfigurator("animation.delay", () -> transitionGetter.get().animation().delay(),
                                    delay -> {
                                        var current = transitionGetter.get();
                                        transitionSetter.accept(new StyleTransition(current.property(), new Animation(
                                                current.animation().duration(),
                                                delay.floatValue(),
                                                current.animation().ease())));
                                    }, 1, true).setType(ConfigNumber.Type.FLOAT).setRange(0, Float.MAX_VALUE).setWheel(0.1f),
                            new SelectorConfigurator<>("animation.ease", () -> transitionGetter.get().animation().ease() instanceof Eases eases ? eases : Eases.LINEAR,
                                    eases -> {
                                        var current = transitionGetter.get();
                                        transitionSetter.accept(new StyleTransition(current.property(), new Animation(
                                                current.animation().duration(),
                                                current.animation().delay(),
                                                eases)));
                                    }, Eases.LINEAR, true, Arrays.stream(Eases.values()).toList(), Enum::name)
                    );
                    return configurator;
                }, true);
        arrayConfigurator.setAddDefault(() -> {
            var animations = getter.get().animations();
            for (Property<?> property : PropertyRegistry.all()) {
                if (property.isAllowTransition() && animations.containsKey(property)) continue;
                return new StyleTransition(property, new Animation(1, 0, Eases.LINEAR));
            }
            return new StyleTransition(PropertyRegistry.OPACITY, new Animation(1, 0, Eases.LINEAR));
        });
        arrayConfigurator.setOnUpdate(transitions -> {
            var animations = new LinkedHashMap<Property<?>, Animation>();
            transitions.forEach(transition -> animations.put(transition.property(), transition.animation()));
            setter.accept(new Transition(animations));
        });
        return arrayConfigurator;
    }

}
