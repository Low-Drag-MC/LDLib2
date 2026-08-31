package com.lowdragmc.lowdraglib2.uitest.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Function;

/**
 * The pieces a test orchestrator needs to fold several processes' reports into one.
 *
 * <p>Shared by the multi-process and parallel orchestrators, which merge on different axes — one
 * combines the <i>same</i> scenario seen by several roles, the other concatenates disjoint slices —
 * but agree on how a report is read, how a missing one is represented, and how the result is
 * written. Runs outside the game, so no Minecraft here.
 */
public final class ReportMerge {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private ReportMerge() {
    }

    /** Reads one process's report, or {@code null} if it is missing or unreadable. */
    public static RunReport read(Path reportFile) {
        if (!Files.isRegularFile(reportFile)) return null;
        try (var reader = Files.newBufferedReader(reportFile, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, RunReport.class);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * A stand-in scenario carrying a failure that belongs to the run rather than to any scenario —
     * a process that left no report, a selection that matched nothing.
     *
     * <p>It has to be a scenario entry rather than only a status, because {@link ReportWriter#finalise}
     * recounts the totals from the scenario list: a bare status would be overwritten, and the run
     * would go green with an empty report. That is the exact shape of the bug this guards.
     */
    public static RunReport.ScenarioReport syntheticError(String name, String message) {
        var scenario = new RunReport.ScenarioReport();
        scenario.name = name;
        scenario.status = RunReport.Status.ERROR;
        scenario.error = RunReport.ErrorInfo.of(new IllegalStateException(message));
        return scenario;
    }

    /**
     * Finalises and writes the merged report and its text summary.
     *
     * <p>Sets {@code finishedAt} before finalising, because {@link ReportWriter#finalise} derives
     * {@code durationMs} from it, and re-sums {@code totals.captures} afterwards, because finalise
     * recounts everything <em>except</em> that from the scenario tree.
     *
     * <p>{@code summariser} is a function rather than a finished string so it cannot run too early:
     * every total it would want to quote is filled in by {@code finalise}, and a summary built
     * before that call reads "0/0 scenarios" over a report that holds twenty passing ones.
     */
    public static void write(RunReport merged, Collection<RunReport> parts, Path outDir,
                             Function<RunReport, String> summariser) throws IOException {
        merged.finishedAt = System.currentTimeMillis();
        ReportWriter.finalise(merged);
        merged.totals.captures = parts.stream().mapToInt(part -> part.totals.captures).sum();
        Files.createDirectories(outDir);
        Files.writeString(outDir.resolve(ReportWriter.REPORT_FILE), GSON.toJson(merged));
        Files.writeString(outDir.resolve(ReportWriter.SUMMARY_FILE), summariser.apply(merged));
    }
}
