package com.lowdragmc.lowdraglib.gui.ui.style.value;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;

public class LengthValue extends StyleValue<LengthValue.Value> {
    public static final LengthValue ZERO = new LengthValue("0", false);
    public static final LengthValue AUTO = new LengthValue("auto", false);

    public enum Unit { NUMBER, PERCENT, AUTO }
    public record Value(Unit unit, float value) {
        public boolean isAuto() {
            return unit == Unit.AUTO;
        }

        public boolean isPercent() {
            return unit == Unit.PERCENT;
        }

        public boolean isNumber() {
            return unit == Unit.NUMBER;
        }
    }

    protected LengthValue(String rawValue, boolean inheritable) {
        super(rawValue, inheritable);
    }

    @Override
    protected Value doCompute(StyleContext ctx) {
        try {
            if (rawValue.equals("auto")) {
                return new Value(Unit.AUTO, 0);
            } else if (rawValue.endsWith("%")) {
                return new Value(Unit.PERCENT, Float.parseFloat(rawValue.substring(0, rawValue.length() - 1)));
            } else {
                return new Value(Unit.NUMBER, Float.parseFloat(rawValue));
            }
        } catch (Throwable ignored) {}
        return new Value(Unit.AUTO, 0);
    }
}