package com.lowdragmc.lowdraglib2.gui.ui.style.animation;

import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.utils.animation.Animation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record StyleTransition(Property<?> property, Animation animation) {
    public static final Codec<StyleTransition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Property.CODEC.fieldOf("property").forGetter(transition -> transition.property),
            Animation.CODEC.fieldOf("animation").forGetter(transition -> transition.animation)
    ).apply(instance, StyleTransition::new));
}
