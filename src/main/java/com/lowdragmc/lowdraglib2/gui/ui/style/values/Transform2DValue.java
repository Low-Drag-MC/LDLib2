package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.LengthPercent;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.Translate2D;
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
                    String[] parts = splitArgs(argsStr);
                    LengthPercent x = parts.length >= 1 ? parseLengthPercent(parts[0]) : LengthPercent.ZERO;
                    LengthPercent y = parts.length >= 2 ? parseLengthPercent(parts[1]) : LengthPercent.ZERO;
                    t.translate(new Translate2D(x, y));
                    break;
                }
                case "translateX": {
                    String[] parts = splitArgs(argsStr);
                    LengthPercent x = parts.length >= 1 ? parseLengthPercent(parts[0]) : LengthPercent.ZERO;
                    t.translate(new Translate2D(x, LengthPercent.ZERO));
                    break;
                }
                case "translateY": {
                    String[] parts = splitArgs(argsStr);
                    LengthPercent y = parts.length >= 1 ? parseLengthPercent(parts[0]) : LengthPercent.ZERO;
                    t.translate(new Translate2D(LengthPercent.ZERO, y));
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

    /** Split args string into individual tokens, preserving unit suffixes like "50%" or "20px" */
    private static String[] splitArgs(String s) {
        if (s.trim().isEmpty()) return new String[0];
        return s.trim().split("\\s*,\\s*|\\s+");
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

    private static LengthPercent parseLengthPercent(String s) {
        s = s.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("%")) {
            return LengthPercent.percent(Float.parseFloat(s.substring(0, s.length() - 1).trim()));
        }
        if (s.endsWith("px")) {
            return LengthPercent.px(Float.parseFloat(s.substring(0, s.length() - 2).trim()));
        }
        return LengthPercent.px(Float.parseFloat(s));
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
