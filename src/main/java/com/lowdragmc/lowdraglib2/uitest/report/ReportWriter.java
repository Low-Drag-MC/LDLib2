package com.lowdragmc.lowdraglib2.uitest.report;

import com.google.gson.GsonBuilder;
import com.lowdragmc.lowdraglib2.LDLib2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Serialises a {@link RunReport} to disk and prints a human summary to the log.
 */
public final class ReportWriter {

    public static final String REPORT_FILE = "report.json";
    public static final String SUMMARY_FILE = "report.txt";

    private ReportWriter() {
    }

    /** Rolls per-step and per-scenario results up into the totals and the overall status. */
    public static void finalise(RunReport report) {
        var totals = report.totals;
        totals.scenarios = report.scenarios.size();
        totals.passed = 0;
        totals.failed = 0;
        totals.errored = 0;
        totals.skipped = 0;
        totals.steps = 0;
        totals.checks = 0;
        totals.checksFailed = 0;

        for (var scenario : report.scenarios) {
            for (var step : scenario.steps) {
                totals.steps++;
                for (var check : step.checks) {
                    totals.checks++;
                    if (!check.passed) totals.checksFailed++;
                }
                scenario.status = RunReport.Status.worst(scenario.status, step.status);
            }
            switch (scenario.status) {
                case RunReport.Status.PASS -> totals.passed++;
                case RunReport.Status.FAIL -> totals.failed++;
                case RunReport.Status.SKIPPED -> totals.skipped++;
                default -> totals.errored++;
            }
            report.status = RunReport.Status.worst(report.status, scenario.status);
        }
        if (report.durationMs == 0 && report.finishedAt > report.startedAt) {
            report.durationMs = report.finishedAt - report.startedAt;
        }
    }

    /**
     * Writes {@code report.json} atomically.
     *
     * <p>The Gradle verification task reads this file the moment the client exits, and a half-written
     * file would surface as a parse error that looks like a harness bug rather than what it is.
     */
    public static void write(RunReport report, Path outDir) {
        try {
            Files.createDirectories(outDir);
            var gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            var json = gson.toJson(report);
            var temp = outDir.resolve(REPORT_FILE + ".tmp");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, outDir.resolve(REPORT_FILE),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(temp, outDir.resolve(REPORT_FILE), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(outDir.resolve(SUMMARY_FILE), summaryOf(report), StandardCharsets.UTF_8);
            LDLib2.LOGGER.info("[uitest] report written to {}", outDir.resolve(REPORT_FILE));
        } catch (IOException e) {
            LDLib2.LOGGER.error("[uitest] could not write the report to {}", outDir, e);
        }
    }

    public static void logSummary(RunReport report) {
        for (var line : summaryOf(report).split("\n")) {
            if (line.contains("FAIL") || line.contains("ERROR") || line.contains("HUNG")) {
                LDLib2.LOGGER.error("[uitest] {}", line);
            } else {
                LDLib2.LOGGER.info("[uitest] {}", line);
            }
        }
    }

    private static String summaryOf(RunReport report) {
        var out = new StringBuilder();
        out.append("========= LDLib2 UI test run ").append(report.runId).append(" =========\n");
        out.append("selection : ").append(report.selection).append('\n');
        out.append("status    : ").append(report.status).append('\n');
        out.append("scenarios : ").append(report.totals.passed).append('/')
                .append(report.totals.scenarios).append(" passed");
        if (report.totals.failed > 0) out.append(", ").append(report.totals.failed).append(" failed");
        if (report.totals.errored > 0) out.append(", ").append(report.totals.errored).append(" errored");
        out.append('\n');
        out.append("checks    : ").append(report.totals.checks - report.totals.checksFailed).append('/')
                .append(report.totals.checks).append(" passed\n");
        out.append("steps     : ").append(report.totals.steps).append('\n');
        out.append("duration  : ").append(report.durationMs).append(" ms\n");

        for (var scenario : report.scenarios) {
            out.append('\n').append(statusMark(scenario.status)).append(' ').append(scenario.name)
                    .append(" (").append(scenario.durationMs).append(" ms)\n");
            if (scenario.error != null) {
                out.append("    ERROR ").append(scenario.error.type).append(": ")
                        .append(scenario.error.message).append('\n');
            }
            for (var step : scenario.steps) {
                if (RunReport.Status.PASS.equals(step.status)) continue;
                out.append("    ").append(statusMark(step.status)).append(" step ").append(step.index)
                        .append(" [").append(step.kind).append("] ").append(step.name).append('\n');
                if (step.error != null) {
                    out.append("        ").append(step.error.type).append(": ")
                            .append(step.error.message).append('\n');
                }
                for (var check : step.checks) {
                    if (check.passed) continue;
                    out.append("        check '").append(check.desc).append('\'');
                    if (check.expected != null) {
                        out.append(" expected=").append(check.expected).append(" actual=").append(check.actual);
                    }
                    out.append('\n');
                }
                for (var capture : step.captures) {
                    out.append("        shot: ").append(capture.path)
                            .append(capture.suspect ? "  (SUSPECT: flat colour)" : "").append('\n');
                }
            }
        }
        return out.toString();
    }

    private static String statusMark(String status) {
        return switch (status) {
            case RunReport.Status.PASS -> "[ok  ]";
            case RunReport.Status.FAIL -> "[FAIL]";
            case RunReport.Status.SKIPPED -> "[skip]";
            case RunReport.Status.HUNG -> "[HUNG]";
            default -> "[ERR ]";
        };
    }
}
