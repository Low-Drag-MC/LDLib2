package com.lowdragmc.lowdraglib2.client.font;

/**
 * Bottom-left skyline rectangle packer, used to place glyph bitmaps inside a glyph atlas page.
 * <p>
 * The skyline is kept as a list of horizontal segments sorted by x, each segment recording the height
 * that is already occupied over its span. Inserting a rectangle picks the segment where the rectangle
 * would rest lowest (ties broken by the narrower segment, which keeps wide gaps available for later).
 * <p>
 * This class is deliberately free of any Minecraft or OpenGL dependency so it can be unit tested.
 */
public final class SkylinePacker {
    private final int width;
    private final int height;

    private int[] nodeX;
    private int[] nodeY;
    private int[] nodeWidth;
    private int count;

    public SkylinePacker(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Packer size must be positive, got " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.nodeX = new int[16];
        this.nodeY = new int[16];
        this.nodeWidth = new int[16];
        this.nodeX[0] = 0;
        this.nodeY[0] = 0;
        this.nodeWidth[0] = width;
        this.count = 1;
    }

    /**
     * Tries to place a rectangle of the given size.
     *
     * @param w   rectangle width
     * @param h   rectangle height
     * @param out receives the position, {@code out[0] = x}, {@code out[1] = y}. Only written on success.
     * @return true if the rectangle was placed
     */
    public boolean insert(int w, int h, int[] out) {
        if (w <= 0 || h <= 0 || w > width || h > height) {
            return false;
        }
        var bestIndex = -1;
        var bestX = 0;
        var bestY = Integer.MAX_VALUE;
        var bestWidth = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            var y = rest(i, w);
            if (y < 0 || y + h > height) continue;
            if (y < bestY || (y == bestY && nodeWidth[i] < bestWidth)) {
                bestIndex = i;
                bestX = nodeX[i];
                bestY = y;
                bestWidth = nodeWidth[i];
            }
        }
        if (bestIndex < 0) {
            return false;
        }
        addNode(bestIndex, bestX, bestY + h, w);
        out[0] = bestX;
        out[1] = bestY;
        return true;
    }

    /**
     * @return the y a rectangle of width {@code w} would rest at when its left edge is aligned to node
     * {@code index}, or -1 if it does not fit horizontally.
     */
    private int rest(int index, int w) {
        var x = nodeX[index];
        if (x + w > width) {
            return -1;
        }
        var y = nodeY[index];
        var remaining = w;
        var i = index;
        while (remaining > 0) {
            if (i >= count) return -1;
            y = Math.max(y, nodeY[i]);
            remaining -= nodeWidth[i];
            i++;
        }
        return y;
    }

    private void addNode(int index, int x, int y, int w) {
        ensureCapacity(count + 1);
        var tail = count - index;
        if (tail > 0) {
            System.arraycopy(nodeX, index, nodeX, index + 1, tail);
            System.arraycopy(nodeY, index, nodeY, index + 1, tail);
            System.arraycopy(nodeWidth, index, nodeWidth, index + 1, tail);
        }
        nodeX[index] = x;
        nodeY[index] = y;
        nodeWidth[index] = w;
        count++;

        // trim the nodes the freshly inserted one overlaps
        for (int i = index + 1; i < count; ) {
            var overlap = (nodeX[i - 1] + nodeWidth[i - 1]) - nodeX[i];
            if (overlap <= 0) break;
            nodeX[i] += overlap;
            nodeWidth[i] -= overlap;
            if (nodeWidth[i] > 0) break;
            removeNode(i);
        }

        // merge neighbours sitting at the same height
        for (int i = 0; i < count - 1; ) {
            if (nodeY[i] == nodeY[i + 1]) {
                nodeWidth[i] += nodeWidth[i + 1];
                removeNode(i + 1);
            } else {
                i++;
            }
        }
    }

    private void removeNode(int index) {
        var tail = count - index - 1;
        if (tail > 0) {
            System.arraycopy(nodeX, index + 1, nodeX, index, tail);
            System.arraycopy(nodeY, index + 1, nodeY, index, tail);
            System.arraycopy(nodeWidth, index + 1, nodeWidth, index, tail);
        }
        count--;
    }

    private void ensureCapacity(int required) {
        if (required <= nodeX.length) return;
        var newSize = Math.max(required, nodeX.length * 2);
        var x = new int[newSize];
        var y = new int[newSize];
        var w = new int[newSize];
        System.arraycopy(nodeX, 0, x, 0, count);
        System.arraycopy(nodeY, 0, y, 0, count);
        System.arraycopy(nodeWidth, 0, w, 0, count);
        nodeX = x;
        nodeY = y;
        nodeWidth = w;
    }
}
