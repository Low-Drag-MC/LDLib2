package com.lowdragmc.lowdraglib2.gui.ui.elements;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;

final class LocalizedNumberText {
    private LocalizedNumberText() {
    }

    static float parseFloat(String text) {
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException ignored) {
            return (float) parseLocalizedDouble(text);
        }
    }

    static double parseDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return parseLocalizedDouble(text);
        }
    }

    static String normalizeFloat(String text) {
        try {
            Float.parseFloat(text);
            return text;
        } catch (NumberFormatException ignored) {
            return canonicalizeFloat(text);
        }
    }

    static String normalizeDouble(String text) {
        try {
            Double.parseDouble(text);
            return text;
        } catch (NumberFormatException ignored) {
            return canonicalizeDouble(text);
        }
    }

    static String canonicalizeFloat(String text) {
        return Float.toString(parseFloat(text));
    }

    static String canonicalizeDouble(String text) {
        return Double.toString(parseDouble(text));
    }

    static boolean isFloatingPointCharacter(char chr) {
        return chr == '.' || chr == decimalSeparator() || Character.isDigit(chr) || chr == '-' || chr == '+';
    }

    private static double parseLocalizedDouble(String text) {
        var trimmed = text.trim();
        var symbols = DecimalFormatSymbols.getInstance();
        var decimalSeparator = symbols.getDecimalSeparator();
        if (decimalSeparator == '.' || trimmed.indexOf(decimalSeparator) < 0) {
            throw new NumberFormatException("Not a localized decimal: " + text);
        }

        var groupingSeparator = symbols.getGroupingSeparator();
        if (groupingSeparator != decimalSeparator && trimmed.indexOf(groupingSeparator) >= 0) {
            throw new NumberFormatException("Grouping separators are not supported: " + text);
        }

        var numberFormat = NumberFormat.getNumberInstance();
        numberFormat.setGroupingUsed(false);
        if (numberFormat instanceof DecimalFormat decimalFormat) {
            decimalFormat.setParseBigDecimal(true);
        }

        var parsePosition = new ParsePosition(0);
        var parsed = numberFormat.parse(trimmed, parsePosition);
        if (parsed == null || parsePosition.getIndex() != trimmed.length()) {
            throw new NumberFormatException("Invalid localized number: " + text);
        }
        return parsed.doubleValue();
    }

    private static char decimalSeparator() {
        var numberFormat = NumberFormat.getNumberInstance();
        if (numberFormat instanceof DecimalFormat decimalFormat) {
            return decimalFormat.getDecimalFormatSymbols().getDecimalSeparator();
        }
        return DecimalFormatSymbols.getInstance().getDecimalSeparator();
    }
}
