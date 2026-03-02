package com.lowdragmc.lowdraglib2.gui.ui.style.animation;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.*;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.utils.animation.*;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.function.Consumers;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unchecked"})
@Accessors(fluent = true, chain = true)
public class StyleAnimation {
    @Nullable
    private final ModularUI mui;

    // runtime
    @Setter
    private int specificity = 0;
    @Setter
    private int sourceOrder = 0;
    @Setter
    private StyleOrigin origin = StyleOrigin.INLINE;
    @Setter
    private StyleOrigin animationOrigin = StyleOrigin.ANIMATION;
    @Setter
    private float duration = 1f;
    @Setter
    private float delay = 0f;
    @Setter
    private IEase ease = Eases.LINEAR;
    private final Set<UIElement> targets = new HashSet<>();
    private final Map<Property<?>, List<FloatObjectPair<Object>>> properties = new HashMap<>();
    @Setter
    private BiConsumer<AnimationRuntime, UIElement> onInterpolate = (r, e) -> {};
    @Setter
    private Consumer<UIElement> onFinished = Consumers.nop();

    private StyleAnimation(@Nullable ModularUI mui) {
        this.mui = mui;
    }

    public static StyleAnimation of(@Nullable ModularUI mui) {
        return new StyleAnimation(mui);
    }

    public StyleAnimation select(UIElement element) {
        targets.add(element);
        return this;
    }

    public StyleAnimation select(String selector) {
        if (mui == null) return this;
        mui.select(selector).forEach(this::select);
        return this;
    }

    public <T> StyleAnimation style(Property<T> property, FloatObjectPair... values) {
        List<FloatObjectPair<Object>> slots = new ArrayList<>();
        Arrays.stream(values).sorted((a, b) -> Float.compare(a.leftFloat(), b.leftFloat()))
                .forEach(pair -> slots.add((FloatObjectPair<Object>) pair));
        properties.put(property, slots);
        return this;
    }

    public <T> StyleAnimation style(Property<T> property, T value) {
        return style(property, FloatObjectPair.of(1f, value));
    }

    public StyleAnimation lss(String property, Object value) {
        var p = PropertyRegistry.byName(property);
        if (p == null) return this;
        return style(p, p.valueParser.parse(value.toString()).compute());
    }

    public ISubscription start() {
        if (mui == null) return () -> {};
        var animation = new Animation(duration, delay, ease);
        var executors = new ArrayList<KFExecutor>();
        for (var target : targets) {
            for (var entry : properties.entrySet()) {
                Property p = entry.getKey();
                var slots = new ArrayList<>(entry.getValue());
                var currentValue = target.getStyleBag().computeCandidate(p);
                if (currentValue == null) {
                    currentValue = p.initialValue;
                }
                // check if 0 and 1 missing
                if (slots.isEmpty() || slots.getFirst().leftFloat() != 0f) {
                    slots.addFirst(FloatObjectPair.of(0f, currentValue));
                }
                if (slots.getLast().leftFloat() != 1f) {
                    slots.add(FloatObjectPair.of(1f, currentValue));
                }
                var keyFrame = KeyFrames.of(p.getInterpolator(), slots.toArray(new FloatObjectPair[0]));
                var executor = new KFExecutor<>(keyFrame, new IFrameValueHandler<>() {
                    @Override
                    public void accept(AnimationRuntime runtime, Object o) {
                        target.getStyleBag().onAnimationUpdate(animationOrigin, p, o);
                        onInterpolate.accept(runtime, target);
                    }

                    @Override
                    public void onFinished(AnimationRuntime runtime) {
                        target.getStyleBag().replaceAnimationFinal(p,
                                slot -> slot.origin() == animationOrigin
                                        && slot.specificity() == 999
                                        && slot.sourceOrder() == 0,
                                StyleSlot.of(p, origin, specificity, sourceOrder, slots.getLast().right()));
                        onFinished.accept(target);
                    }
                });
                executors.add(executor);
            }
        }
        return mui.getAnimationEngine().play(new KeyFrameAnimation(executors.toArray(new KFExecutor[0]), animation));
    }
}
