package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.utils.FluidHelper;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TextFormattingUtil {

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    private static final NavigableMap<Long, String> suffixesBucket = new TreeMap<>();

    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");

        suffixesBucket.put(1L, "m");
        suffixesBucket.put(1_000L, "");
        suffixesBucket.put(1_000_000L, "k");
        suffixesBucket.put(1_000_000_000L, "M");
        suffixesBucket.put(1_000_000_000_000L, "G");
        suffixesBucket.put(1_000_000_000_000_000L, "T");
        suffixesBucket.put(1_000_000_000_000_000_000L, "P");
    }

    public static String formatLongToCompactString(long value, int precision) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return formatLongToCompactString(Long.MIN_VALUE + 1, precision);
        if (value < 0) return "-" + formatLongToCompactString(-value, precision);
        if (value < Math.pow(10, precision)) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10d);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

    public static String formatLongToCompactStringBuckets(long value, int precision) {
        if (value == 0) return value + "";
        value = value * 1000 / FluidHelper.getBucket();
        if (value == 0) return String.format("%sm", new DecimalFormat("0.####").format(value * 1000d / FluidHelper.getBucket()));
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return formatLongToCompactStringBuckets(Long.MIN_VALUE + 1, precision);
        if (value < 0) return "-" + formatLongToCompactStringBuckets(-value, precision);
        if (value < Math.pow(10, precision)) return value + suffixesBucket.floorEntry(value).getValue(); //deal with easy case

        Map.Entry<Long, String> e = suffixesBucket.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10d);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }

}
