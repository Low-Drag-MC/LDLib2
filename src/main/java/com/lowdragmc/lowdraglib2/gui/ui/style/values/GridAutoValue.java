package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridAuto;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses CSS grid-auto-rows and grid-auto-columns syntax.
 *
 * Supported syntax:
 * - auto
 * - min-content, max-content
 * - 100px, 50%, 1fr
 * - minmax(100px, 1fr)
 * - fit-content(200px)
 * - Multiple values: 100px 1fr auto
 */
public class GridAutoValue extends StyleValue<GridAuto> {

    public GridAutoValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable GridAuto doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static GridAuto parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return GridAuto.EMPTY;
        }

        try {
            List<String> tokens = tokenize(rawValue.trim());
            List<TrackSizingFunction> tracks = new ArrayList<>();

            for (String token : tokens) {
                TrackSizingFunction track = parseTrack(token);
                if (track != null) {
                    tracks.add(track);
                }
            }

            if (tracks.isEmpty()) {
                return GridAuto.EMPTY;
            }

            return new GridAuto(Collections.unmodifiableList(tracks));
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Tokenization ====================

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (Character.isWhitespace(c) && depth == 0) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString().trim());
        }

        return tokens;
    }

    // ==================== Track Parsing ====================

    private static TrackSizingFunction parseTrack(String token) {
        if (token == null || token.isEmpty()) return null;

        token = token.trim();

        // Check for keywords
        if ("auto".equals(token)) {
            return TrackSizingFunction.auto();
        } else if ("min-content".equals(token)) {
            return TrackSizingFunction.minContent();
        } else if ("max-content".equals(token)) {
            return TrackSizingFunction.maxContent();
        }

        // Check for flex (fr)
        if (token.endsWith("fr")) {
            try {
                String numStr = token.substring(0, token.length() - 2).trim();
                float value = Float.parseFloat(numStr);
                return TrackSizingFunction.flex(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // Check for functions
        if (isFunction(token)) {
            if (token.startsWith("minmax(")) {
                return parseMinmax(token);
            } else if (token.startsWith("fit-content(")) {
                return parseFitContent(token);
            }
        }

        // Try to parse as length or percentage
        LengthPercentage lp = parseLength(token);
        if (lp != null) {
            return TrackSizingFunction.fixed(lp);
        }

        return null;
    }

    private static boolean isFunction(String token) {
        return token.contains("(") && token.endsWith(")");
    }

    private static TrackSizingFunction parseMinmax(String token) {
        try {
            String args = extractFunctionArgs(token);
            String[] parts = splitArgs(args);

            if (parts.length != 2) return null;

            TrackSizingFunction min = parseTrack(parts[0].trim());
            TrackSizingFunction max = parseTrack(parts[1].trim());

            if (min == null || max == null) return null;

            return TrackSizingFunction.minmax(min, max);
        } catch (Exception e) {
            return null;
        }
    }

    private static TrackSizingFunction parseFitContent(String token) {
        try {
            String args = extractFunctionArgs(token);
            LengthPercentage limit = parseLength(args.trim());

            if (limit == null) return null;

            return TrackSizingFunction.fitContent(limit);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Helper Methods ====================

    private static String extractFunctionArgs(String func) {
        int start = func.indexOf('(');
        int end = func.lastIndexOf(')');
        if (start == -1 || end == -1 || start >= end) return "";
        return func.substring(start + 1, end);
    }

    private static String[] splitArgs(String args) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);

            if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString().trim());
        }

        return parts.toArray(new String[0]);
    }

    private static LengthPercentage parseLength(String value) {
        if (value == null || value.isEmpty()) return null;

        value = value.trim();

        try {
            // Check for percentage
            if (value.endsWith("%")) {
                String numStr = value.substring(0, value.length() - 1).trim();
                float percent = Float.parseFloat(numStr) / 100f;
                return LengthPercentage.percent(percent);
            }

            // Check for px
            if (value.endsWith("px")) {
                String numStr = value.substring(0, value.length() - 2).trim();
                float length = Float.parseFloat(numStr);
                return LengthPercentage.length(length);
            }

            // Try as plain number (treated as px)
            float length = Float.parseFloat(value);
            return LengthPercentage.length(length);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(GridAuto gridAuto) {
        if (gridAuto == null || gridAuto == GridAuto.EMPTY) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gridAuto.values().size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(trackToString(gridAuto.values().get(i)));
        }
        return sb.toString();
    }

    private static String trackToString(TrackSizingFunction track) {
        return switch (track.getType()) {
            case AUTO -> "auto";
            case MIN_CONTENT -> "min-content";
            case MAX_CONTENT -> "max-content";
            case FLEX -> formatFloat(track.getFlexValue()) + "fr";
            case FIXED -> lengthToString(track.getFixedValue());
            case FIT_CONTENT -> "fit-content(" + lengthToString(track.getFitContentArgument()) + ")";
            case MINMAX -> "minmax(" + trackToString(track.getMinFunc()) + ", " + trackToString(track.getMaxFunc()) + ")";
        };
    }

    private static String lengthToString(LengthPercentage lp) {
        if (lp.isPercent()) {
            return formatFloat(lp.getValue() * 100) + "%";
        } else {
            return formatFloat(lp.getValue()) + "px";
        }
    }

    private static String formatFloat(float value) {
        // Remove trailing .0 for integer values
        if (value == (int) value) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }
}
