package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.StyleTransition;
import com.lowdragmc.lowdraglib2.gui.ui.style.animation.Transition;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.utils.animation.Animation;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransitionValue extends StyleValue<Transition> {
    public TransitionValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable Transition doCompute(String rawValue) {
        Map<Property<?>, Animation> animations = new LinkedHashMap<>();
        for (var rawTransition : rawValue.split(",")) {
            var styleTransition = parse(rawTransition.trim());
            if (styleTransition != null) {
                animations.put(styleTransition.property(), styleTransition.animation());
            }
        }
        return new Transition(animations);
    }

    @Nullable
    public static StyleTransition parse(String rawValue) {
        try {
            var args = rawValue.split(" ");
            if (args.length < 2) return null;
            var property = PropertyRegistry.byName(args[0]);
            if (property == null) return null;
            var duration = Float.parseFloat(args[1]);
            var ease = Eases.LINEAR;
            var delay = 0f;
            if (args.length == 3) {
                for (Eases value : Eases.values()) {
                    if (value.name().equalsIgnoreCase(args[2])) {
                        ease = value;
                        break;
                    }
                }
            }
            if (args.length == 4) {
                delay = Float.parseFloat(args[3]);
            }
            return new StyleTransition(property, new Animation(duration, delay, ease));
        } catch (Throwable e) {
            return null;
        }
    }
}
