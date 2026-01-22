package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.GridTemplateAreas;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CSS grid-template-areas syntax.
 *
 * Supported syntax:
 * <pre>
 * grid-template-areas:
 *   "header header header"
 *   "nav main aside"
 *   "footer footer footer";
 * </pre>
 *
 * Each quoted string represents one row of the grid.
 * Use "." or "..." to represent empty cells.
 */
public class GridTemplateAreasValue extends StyleValue<GridTemplateAreas> {

    private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("\"([^\"]*)\"");

    public GridTemplateAreasValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable GridTemplateAreas doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static GridTemplateAreas parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return GridTemplateAreas.EMPTY;
        }

        try {
            // Step 1: Parse CSS syntax into 2D grid of area names
            List<String[]> grid = new ArrayList<>();

            // Extract all quoted strings
            Matcher matcher = QUOTED_STRING_PATTERN.matcher(rawValue);
            while (matcher.find()) {
                String rowContent = matcher.group(1).trim();
                if (!rowContent.isEmpty()) {
                    // Split by whitespace to get individual cell names
                    String[] cells = rowContent.split("\\s+");
                    grid.add(cells);
                }
            }

            if (grid.isEmpty()) {
                return GridTemplateAreas.EMPTY;
            }

            // Validate that all rows have the same number of columns
            int columnCount = grid.get(0).length;
            for (String[] row : grid) {
                if (row.length != columnCount) {
                    // Invalid grid - rows must have same number of columns
                    return null;
                }
            }

            // Step 2: Convert 2D grid to GridTemplateArea objects
            List<GridTemplateArea> areas = convertGridToAreas(grid);

            return new GridTemplateAreas(areas);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts a 2D grid of area names into a list of GridTemplateArea objects.
     * Each unique area name (except ".") gets one GridTemplateArea with its bounding rectangle.
     */
    private static List<GridTemplateArea> convertGridToAreas(List<String[]> grid) {
        Map<String, GridTemplateArea> areaMap = new LinkedHashMap<>();

        int rowCount = grid.size();
        int colCount = grid.get(0).length;

        // Find bounds for each unique area name
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < colCount; col++) {
                String name = grid.get(row)[col];

                // Skip empty cells (represented by "." or "...")
                if (name.equals(".") || name.startsWith(".")) {
                    continue;
                }

                if (!areaMap.containsKey(name)) {
                    // First occurrence - find the bounding rectangle
                    int[] bounds = findAreaBounds(grid, name, row, col);
                    if (bounds != null) {
                        // Taffy uses 1-based line indices
                        // bounds: [minRow, maxRow, minCol, maxCol] (0-based, inclusive)
                        // Convert to 1-based line indices (start line, end line)
                        GridTemplateArea area = new GridTemplateArea(
                            name,
                            bounds[0] + 1,  // rowStart (1-based line before first row)
                            bounds[1] + 2,  // rowEnd (1-based line after last row)
                            bounds[2] + 1,  // columnStart (1-based line before first col)
                            bounds[3] + 2   // columnEnd (1-based line after last col)
                        );
                        areaMap.put(name, area);
                    }
                }
            }
        }

        return new ArrayList<>(areaMap.values());
    }

    /**
     * Finds the bounding rectangle for an area starting from the given position.
     * Returns [minRow, maxRow, minCol, maxCol] (0-based, inclusive) or null if invalid.
     */
    private static int[] findAreaBounds(List<String[]> grid, String name, int startRow, int startCol) {
        int rowCount = grid.size();
        int colCount = grid.get(0).length;

        // Find the extent of this area
        int minRow = startRow;
        int maxRow = startRow;
        int minCol = startCol;
        int maxCol = startCol;

        // Expand right to find max column
        while (maxCol + 1 < colCount && grid.get(startRow)[maxCol + 1].equals(name)) {
            maxCol++;
        }

        // Expand down to find max row
        while (maxRow + 1 < rowCount) {
            boolean canExpand = true;
            for (int col = minCol; col <= maxCol; col++) {
                if (!grid.get(maxRow + 1)[col].equals(name)) {
                    canExpand = false;
                    break;
                }
            }
            if (canExpand) {
                maxRow++;
            } else {
                break;
            }
        }

        // Validate that the area is rectangular
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                if (!grid.get(row)[col].equals(name)) {
                    // Area is not rectangular!
                    return null;
                }
            }
        }

        return new int[]{minRow, maxRow, minCol, maxCol};
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(GridTemplateAreas gridTemplateAreas) {
        if (gridTemplateAreas == null || gridTemplateAreas == GridTemplateAreas.EMPTY) {
            return "";
        }

        List<GridTemplateArea> areas = gridTemplateAreas.areas();
        if (areas.isEmpty()) {
            return "";
        }

        // Determine grid dimensions from area bounds
        int maxRow = 0;
        int maxCol = 0;
        for (GridTemplateArea area : areas) {
            maxRow = Math.max(maxRow, area.getRowEnd());
            maxCol = Math.max(maxCol, area.getColumnEnd());
        }

        // Create 2D grid (1-based indices, so we need maxRow and maxCol cells)
        // Initialize with "." for empty cells
        String[][] grid = new String[maxRow - 1][maxCol - 1];
        for (int r = 0; r < maxRow - 1; r++) {
            for (int c = 0; c < maxCol - 1; c++) {
                grid[r][c] = ".";
            }
        }

        // Fill in the areas
        for (GridTemplateArea area : areas) {
            String name = area.getName();
            // Convert 1-based line indices to 0-based cell indices
            int rowStart = area.getRowStart() - 1;
            int rowEnd = area.getRowEnd() - 1;
            int colStart = area.getColumnStart() - 1;
            int colEnd = area.getColumnEnd() - 1;

            for (int r = rowStart; r < rowEnd; r++) {
                for (int c = colStart; c < colEnd; c++) {
                    grid[r][c] = name;
                }
            }
        }

        // Convert grid to CSS string
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < grid.length; r++) {
            if (r > 0) sb.append(' ');
            sb.append('"');
            for (int c = 0; c < grid[r].length; c++) {
                if (c > 0) sb.append(' ');
                sb.append(grid[r][c]);
            }
            sb.append('"');
        }

        return sb.toString();
    }
}
