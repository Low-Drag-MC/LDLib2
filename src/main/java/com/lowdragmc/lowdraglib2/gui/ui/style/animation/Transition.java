package com.lowdragmc.lowdraglib2.gui.ui.style.animation;

import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.utils.animation.Animation;
import com.mojang.serialization.Codec;

import java.util.*;

public record Transition(Map<Property<?>, Animation> animations) {
    public static final Codec<Transition> CODEC = Codec.unboundedMap(Property.CODEC, Animation.CODEC)
            .xmap(Transition::new, transition -> transition.animations);

    public static final Transition EMPTY = new Transition(Map.of());
}
