package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.gui.ui.style.IValueInterpolator;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;

import java.util.Collections;
import java.util.TreeSet;

public record KeyFrames<T>(TreeSet<FloatObjectPair<T>> keyframes, IValueInterpolator<T> interpolator) {

    public static <T> KeyFrames<T> of(IValueInterpolator<T> interpolator, FloatObjectPair<T>... keyframes) {
        if (keyframes.length == 0) throw new IllegalArgumentException("Keyframes cannot be empty");
        var set = new TreeSet<FloatObjectPair<T>>((x, y) ->
                Float.compare(x.leftFloat(), y.leftFloat()));
        Collections.addAll(set, keyframes);
        return new KeyFrames<>(set, interpolator);
    }

    public static <T> KeyFrames<T> of(IValueInterpolator<T> interpolator, T from, T to) {
        return of(interpolator, FloatObjectPair.of(0, from), FloatObjectPair.of(1, to));
    }

    public T getValue(float time) {
        var query = FloatObjectPair.<T>of(time, null);
        var floor = keyframes.floor(query);
        var ceiling = keyframes.ceiling(query);

        assert floor != null || ceiling != null;
        if (floor == null) return ceiling.value();
        if (ceiling == null) return floor.value();
        if (floor == ceiling) return floor.value();

        var fraction = (time - floor.leftFloat()) / (ceiling.leftFloat() - floor.leftFloat());

        return interpolator.interpolate(floor.value(), ceiling.value(), fraction);
    }
}
