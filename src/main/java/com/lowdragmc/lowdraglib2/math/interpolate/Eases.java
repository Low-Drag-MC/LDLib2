package com.lowdragmc.lowdraglib2.math.interpolate;

/**
 * Author: KilaBash
 * Date: 2022/08/26
 */
public enum Eases implements IEase {
    EaseLinear(input -> input),
    EaseQuadIn(input -> input * input),
    EaseQuadInOut(input -> {
        if((input /= 0.5f) < 1) {
            return 0.5f * input * input;
        }
        return -0.5f * ((--input) * (input - 2) - 1);
    }),
    EaseQuadOut(input -> -input * (input - 2));


    final IEase ease;

    Eases(IEase ease){
        this.ease = ease;
    }
    @Override
    public float getInterpolation(float t) {
        return ease.getInterpolation(t);
    }
}
