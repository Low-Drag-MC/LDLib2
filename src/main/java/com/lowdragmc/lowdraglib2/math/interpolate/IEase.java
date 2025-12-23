package com.lowdragmc.lowdraglib2.math.interpolate;

import com.lowdragmc.lowdraglib2.utils.LDLibExtraCodecs;
import com.mojang.serialization.Codec;

@FunctionalInterface
public interface IEase {
    Codec<IEase> CODEC = LDLibExtraCodecs.enumCodec(Eases.class, Eases.LINEAR)
            .xmap(eases -> eases, ease -> {
                if (ease instanceof Eases eases) return eases;
                return Eases.LINEAR;
            });

    /**
     * Interpolates a value based on the provided input parameter {@code t} using a specific algorithm.
     * This method typically applies an easing function to produce a smooth transformation.
     *
     * @param t the input value, typically in the range of {@code 0.0} to {@code 1.0}, where
     *          {@code 0.0} represents the start of the interpolation and {@code 1.0} represents the end.
     * @return the interpolated value, corresponding to the input {@code t}, after applying the easing function.
     */
    float interpolate(float t);
}
