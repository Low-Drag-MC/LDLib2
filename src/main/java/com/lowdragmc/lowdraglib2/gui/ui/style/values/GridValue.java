package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.Grid;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridPlacement;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS grid-row and grid-column syntax.
 *
 * Supported syntax:
 * <pre>
 * grid-row: auto;
 * grid-row: 1;                     // Line 1 (start), auto (end)
 * grid-row: 1 / 3;                 // Line 1 to line 3
 * grid-row: span 2;                // Span 2 tracks
 * grid-row: 1 / span 2;            // Start at line 1, span 2 tracks
 * grid-row: header;                // Named line (start)
 * grid-row: header / footer;       // Named lines
 * grid-row: header 2 / footer;     // 2nd occurrence of "header" line
 * grid-row: span header;           // Span until "header" line
 * grid-row: span header 2;         // Span until 2nd "header" line
 * grid-row: -1;                    // Last line (negative indexing)
 * </pre>
 */
public class GridValue extends StyleValue<Grid> {

    public GridValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable Grid doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static Grid parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return Grid.EMPTY;
        }

        try {
            String trimmed = rawValue.trim();

            // Check for shorthand syntax with "/"
            if (trimmed.contains("/")) {
                String[] parts = trimmed.split("/", 2);
                GridPlacement start = parsePlacement(parts[0].trim());
                GridPlacement end = parsePlacement(parts[1].trim());

                if (start == null || end == null) {
                    return null;
                }

                return new Grid(new TaffyLine<>(start, end));
            } else {
                // Single value applies to start only, end is auto
                GridPlacement start = parsePlacement(trimmed);

                if (start == null) {
                    return null;
                }

                return new Grid(new TaffyLine<>(start, GridPlacement.auto()));
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses a single grid placement value.
     *
     * Syntax:
     * - "auto" → auto placement
     * - "<integer>" → line number (1-based, negative from end)
     * - "<name>" → named line (first occurrence)
     * - "<name> <integer>" → named line (nth occurrence)
     * - "span <integer>" → span N tracks
     * - "span <name>" → span until named line (first occurrence)
     * - "span <name> <integer>" → span until nth named line
     */
    private static GridPlacement parsePlacement(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        String trimmed = value.trim();

        // Check for "auto"
        if (trimmed.equalsIgnoreCase("auto")) {
            return GridPlacement.auto();
        }

        // Check for "span" syntax
        if (trimmed.toLowerCase().startsWith("span ")) {
            return parseSpan(trimmed.substring(5).trim());
        }

        // Check for integer (line number)
        try {
            int lineNum = Integer.parseInt(trimmed);
            return GridPlacement.line(lineNum);
        } catch (NumberFormatException e) {
            // Not an integer, try as named line
        }

        // Try parsing as named line
        return parseNamedLine(trimmed);
    }

    /**
     * Parses span syntax: "span <integer>" or "span <name>" or "span <name> <integer>"
     */
    private static GridPlacement parseSpan(String spanValue) {
        if (spanValue.isEmpty()) {
            return null;
        }

        // Try parsing as "span <integer>"
        try {
            int spanCount = Integer.parseInt(spanValue);
            return GridPlacement.span(spanCount);
        } catch (NumberFormatException e) {
            // Not just an integer, could be "span <name>" or "span <name> <integer>"
        }

        // Split by whitespace
        String[] parts = spanValue.split("\\s+");

        if (parts.length == 1) {
            // "span <name>"
            return GridPlacement.namedSpan(parts[0], 1);
        } else if (parts.length >= 2) {
            // "span <name> <integer>"
            String name = parts[0];
            try {
                int count = Integer.parseInt(parts[1]);
                return GridPlacement.namedSpan(name, count);
            } catch (NumberFormatException e) {
                // If second part isn't a number, treat entire thing as name
                return GridPlacement.namedSpan(spanValue, 1);
            }
        }

        return null;
    }

    /**
     * Parses named line syntax: "<name>" or "<name> <integer>"
     */
    private static GridPlacement parseNamedLine(String value) {
        if (value.isEmpty()) {
            return null;
        }

        // Split by whitespace
        String[] parts = value.split("\\s+");

        if (parts.length == 1) {
            // Just a name
            return GridPlacement.namedLine(parts[0]);
        } else if (parts.length >= 2) {
            // "<name> <integer>" for nth occurrence
            String name = parts[0];
            try {
                int nthIndex = Integer.parseInt(parts[1]);
                return GridPlacement.namedLine(name, nthIndex);
            } catch (NumberFormatException e) {
                // If second part isn't a number, treat entire thing as a single name
                return GridPlacement.namedLine(value);
            }
        }

        return null;
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(Grid grid) {
        if (grid == null || grid == Grid.EMPTY) {
            return "auto";
        }

        TaffyLine<GridPlacement> line = grid.grid();
        String startStr = placementToString(line.start);
        String endStr = placementToString(line.end);

        // If end is auto, just return start
        if (line.end.isAuto()) {
            return startStr;
        }

        // Return full shorthand
        return startStr + " / " + endStr;
    }

    private static String placementToString(GridPlacement placement) {
        if (placement == null || placement.isAuto()) {
            return "auto";
        }

        return switch (placement.getType()) {
            case AUTO -> "auto";
            case LINE -> String.valueOf(placement.getValue());
            case NAMED_LINE -> {
                int nthIndex = placement.getNthIndex();
                if (nthIndex == 1) {
                    yield placement.getLineName();
                } else {
                    yield placement.getLineName() + " " + nthIndex;
                }
            }
            case SPAN -> "span " + placement.getValue();
            case NAMED_SPAN -> {
                int count = placement.getValue();
                if (count == 1) {
                    yield "span " + placement.getLineName();
                } else {
                    yield "span " + placement.getLineName() + " " + count;
                }
            }
        };
    }
}
