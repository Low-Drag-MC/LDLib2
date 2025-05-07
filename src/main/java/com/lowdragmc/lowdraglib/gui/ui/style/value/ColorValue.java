package com.lowdragmc.lowdraglib.gui.ui.style.value;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleContext;

public class ColorValue extends StyleValue<Integer> {
    protected ColorValue(String rawValue, boolean inheritable) {
        super(rawValue, inheritable);
    }

    @Override
    protected Integer doCompute(StyleContext ctx) {
        return parseColor(rawValue);
    }
    
    private static Integer parseColor(String value) {
        if (value.startsWith("#")) {
            return Integer.parseInt(value.substring(1), 16);
        } else if (value.startsWith("rgb(")) {
            String[] parts = value.substring(4, value.length() - 1).split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            return (r << 16) | (g << 8) | b | 0xFF000000;
        } else if (value.startsWith("rgba(")) {
            String[] parts = value.substring(5, value.length() - 1).split(",");
            int r = Integer.parseInt(parts[0].trim());
            int g = Integer.parseInt(parts[1].trim());
            int b = Integer.parseInt(parts[2].trim());
            int a = (int) (Float.parseFloat(parts[3].trim()) * 255);
            return (r << 16) | (g << 8) | b | (a << 24);
        }
        return 0;
    }
}