package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

public class Transform2DValue extends StyleValue<Transform2D> {
    private static final Pattern TRANSFORM_PATTERN =
            Pattern.compile("(\\w+)\\s*\\(([^)]*)\\)");

    public Transform2DValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable Transform2D doCompute(String rawValue) {
        var m = TRANSFORM_PATTERN.matcher(rawValue);
        var t = new Transform2D();
        while (m.find()) {
            String name = m.group(1);
            String argsStr = m.group(2);
            switch (name) {
                case "translate": {
                    float[] a = parseArgs(argsStr);
                    float x = a.length >= 1 ? a[0] : 0f;
                    float y = a.length >= 2 ? a[1] : 0f;
                    t.translate(x, y);
                    break;
                }
                case "translateX": {
                    float[] a = parseArgs(argsStr);
                    t.translate(a.length >= 1 ? a[0] : 0f, 0f);
                    break;
                }
                case "translateY": {
                    float[] a = parseArgs(argsStr);
                    t.translate(0f, a.length >= 1 ? a[0] : 0f);
                    break;
                }
                case "scale": {
                    float[] a = parseArgs(argsStr);
                    if (a.length == 1) {
                        t.scale(a[0]);
                    } else {
                        t.scale(a[0], a[1]);
                    }
                    break;
                }
                case "scaleX": {
                    float[] a = parseArgs(argsStr);
                    t.scale(a.length >= 1 ? a[0] : 1f, 1f);
                    break;
                }
                case "scaleY": {
                    float[] a = parseArgs(argsStr);
                    t.scale(1f, a.length >= 1 ? a[0] : 1f);
                    break;
                }
                case "rotate":
                case "rotation": {               // support rotate / rotation
                    float angle = parseAngle(argsStr); // support "45", "45deg", "-1.2e2"
                    t.rotation(angle);
                    break;
                }
                case "pivot": {
                    float[] a = parseArgs(argsStr);
                    float px = a.length >= 1 ? a[0] : 0f;
                    float py = a.length >= 2 ? a[1] : 0f;
                    t.pivot(px, py);
                    break;
                }
                default:
                    break;
            }
        }
        return t;
    }

    /** float array： "1,2" / "1 2" / "1 , 2" */
    private static float[] parseArgs(String s) {
        if (s.isEmpty()) return new float[0];
        String[] parts = s.trim().split("\\s*,\\s*|\\s+");
        float[] out = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            out[i] = parseNumber(parts[i]);
        }
        return out;
    }

    private static float parseAngle(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("deg")) {
            return parseNumber(s.substring(0, s.length() - 3));
        }
        // support "rad"
        if (s.endsWith("rad")) {
            float rad = parseNumber(s.substring(0, s.length() - 3));
            return (float) Math.toDegrees(rad);
        }
        return parseNumber(s);
    }

    private static float parseNumber(String s) {
        s = s.trim();
        return Float.parseFloat(s);
    }
}
