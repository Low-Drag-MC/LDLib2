package com.lowdragmc.lowdraglib2.uitest.par;

import com.lowdragmc.lowdraglib2.uitest.ShardWeights;
import com.lowdragmc.lowdraglib2.uitest.proc.ChildBuilds;
import com.lowdragmc.lowdraglib2.uitest.report.ReportMerge;
import com.lowdragmc.lowdraglib2.uitest.report.ReportWriter;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * The entry point of {@code gradlew runParTest}: splits the selection across N client processes,
 * runs them at once, and merges their reports into one {@code report.json}.
 *
 * <p>Each shard is an ordinary {@code -PldTest} run with two extra system properties, launched as a
 * child Gradle build against its own NeoGradle run — and therefore its own {@code runs/} directory,
 * its own saves and its own log. That isolation is not cosmetic: {@code WorldBootstrap} sweeps
 * <em>every</em> {@code ldtest_} world it can see before creating its own, so shards sharing a game
 * directory would delete each other's worlds mid-run.
 *
 * <p>Nothing coordinates the split at runtime. Every shard resolves the same name-sorted selection
 * and runs {@code ShardPlan} over it, keeping its own bucket; this process only supplies the
 * durations that make the buckets even, and afterwards checks that the shards really did agree.
 *
 * <p>Runs <b>outside</b> the game — plain JDK plus Gson, never touching a Minecraft class.
 */
public final class ParallelTestOrchestrator {

    private static final String SHARD_TASK_PREFIX = "runUiTestShard";

    /**
     * <b>The project properties each shard build is launched with.</b>
     *
     * <p>⚠️⚠️ <b>A child build inherits none of this one's project properties</b> — it is a fresh
     * {@code gradlew} invocation — so everything the shard's run configuration reads from
     * {@code project.findProperty} has to be handed over here, by name.
     *
     * <p>That was known for {@code -PldTestHeadless} and missed for every other one. Two kinds of
     * damage came of it, and the second is worse than the first:
     *
     * <ul>
     *   <li><b>What runs.</b> Without {@code -PldTest} a shard's {@code ldlib2.uitest.run} falls
     *       back to {@code all}, so {@code runParTest -PldTest=group:mine} ran every scenario in the
     *       workspace while the parent printed the selection it had been given — the one place a
     *       reader would look. The symptom is a scenario count that is too large, and nothing
     *       else.</li>
     *   <li><b>⚠️⚠️ What it runs in.</b> Without {@code -PldTestWindow} a shard falls back to the
     *       headless default of 1920x1080 — so {@code -PldTestWindow=3840x2160} gave the parallel
     *       run <i>half</i> the logical viewport the same selection got serially. Every layout in
     *       it was half the size, and what that produced was not an error but scenarios failing on
     *       coordinates: a button at the bottom of a pane that is no longer tall enough, clicked
     *       and missed. It reads as a broken widget, in a scenario that passes on its own.</li>
     * </ul>
     *
     * <p>Each is passed only when it was given: an empty {@code -PldTestExclude=} would still make
     * {@code project.hasProperty} true downstream, which is a different thing from not asking to
     * exclude anything, and a blank window would override nothing while looking like it does.
     *
     * <p>⚠️ Each of these is one argument to {@code ProcessBuilder}, so a value needs no quoting —
     * but on Windows the child goes through {@code cmd.exe /c}, which does its own parsing, and a
     * {@code regex:} selection containing spaces or {@code &} would not survive it. Selections of
     * that shape have to be given to a serial run.
     */
    static List<String> childProperties(Map<String, String> options, int jobs, boolean headless) {
        var properties = new ArrayList<String>();
        properties.add("-PldTestJobs=" + jobs);
        properties.add("-PldTest=" + options.getOrDefault("selection", "all"));
        forward(properties, options, "exclude", "-PldTestExclude");
        forward(properties, options, "window", "-PldTestWindow");
        forward(properties, options, "guiScale", "-PldTestGuiScale");
        forward(properties, options, "inputMode", "-PldTestInputMode");
        forward(properties, options, "watchdogSec", "-PldTestWatchdogSec");
        if (headless) {
            properties.add("-PldTestHeadless");
        }
        return List.copyOf(properties);
    }

