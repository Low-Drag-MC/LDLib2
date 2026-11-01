package com.lowdragmc.lowdraglib2.test.gametest.syncdata.bench;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Collects benchmark sections and rewrites {@code <game dir>/ldlib2-syncbench/report.md} after each one.
 */
public final class SyncBenchReport {
    private static final Map<String, String> SECTIONS = new LinkedHashMap<>();

    private SyncBenchReport() {}

    public record MicroRow(String profile, String fields, boolean active, double nsPerHolder, double nsMin, double nsMax,
                           double allocPerHolder, double packetsPerHolder, double bytesPerPacket,
                           double persistNoticesPerHolder, double initMicrosPerHolder) {}

    public static void micro(int holders, List<MicroRow> rows) {
        var out = new StringBuilder();
        out.append("## Micro: one sync period per holder, run inline on the server thread\n\n")
                .append(holders).append(" holders per profile; each row is the median of the measured rounds. ")
                .append("Idle = nothing changes, so the cost is pure change detection. ")
                .append("Active = one field changes every period (the change itself is inside the timing), ")
                .append("so the cost adds encoding the packet and handing it to the chunk's watchers.\n\n")
                .append("| profile | fields | state | ns / holder / period | (min-max) | alloc B / holder / period | ")
                .append("packets / holder / period | data B / packet | persist notices / holder / period | ")
                .append("init us / holder | ms / period @10k holders |\n")
                .append("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (var row : rows) {
            out.append("| ").append(row.profile)
                    .append(" | ").append(row.fields)
                    .append(" | ").append(row.active ? "active" : "idle")
                    .append(" | ").append(fmt(row.nsPerHolder))
                    .append(" | ").append(fmt(row.nsMin)).append("-").append(fmt(row.nsMax))
                    .append(" | ").append(fmt(row.allocPerHolder))
                    .append(" | ").append(fmt(row.packetsPerHolder))
                    .append(" | ").append(fmt(row.bytesPerPacket))
                    .append(" | ").append(fmt(row.persistNoticesPerHolder))
                    .append(" | ").append(fmt(row.initMicrosPerHolder))
                    .append(" | ").append(fmt(row.nsPerHolder * 10_000 / 1_000_000))
                    .append(" |\n");
        }
        put("micro", out.toString());
    }

    public static void section(String key, String title, List<String> lines) {
        var out = new StringBuilder("## ").append(title).append("\n\n");
        for (var line : lines) {
            out.append("- ").append(line).append('\n');
        }
        put(key, out.toString());
    }

    private static void put(String key, String body) {
        SECTIONS.put(key, body);
        LDLib2.LOGGER.info("[syncbench]\n{}", body);
        write();
    }

    private static void write() {
        var runtime = Runtime.getRuntime();
        var header = new ArrayList<String>();
        header.add("# LDLib2 sync benchmark");
        header.add("");
        header.add("- date: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        header.add("- java: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        header.add("- os: " + System.getProperty("os.name") + ", " + runtime.availableProcessors() + " cores, max heap "
                + runtime.maxMemory() / (1024 * 1024) + " MB");
        header.add("");
        var path = Platform.getGamePath().resolve("ldlib2-syncbench").resolve("report.md");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, String.join("\n", header) + "\n" + String.join("\n", SECTIONS.values()));
        } catch (IOException e) {
            LDLib2.LOGGER.error("Failed to write the sync benchmark report to {}", path, e);
        }
    }

    public static String fmt(double value) {
        if (value == 0) return "0";
        if (Math.abs(value) >= 100) return String.format(Locale.ROOT, "%.0f", value);
        if (Math.abs(value) >= 1) return String.format(Locale.ROOT, "%.1f", value);
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
