package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.style.TaffyDimension;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS TaffyDimension syntax.
 *
 * Supported syntax:
 * <pre>
 * auto                  // Auto sizing
 * 100px or 100          // Absolute length
 * 50%                   // Percentage
 * </pre>
 */
public class DimensionValue extends StyleValue<TaffyDimension> {

    public DimensionValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable TaffyDimension doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static TaffyDimension parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return TaffyDimension.AUTO;
        }

        String trimmed = rawValue.trim().toLowerCase();

        try {
            // Check for auto keyword
            switch (trimmed) {
                case "auto" -> {
                    return TaffyDimension.AUTO;
                }
                case "fit-content" -> {
                    return TaffyDimension.FIT_CONTENT;
                }
                case "stretch" -> {
                    return TaffyDimension.STRETCH;
                }
                case "max-content" -> {
                    return TaffyDimension.MAX_CONTENT;
                }
                case "min-content" -> {
                    return TaffyDimension.MIN_CONTENT;
                }
            }

            // Check for percentage
            if (trimmed.endsWith("%")) {
                String numberPart = trimmed.substring(0, trimmed.length() - 1).trim();
                float percentage = Float.parseFloat(numberPart);
                // CSS percentages are 0-100, but Taffy uses 0.0-1.0
                return TaffyDimension.percent(percentage / 100f);
            }

            // Check for length with "px" unit
            if (trimmed.endsWith("px")) {
                String numberPart = trimmed.substring(0, trimmed.length() - 2).trim();
                float length = Float.parseFloat(numberPart);
                return TaffyDimension.length(length);
            }

            // Try parsing as plain number (assume pixels)
            float length = Float.parseFloat(trimmed);
            return TaffyDimension.length(length);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(TaffyDimension value) {
        if (value == null || value.isAuto()) {
            return "auto";
        }

        if (value.isLength()) {
            return value.getValue() + "px";
        }

        if (value.isPercent()) {
            // Convert 0.0-1.0 to 0-100
            return (value.getValue() * 100) + "%";
        }

        return "auto";
    }
}
