package com.lowdragmc.lowdraglib2.math.interpolate;
public enum Eases implements IEase {
    LINEAR(input -> input),
    QUAD_IN(input -> input * input),
    QUAD_IN_OUT(input -> {
        if((input /= 0.5f) < 1) {
            return 0.5f * input * input;
        }
        return -0.5f * ((--input) * (input - 2) - 1);
    }),
    QUAD_OUT(input -> -input * (input - 2)),
    CUBIC_IN(input -> input * input * input),
    CUBIC_OUT(input -> (input - 1) * (input - 1) * (input - 1) + 1),
    CUBIC_IN_OUT(input -> {
        if ((input /= 0.5f) < 1) return 0.5f * input * input * input;
        return 0.5f * ((input -= 2) * input * input + 2);
    }),
    QUART_IN(input -> input * input * input * input),
    QUART_OUT(input -> 1 - (--input) * input * input * input),
    QUART_IN_OUT(input -> {
        if ((input /= 0.5f) < 1) return 0.5f * input * input * input * input;
        return -0.5f * ((input -= 2) * input * input * input - 2);
    }),
    EXPO_IN(input -> (input == 0) ? 0 : (float) Math.pow(2, 10 * (input - 1))),
    EXPO_OUT(input -> (input == 1) ? 1 : -(float) Math.pow(2, -10 * input) + 1),
    EXPO_IN_OUT(input -> {
        if (input == 0) return 0;
        if (input == 1) return 1;
        if ((input /= 0.5f) < 1) return 0.5f * (float) Math.pow(2, 10 * (input - 1));
        return 0.5f * (-(float) Math.pow(2, -10 * --input) + 2);
    }),
    SINE_IN(input -> 1 - (float) Math.cos(input * Math.PI / 2)),
    SINE_OUT(input -> (float) Math.sin(input * Math.PI / 2)),
    SINE_IN_OUT(input -> -0.5f * ((float) Math.cos(Math.PI * input) - 1)),
    CIRC_IN(input -> 1 - (float) Math.sqrt(1 - input * input)),
    CIRC_OUT(input -> (float) Math.sqrt(1 - (input - 1) * (input - 1))),
    CIRC_IN_OUT(input -> {
        if ((input /= 0.5f) < 1) return -0.5f * ((float) Math.sqrt(1 - input * input) - 1);
        return 0.5f * ((float) Math.sqrt(1 - (input -= 2) * input) + 1);
    }),
    ELASTIC_IN(input -> {
        if (input == 0) return 0;
        if (input == 1) return 1;
        float p = 0.3f;
        float s = p / 4;
        return -((float) Math.pow(2, 10 * (input -= 1)) * (float) Math.sin((input - s) * (2 * Math.PI) / p));
    }),
    ELASTIC_OUT(input -> {
        if (input == 0) return 0;
        if (input == 1) return 1;
        float p = 0.3f;
        float s = p / 4;
        return ((float) Math.pow(2, -10 * input) * (float) Math.sin((input - s) * (2 * Math.PI) / p) + 1);
    }),
    ELASTIC_IN_OUT(input -> {
        if (input == 0) return 0;
        if ((input /= 0.5f) == 2) return 1;
        float p = 0.45f;
        float s = p / 4;
        if (input < 1)
            return -0.5f * ((float) Math.pow(2, 10 * (input -= 1)) * (float) Math.sin((input - s) * (2 * Math.PI) / p));
        return ((float) Math.pow(2, -10 * (input -= 1)) * (float) Math.sin((input - s) * (2 * Math.PI) / p) * 0.5f + 1);
    });


    final IEase ease;

    Eases(IEase ease){
        this.ease = ease;
    }

    @Override
    public float interpolate(float t) {
        return ease.interpolate(t);
    }
}
