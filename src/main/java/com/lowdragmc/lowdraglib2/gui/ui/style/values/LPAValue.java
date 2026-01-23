package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS LengthPercentageAuto syntax.
 *
 * Supported syntax:
 * <pre>
 * auto                  // Auto sizing
 * 100px or 100          // Absolute length
 * 50%                   // Percentage
 * min-content           // Intrinsic minimum size
 * max-content           // Intrinsic maximum size
 * fit-content           // Clamped intrinsic size
 * stretch               // Fill available space
 * </pre>
 */
public class LPAValue extends StyleValue<LengthPercentageAuto> {

    public LPAValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable LengthPercentageAuto doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static LengthPercentageAuto parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return LengthPercentageAuto.auto();
        }

        String trimmed = rawValue.trim().toLowerCase();

        try {
            // Check for keywords
            return switch (trimmed) {
                case "auto" -> LengthPercentageAuto.auto();
                case "min-content" -> LengthPercentageAuto.minContent();
                case "max-content" -> LengthPercentageAuto.maxContent();
                case "fit-content" -> LengthPercentageAuto.fitContent();
                case "stretch" -> LengthPercentageAuto.stretch();
                default -> {
                    // Check for percentage
                    if (trimmed.endsWith("%")) {
                        String numberPart = trimmed.substring(0, trimmed.length() - 1).trim();
                        float percentage = Float.parseFloat(numberPart);
                        // CSS percentages are 0-100, but Taffy uses 0.0-1.0
                        yield LengthPercentageAuto.percent(percentage / 100f);
                    }

                    // Check for length with "px" unit
                    if (trimmed.endsWith("px")) {
                        String numberPart = trimmed.substring(0, trimmed.length() - 2).trim();
                        float length = Float.parseFloat(numberPart);
                        yield LengthPercentageAuto.length(length);
                    }

                    // Try parsing as plain number (assume pixels)
                    float length = Float.parseFloat(trimmed);
                    yield LengthPercentageAuto.length(length);
                }
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(LengthPercentageAuto value) {
        if (value == null) {
            return "auto";
        }

        return switch (value.getType()) {
            case AUTO -> "auto";
            case LENGTH -> value.getValue() + "px";
            case PERCENT -> (value.getValue() * 100) + "%";  // Convert 0.0-1.0 to 0-100
            case CALC -> "calc(...)";  // calc expressions can't be fully serialized
            case MIN_CONTENT -> "min-content";
            case MAX_CONTENT -> "max-content";
            case FIT_CONTENT -> "fit-content";
            case STRETCH -> "stretch";
        };
    }
}
