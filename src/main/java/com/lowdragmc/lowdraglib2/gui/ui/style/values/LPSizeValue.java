package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.LPSize;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.LengthPercentage;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS gap/size shorthand syntax using LengthPercentage.
 *
 * Supported syntax (following CSS):
 * <pre>
 * gap: 10px;              // Both dimensions
 * gap: 10px 20px;         // Width Height
 * gap: 50% 100px;         // Width Height with mixed units
 * </pre>
 */
public class LPSizeValue extends StyleValue<LPSize> {

    public LPSizeValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable LPSize doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static LPSize parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return LPSize.ZERO;
        }

        try {
            // Split by whitespace
            String[] parts = rawValue.trim().split("\\s+");

            if (parts.length == 0) {
                return LPSize.ZERO;
            }

            // Parse each part using LengthPercentage syntax
            LengthPercentage width = parseLengthPercentage(parts[0]);
            if (width == null) {
                return null;
            }

            LengthPercentage height;
            if (parts.length >= 2) {
                height = parseLengthPercentage(parts[1]);
                if (height == null) {
                    return null;
                }
            } else {
                // If only one value, use it for both dimensions
                height = width;
            }

            return new LPSize(new TaffySize<>(width, height));
        } catch (Exception e) {
            return null;
        }
    }

    private static LengthPercentage parseLengthPercentage(String value) {
        if (value == null || value.trim().isEmpty()) {
            return LengthPercentage.ZERO;
        }

        String trimmed = value.trim().toLowerCase();

        try {
            // Check for percentage
            if (trimmed.endsWith("%")) {
                String numberPart = trimmed.substring(0, trimmed.length() - 1).trim();
                float percentage = Float.parseFloat(numberPart);
                // CSS percentages are 0-100, but Taffy uses 0.0-1.0
                return LengthPercentage.percent(percentage / 100f);
            }

            // Check for length with "px" unit
            if (trimmed.endsWith("px")) {
                String numberPart = trimmed.substring(0, trimmed.length() - 2).trim();
                float length = Float.parseFloat(numberPart);
                return LengthPercentage.length(length);
            }

            // Try parsing as plain number (assume pixels)
            float length = Float.parseFloat(trimmed);
            return LengthPercentage.length(length);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(LPSize value) {
        if (value == null || value.size() == null) {
            return "0";
        }

        TaffySize<LengthPercentage> size = value.size();

        String widthStr = toLengthPercentageString(size.width);
        String heightStr = toLengthPercentageString(size.height);

        // Optimize output: if both are same, only output once
        if (widthStr.equals(heightStr)) {
            return widthStr;
        }

        return widthStr + " " + heightStr;
    }

    private static String toLengthPercentageString(LengthPercentage lp) {
        if (lp == null) {
            return "0";
        }

        if (lp.isLength()) {
            return lp.getValue() + "px";
        }

        if (lp.isPercent()) {
            // Convert 0.0-1.0 to 0-100
            return (lp.getValue() * 100) + "%";
        }

        return "0";
    }
}
