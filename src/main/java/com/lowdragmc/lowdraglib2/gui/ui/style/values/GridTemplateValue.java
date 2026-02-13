package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplate;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.style.*;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GridTemplateValue extends StyleValue<GridTemplate> {

    public GridTemplateValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable GridTemplate doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static GridTemplate parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return GridTemplate.EMPTY;
        }

        try {
            List<String> tokens = tokenize(rawValue.trim());
            List<TrackSizingFunction> simples = new ArrayList<>();
            List<GridTemplateComponent> repeats = new ArrayList<>();
            List<NamedGridLine> names = new ArrayList<>();

            int trackIndex = 0;

            for (String token : tokens) {
                if (isNamedLine(token)) {
                    // Parse named line
                    NamedGridLine namedLine = parseNamedLine(token, trackIndex);
                    if (namedLine != null) {
                        names.add(namedLine);
                    } else {
                        return null;
                    }
                } else if (isRepeatFunction(token)) {
                    // Parse repeat function
                    GridTemplateComponent component = parseRepeat(token);
                    if (component != null) {
                        repeats.add(component);
                        trackIndex++;
                    } else {
                        return null;
                    }
                } else {
                    // Parse single track
                    TrackSizingFunction track = parseTrack(token);
                    if (track != null) {
                        simples.add(track);
                        repeats.add(GridTemplateComponent.single(track));
                        trackIndex++;
                    } else {
                        return null;
                    }
                }
            }

            return new GridTemplate(
                    Collections.unmodifiableList(simples),
                    Collections.unmodifiableList(repeats),
                    Collections.unmodifiableList(names)
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Tokenization ====================

    private static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inBracket = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '[') {
                if (!current.isEmpty() && !inBracket) {
                    tokens.add(current.toString().trim());
                    current.setLength(0);
                }
                inBracket = true;
                current.append(c);
            } else if (c == ']') {
                current.append(c);
                inBracket = false;
                tokens.add(current.toString().trim());
                current.setLength(0);
            } else if (c == '(') {
                depth++;
                current.append(c);
            } else if (c == ')') {
                depth--;
                current.append(c);
            } else if (Character.isWhitespace(c) && depth == 0 && !inBracket) {
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

    // ==================== Token Classification ====================

    private static boolean isNamedLine(String token) {
        return token.startsWith("[") && token.endsWith("]");
    }

    private static boolean isRepeatFunction(String token) {
        return token.startsWith("repeat(");
    }

    private static boolean isFunction(String token) {
        return token.contains("(") && token.endsWith(")");
    }

    // ==================== Component Parsing ====================

    private static NamedGridLine parseNamedLine(String token, int index) {
        String name = token.substring(1, token.length() - 1).trim();
        if (name.isEmpty()) return null;
        return new NamedGridLine(name, index);
    }

    private static GridTemplateComponent parseRepeat(String token) {
        try {
            String args = extractFunctionArgs(token);
            String[] parts = splitArgs(args);

            if (parts.length < 2) return null;

            String countOrAuto = parts[0].trim();
            List<TrackSizingFunction> tracks = new ArrayList<>();

            // Parse tracks
            for (int i = 1; i < parts.length; i++) {
                TrackSizingFunction track = parseTrack(parts[i].trim());
                if (track != null) {
                    tracks.add(track);
                }
            }

            if (tracks.isEmpty()) return null;

            // Parse repetition type
            if ("auto-fill".equals(countOrAuto)) {
                return GridTemplateComponent.repeat(GridRepetition.autoFill(tracks));
            } else if ("auto-fit".equals(countOrAuto)) {
                return GridTemplateComponent.repeat(GridRepetition.autoFit(tracks));
            } else {
                try {
                    int count = Integer.parseInt(countOrAuto);
                    return GridTemplateComponent.repeat(GridRepetition.count(count, tracks));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

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

        if (current.length() > 0) {
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

    public static String toString(GridTemplate template) {
        if (template == null || template == GridTemplate.EMPTY) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int nameIndex = 0;
        var names = template.names();

        for (int i = 0; i < template.repeats().size(); i++) {
            // Add named lines before this track
            while (nameIndex < names.size() && names.get(nameIndex).getIndex() == i) {
                if (sb.length() > 0) sb.append(' ');
                sb.append('[').append(names.get(nameIndex).getName()).append(']');
                nameIndex++;
            }

            // Add the track/repeat
            if (sb.length() > 0) sb.append(' ');
            sb.append(componentToString(template.repeats().get(i)));
        }

        // Add any trailing named lines
        while (nameIndex < names.size()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append('[').append(names.get(nameIndex).getName()).append(']');
            nameIndex++;
        }

        return sb.toString();
    }

    private static String componentToString(GridTemplateComponent component) {
        if (component.isSingle()) {
            return trackToString(component.getSingle());
        } else if (component.isRepeat()) {
            GridRepetition repeat = component.getRepeat();
            StringBuilder sb = new StringBuilder("repeat(");

            switch (repeat.getType()) {
                case COUNT -> sb.append(repeat.getCount());
                case AUTO_FILL -> sb.append("auto-fill");
                case AUTO_FIT -> sb.append("auto-fit");
            }

            for (TrackSizingFunction track : repeat.getTracks()) {
                sb.append(", ").append(trackToString(track));
            }

            sb.append(')');
            return sb.toString();
        }
        return "";
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
