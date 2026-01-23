package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.gui.ui.data.LPARect;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import org.jetbrains.annotations.Nullable;

/**
 * Parses CSS margin/padding shorthand syntax.
 *
 * Supported syntax (following CSS box model):
 * <pre>
 * margin: 10px;                     // All sides
 * margin: 10px 20px;                // Vertical Horizontal
 * margin: 10px 20px 30px;           // Top Horizontal Bottom
 * margin: 10px 20px 30px 40px;      // Top Right Bottom Left (clockwise)
 * </pre>
 */
public class LPARectValue extends StyleValue<LPARect> {

    public LPARectValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable LPARect doCompute(String rawValue) {
        return parse(rawValue);
    }

    public static LPARect parse(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return new LPARect(TaffyRect.all(LengthPercentageAuto.auto()));
        }

        try {
            // Split by whitespace
            String[] parts = rawValue.trim().split("\\s+");

            if (parts.length == 0) {
                return new LPARect(TaffyRect.all(LengthPercentageAuto.auto()));
            }

            // Parse each part
            LengthPercentageAuto[] values = new LengthPercentageAuto[parts.length];
            for (int i = 0; i < parts.length; i++) {
                values[i] = LPAValue.parse(parts[i]);
                if (values[i] == null) {
                    return null; // Invalid value
                }
            }

            // Apply CSS shorthand rules
            TaffyRect<LengthPercentageAuto> rect = switch (parts.length) {
                case 1 ->
                    // All sides
                    TaffyRect.all(values[0]);
                case 2 ->
                    // Vertical (top/bottom), Horizontal (left/right)
                    TaffyRect.hv(values[1], values[0]);
                case 3 ->
                    // Top, Horizontal (left/right), Bottom
                    new TaffyRect<>(values[1], values[1], values[0], values[2]);
                case 4 ->
                    // Top, Right, Bottom, Left (clockwise from top)
                    new TaffyRect<>(values[3], values[1], values[0], values[2]);
                default ->
                    // Invalid number of values
                    null;
            };

            if (rect == null) {
                return null;
            }

            return new LPARect(rect);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== Serialization to CSS String ====================

    public static String toString(LPARect lpaRect) {
        if (lpaRect == null || lpaRect.rect() == null) {
            return "auto";
        }

        TaffyRect<LengthPercentageAuto> rect = lpaRect.rect();

        // Convert to strings
        String top = LPAValue.toString(rect.top);
        String right = LPAValue.toString(rect.right);
        String bottom = LPAValue.toString(rect.bottom);
        String left = LPAValue.toString(rect.left);

        // Optimize output using CSS shorthand rules
        if (top.equals(right) && right.equals(bottom) && bottom.equals(left)) {
            // All sides are the same
            return top;
        } else if (top.equals(bottom) && left.equals(right)) {
            // Vertical and horizontal pairs
            return top + " " + left;
        } else if (left.equals(right)) {
            // Top, Horizontal, Bottom
            return top + " " + left + " " + bottom;
        } else {
            // All four sides different
            return top + " " + right + " " + bottom + " " + left;
        }
    }
}
