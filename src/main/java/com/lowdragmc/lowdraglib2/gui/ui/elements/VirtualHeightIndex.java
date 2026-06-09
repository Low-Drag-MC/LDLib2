package com.lowdragmc.lowdraglib2.gui.ui.elements;

import java.util.Arrays;

public class VirtualHeightIndex {
    private VirtualItemHeightMode mode = VirtualItemHeightMode.VARIABLE;
    private int itemCount;
    private float estimatedHeight = 1;
    private float[] measuredHeights = new float[0];
    private boolean[] measured = new boolean[0];
    private float[] deltas = new float[1];
    private float totalDelta;

    public void setMode(VirtualItemHeightMode mode) {
        this.mode = mode;
    }

    public VirtualItemHeightMode getMode() {
        return mode;
    }

    public void reset(int itemCount, float estimatedHeight) {
        this.itemCount = Math.max(0, itemCount);
        this.estimatedHeight = Math.max(0.001f, estimatedHeight);
        this.measuredHeights = new float[this.itemCount];
        this.measured = new boolean[this.itemCount];
        this.deltas = new float[this.itemCount + 1];
        this.totalDelta = 0;
    }

    public int getItemCount() {
        return itemCount;
    }

    public float getEstimatedHeight() {
        return estimatedHeight;
    }

    public float getTotalHeight() {
        return itemCount * estimatedHeight + totalDelta;
    }

    public float getOffset(int index) {
        if (index <= 0 || itemCount == 0) {
            return 0;
        }
        if (index >= itemCount) {
            return getTotalHeight();
        }
        return index * estimatedHeight + prefixDelta(index);
    }

    public int findIndexAtOffset(float offset) {
        if (itemCount <= 0) {
            return 0;
        }
        if (offset <= 0) {
            return 0;
        }
        if (offset >= getTotalHeight()) {
            return itemCount - 1;
        }

        var low = 0;
        var high = itemCount - 1;
        while (low <= high) {
            var mid = (low + high) >>> 1;
            var start = getOffset(mid);
            var end = getOffset(mid + 1);
            if (offset < start) {
                high = mid - 1;
            } else if (offset >= end) {
                low = mid + 1;
            } else {
                return mid;
            }
        }
        return Math.max(0, Math.min(itemCount - 1, low));
    }

    public boolean updateMeasuredHeight(int index, float measuredHeight) {
        if (mode == VirtualItemHeightMode.FIXED || index < 0 || index >= itemCount) {
            return false;
        }

        var height = Math.max(0, measuredHeight);
        var oldDelta = measured[index] ? measuredHeights[index] - estimatedHeight : 0;
        var newDelta = height - estimatedHeight;
        if (Math.abs(oldDelta - newDelta) <= 0.001f) {
            return false;
        }

        measured[index] = true;
        measuredHeights[index] = height;
        addDelta(index, newDelta - oldDelta);
        totalDelta += newDelta - oldDelta;
        return true;
    }

    public void clearMeasurements() {
        Arrays.fill(measuredHeights, 0);
        Arrays.fill(measured, false);
        Arrays.fill(deltas, 0);
        totalDelta = 0;
    }

    private void addDelta(int index, float delta) {
        for (var i = index + 1; i < deltas.length; i += i & -i) {
            deltas[i] += delta;
        }
    }

    private float prefixDelta(int itemCount) {
        var sum = 0f;
        for (var i = itemCount; i > 0; i -= i & -i) {
            sum += deltas[i];
        }
        return sum;
    }
}