    /** ⚠️ Absent rather than empty — see {@link #childProperties}. */
    private static void forward(List<String> into, Map<String, String> options, String option,
                                String property) {
        var value = options.get(option);
        if (value != null && !value.isBlank()) {
            into.add(property + "=" + value);
        }
    }

    public static void main(String[] args) throws Exception {
        var options = parseArgs(args);
        var projectDir = Path.of(options.get("projectDir")).toAbsolutePath();
        var outDir = Path.of(options.get("out")).toAbsolutePath();
        var weightsFile = Path.of(options.get("weights")).toAbsolutePath();
        var selection = options.getOrDefault("selection", "all");
        int jobs = Math.max(1, Integer.parseInt(options.getOrDefault("jobs", "2")));
        long timeoutMs = Long.parseLong(options.getOrDefault("timeoutSec", "1800")) * 1000L;
        long runStartedMs = System.currentTimeMillis();
        // Headless has to be forwarded explicitly: a child build is a fresh Gradle invocation and
        // inherits none of this one's project properties, so without this the shards would each try
        // to open a window on a machine that has no display.
        var headless = Boolean.parseBoolean(options.getOrDefault("headless", "false"));

        log("parallel run: selection '" + selection + "' across " + jobs + " job(s)"
                + (headless ? ", headless" : ""));
        log("output: " + outDir);
        if (Files.isRegularFile(weightsFile)) {
            log("balancing from " + weightsFile);
        } else {
            log("no recorded durations yet - splitting evenly; the next run will be balanced");
        }

        // The weights file lives outside the output directory precisely so this does not delete it.
        ChildBuilds.deleteRecursively(outDir, ParallelTestOrchestrator::log);
        Files.createDirectories(outDir);

        long deadline = System.currentTimeMillis() + timeoutMs;
        var processes = new LinkedHashMap<String, Process>();
        int exitCode;
        try {
            var childProperties = childProperties(options, jobs, headless);
            for (int shard = 0; shard < jobs; shard++) {
                var name = shardName(shard);
                processes.put(name, ChildBuilds.spawn(projectDir, SHARD_TASK_PREFIX + shard,
                        childProperties, outDir.resolve(name).resolve("gradle.log")));
            }
            log("spawned " + jobs + " shard(s); waiting...");

            boolean timedOut = false;
            for (var entry : processes.entrySet()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0 || !entry.getValue().waitFor(remaining, TimeUnit.MILLISECONDS)) {
                    timedOut = true;
                    break;
                }
                // 0 pass, 1 failed checks, 3 watchdog hang - all of them still leave a report behind,
                // so the code is informative and the report is authoritative.
                log("'" + entry.getKey() + "' exited with code " + entry.getValue().exitValue());
            }
            if (timedOut) {
                log("GLOBAL TIMEOUT - killing the survivors");
                ChildBuilds.killAll(processes);
            }
            exitCode = mergeAndReport(outDir, weightsFile, selection, jobs, timedOut, runStartedMs);
        } finally {
            ChildBuilds.killAll(processes);
        }
        System.exit(exitCode);
    }

    private static String shardName(int shard) {
        return "shard" + shard;
    }

    // region merging

    private static int mergeAndReport(Path outDir, Path weightsFile, String selection, int jobs,
                                      boolean timedOut, long runStartedMs) {
        var merged = new RunReport();
        merged.runId = "par";
        merged.selection = selection;
        merged.startedAt = runStartedMs;

        var parts = new LinkedHashMap<String, RunReport>();
        for (int shard = 0; shard < jobs; shard++) {
            var name = shardName(shard);
            var part = ReportMerge.read(outDir.resolve(name).resolve(ReportWriter.REPORT_FILE));
            if (part == null) {
                log("MISSING report from '" + name + "' (" + outDir.resolve(name) + ")");
                merged.scenarios.add(ReportMerge.syntheticError("<" + name + ">",
                        "the '" + name + "' process left no readable report - it crashed, hung before "
                                + "writing one, or never started; see " + outDir.resolve(name).resolve("gradle.log")));
                continue;
            }
            parts.put(name, part);
            // Every shard is the same build on the same machine, so the first one that reports an
            // environment describes them all.
            if (part.environment != null && merged.environment.minecraft.isEmpty()) {
                merged.environment = part.environment;
            }
            // Disjoint by construction, so merging is concatenation - unlike the multi-process
            // merger, where every role sees the same scenario and they have to be folded together.
            merged.scenarios.addAll(part.scenarios);
        }
        if (timedOut) {
            merged.status = RunReport.Status.worst(merged.status, RunReport.Status.HUNG);
        }
        verifyNothingWasLost(merged, parts);

        merged.scenarios.sort(Comparator.comparing(scenario -> scenario.name));
        try {
            collectScreenshots(outDir, parts.keySet());
        } catch (IOException e) {
            log("could not gather the shard screenshots: " + e);
        }
        try {
            ReportMerge.write(merged, parts.values(), outDir, report -> summarise(report, jobs));
        } catch (IOException e) {
            log("could not write the merged report: " + e);
            return 2;
        }
        writeWeights(weightsFile, merged);

        log("");
        log(summarise(merged, jobs));
        return RunReport.Status.PASS.equals(merged.status) ? 0 : 1;
    }

    /**
     * Checks that the shards agreed on what there was to run.
     *
     * <p>The whole design rests on every shard resolving the same scenario list and splitting it the
     * same way. If one of them saw a different list — a mod that failed to load in that process, a
     * stale build — the splits diverge and a scenario can end up assigned to nobody. Nothing about
     * that is visible in the merged report: it simply contains fewer scenarios, all passing.
     *
     * <p>So compare the lists each shard says it knew about, and require the union of what actually
     * ran to cover them.
     */
    private static void verifyNothingWasLost(RunReport merged, Map<String, RunReport> parts) {
        var known = new LinkedHashSet<String>();
        String reference = null;
        List<String> referenceKnown = null;
        for (var entry : parts.entrySet()) {
            var shard = entry.getValue().shard;
            if (shard == null || shard.known.isEmpty()) continue;
            if (referenceKnown == null) {
                reference = entry.getKey();
                referenceKnown = shard.known;
            } else if (!referenceKnown.equals(shard.known)) {
                var message = "'" + entry.getKey() + "' and '" + reference + "' resolved different "
                        + "scenario lists, so the split cannot be trusted and a scenario may have run "
                        + "nowhere (" + shard.known.size() + " vs " + referenceKnown.size() + " scenarios)";
                log("DISAGREEMENT: " + message);
                merged.scenarios.add(ReportMerge.syntheticError("<shards>", message));
            }
            known.addAll(shard.known);
        }

        if (known.isEmpty()) {
            // Every shard was empty. A serial run reports this itself; here only this side can see it.
            if (merged.scenarios.isEmpty()) {
                log("the selection matched no scenarios in any shard");
                merged.scenarios.add(ReportMerge.syntheticError("<selection>",
                        "selection '" + merged.selection + "' matched no scenarios"));
            }
            return;
        }
        // Counted rather than collected into a set, so running one twice is as visible as running it
        // zero times - both mean the shards split the list differently, and neither shows up in the
        // totals as anything but a slightly wrong number.
        var runCount = new LinkedHashMap<String, Integer>();
        for (var scenario : merged.scenarios) {
            runCount.merge(scenario.name, 1, Integer::sum);
        }
        var missing = known.stream().filter(name -> !runCount.containsKey(name)).sorted().toList();
        if (!missing.isEmpty()) {
            var message = "no shard ran " + missing.size() + " scenario(s) that the shards agreed exist: "
                    + missing;
            log("LOST: " + message);
            merged.scenarios.add(ReportMerge.syntheticError("<missing>", message));
        }
        var duplicated = runCount.entrySet().stream()
                .filter(entry -> entry.getValue() > 1 && !entry.getKey().startsWith("<"))
                .map(Map.Entry::getKey).sorted().toList();
        if (!duplicated.isEmpty()) {
            var message = "more than one shard ran " + duplicated.size() + " scenario(s): " + duplicated;
            log("DUPLICATED: " + message);
            merged.scenarios.add(ReportMerge.syntheticError("<duplicated>", message));
        }
    }

    /**
     * Moves each shard's screenshots under the merged output directory.
     *
     * <p>{@code CaptureRef#path} is relative to its own run's output directory and always starts
     * {@code screenshots/<scenario>/}, and scenarios are disjoint across shards — so lifting the
     * trees into one keeps every recorded path valid without rewriting any of them.
     */
    private static void collectScreenshots(Path outDir, Iterable<String> shardNames) throws IOException {
        var target = outDir.resolve("screenshots");
        for (var name : shardNames) {
            var source = outDir.resolve(name).resolve("screenshots");
            if (!Files.isDirectory(source)) continue;
            Files.createDirectories(target);
            try (Stream<Path> entries = Files.list(source)) {
                for (var scenarioDir : entries.toList()) {
                    var destination = target.resolve(scenarioDir.getFileName().toString());
                    // Scenarios are disjoint, so a collision means two shards ran the same one -
                    // which verifyNothingWasLost reports. Leave the first in place rather than
                    // overwriting, so both trees survive under the shard directories for inspection.
                    if (Files.exists(destination)) continue;
                    Files.move(scenarioDir, destination);
                }
            }
        }
    }

    /**
     * Records how long each scenario took, for the next run to balance with.
     *
     * <p>Synthetic entries ({@code <selection>}, {@code <shard0>}) are skipped: they are this
     * orchestrator's own bookkeeping, not scenarios, and would be balanced around as if they were.
     */
    private static void writeWeights(Path weightsFile, RunReport merged) {
        var durations = new TreeMap<String, Long>();
        for (var scenario : merged.scenarios) {
            if (!scenario.name.startsWith("<")) {
                durations.put(scenario.name, scenario.durationMs);
            }
        }
        int recorded = ShardWeights.merge(weightsFile, durations);
        if (recorded < 0) {
            log("could not record the durations to " + weightsFile);
        } else if (recorded > 0) {
            log("recorded " + recorded + " scenario duration(s) for the next run");
        }
    }

    private static String summarise(RunReport merged, int jobs) {
        var out = new StringBuilder();
        out.append("LDLib2 parallel test: ").append(merged.status)
                .append(" (").append(merged.totals.passed).append('/').append(merged.totals.scenarios)
                .append(" scenarios across ").append(jobs).append(" job(s))\n");
        merged.scenarios.stream()
                .sorted(Comparator.comparing(scenario -> scenario.name))
                .forEach(scenario -> {
                    out.append("  ").append(scenario.status).append("  ").append(scenario.name).append('\n');
                    if (RunReport.Status.PASS.equals(scenario.status)) return;
                    if (scenario.error != null) {
                        out.append("      ").append(scenario.error.type).append(": ")
                                .append(scenario.error.message).append('\n');
                    }
                    scenario.steps.stream()
                            .filter(step -> !RunReport.Status.PASS.equals(step.status))
                            .forEach(step -> {
                                out.append("      step ").append(step.index)
                                        .append(" '").append(step.name).append("' ").append(step.status);
                                if (step.error != null) out.append(" - ").append(step.error.message);
                                out.append('\n');
                            });
                });
        return out.toString();
    }

    // endregion

    private static Map<String, String> parseArgs(String[] args) {
        var options = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("expected --key value pairs, got " + args[i]);
            }
            options.put(args[i].substring(2), args[i + 1]);
        }
        for (var required : List.of("projectDir", "out", "weights")) {
            if (!options.containsKey(required)) {
                throw new IllegalArgumentException("--" + required + " is required");
            }
        }
        return options;
    }

    private static void log(String message) {
        System.out.println("[partest] " + message);
    }
}
