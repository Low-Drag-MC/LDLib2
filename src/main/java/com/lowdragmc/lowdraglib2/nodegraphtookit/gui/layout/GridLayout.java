package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.layout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Packs the boxes into an even grid, ignoring the wires entirely.
 *
 * <p>The one that earns its place by not being clever: a pile of unconnected constants, a scratch
 * area of nodes dragged out of the library, a set of subgraph entry points. Connectivity-driven
 * layouts have nothing to work with there and scatter them; a grid just makes them readable.</p>
 *
 * <p>Boxes keep their reading order — sorted by current row, then by position along the row — so
 * this tidies an arrangement rather than reshuffling it.</p>
 */
public class GridLayout implements IGraphLayout {

    @Override
    public void layout(List<LayoutBox> boxes, List<LayoutEdge> edges, LayoutOptions options) {
        var n = boxes.size();
        if (n == 0) return;
        var direction = options.direction();
        var spacingFlow = options.layerSpacing();
        var spacingCross = options.nodeSpacing();

        // Band the cross-axis coordinate before sorting, so a row of nodes that are a few pixels
        // out of alignment is still read as one row instead of a staircase.
        var band = 0f;
        for (var box : boxes) band += box.crossSize(direction);
        band = Math.max(1f, band / n);
        final var bandSize = band;
        final var dir = direction;

        var order = new ArrayList<>(boxes);
        order.sort(Comparator
                .comparingLong((LayoutBox box) -> Math.round(box.cross(dir) / bandSize))
                .thenComparingDouble(box -> box.flow(dir)));

        var columns = (int) Math.ceil(Math.sqrt(n));
        var rows = (int) Math.ceil(n / (double) columns);

        // Uniform tracks: every box in a column shares its width, every box in a row its height.
        var columnExtent = new float[columns];
        var rowExtent = new float[rows];
        for (var i = 0; i < n; i++) {
            var box = order.get(i);
            var column = i % columns;
            var row = i / columns;
            columnExtent[column] = Math.max(columnExtent[column], box.flowSize(dir));
            rowExtent[row] = Math.max(rowExtent[row], box.crossSize(dir));
        }

        var columnStart = new float[columns];
        for (var c = 1; c < columns; c++) columnStart[c] = columnStart[c - 1] + columnExtent[c - 1] + spacingFlow;
        var rowStart = new float[rows];
        for (var r = 1; r < rows; r++) rowStart[r] = rowStart[r - 1] + rowExtent[r - 1] + spacingCross;

        for (var i = 0; i < n; i++) {
            var box = order.get(i);
            box.setFlow(dir, columnStart[i % columns]);
            box.setCross(dir, rowStart[i / columns]);
        }
    }
}
