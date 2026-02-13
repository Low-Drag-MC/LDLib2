package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.DimensionSize;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS size/min-size/max-size shorthand syntax using TaffyDimension.
 *
 * Supported syntax (following CSS):
 * <pre>
 * width: 10px;            // Both dimensions (when used as shorthand)
 * size: 10px 20px;        // Width Height
 * size: auto;             // Auto sizing
 * size: 50% 100px;        // Width Height with mixed units
 * </pre>
 */
public class DimensionSizeValue extends StyleValue<DimensionSize> {

    public DimensionSizeValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable DimensionSize doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static DimensionSize parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return DimensionSize.AUTO;
        }

        try {
            // Split by whitespace
            String[] parts = rawValue.trim().split("\\s+");

            if (parts.length == 0) {
                return DimensionSize.AUTO;
            }

            // Parse each part using TaffyDimension syntax
            TaffyDimension width = DimensionValue.parse(parts[0]);
            if (width == null) {
                return null;
            }

            TaffyDimension height;
            if (parts.length >= 2) {
                height = DimensionValue.parse(parts[1]);
                if (height == null) {
                    return null;
                }
            } else {
                // If only one value, use it for both dimensions
                height = width;
            }

            return new DimensionSize(new TaffySize<>(width, height));
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(DimensionSize value) {
        if (value == null || value.size() == null) {
            return "auto";
        }

        TaffySize<TaffyDimension> size = value.size();

        String widthStr = DimensionValue.toString(size.width);
        String heightStr = DimensionValue.toString(size.height);

        // Optimize output: if both are same, only output once
        if (widthStr.equals(heightStr)) {
            return widthStr;
        }

        return widthStr + " " + heightStr;
    }
}
