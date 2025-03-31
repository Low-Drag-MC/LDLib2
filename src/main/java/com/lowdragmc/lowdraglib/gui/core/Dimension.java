package com.lowdragmc.lowdraglib.gui.core;

public class Dimension {
    public enum Type { AUTO, LENGTH, PERCENT, NONE }

    public final Type type;
    public final float value;

    private Dimension(Type type, float value) {
        this.type = type;
        this.value = value;
    }

    public static final Dimension AUTO = new Dimension(Type.AUTO, 0);
    public static final Dimension NONE = new Dimension(Type.NONE, 0);

    public static Dimension pixels(float value) {
        return new Dimension(Type.LENGTH, value);
    }

    public static Dimension percent(float value) {
        return new Dimension(Type.PERCENT, value);
    }

    public int resolve(int parentSize) {
        switch (type) {
            case LENGTH: return (int)value;
            case PERCENT: return (int)(parentSize * value / 100);
            default: return -1; // AUTO/NONE返回-1需要特殊处理
        }
    }
}
