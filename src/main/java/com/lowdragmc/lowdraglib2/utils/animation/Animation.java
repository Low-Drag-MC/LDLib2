package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Animation(float duration, float delay, IEase ease) {
    public static final Codec<Animation> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("duration").forGetter(animation -> animation.duration),
            Codec.FLOAT.fieldOf("delay").forGetter(animation -> animation.delay),
            IEase.CODEC.fieldOf("ease").forGetter(animation -> animation.ease)
    ).apply(instance, Animation::new));
}
