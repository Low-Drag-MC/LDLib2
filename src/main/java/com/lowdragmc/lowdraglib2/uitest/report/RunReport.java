package com.lowdragmc.lowdraglib2.uitest.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The machine-readable result of one run, serialised to {@code report.json}.
 *
 * <p>Deliberately plain mutable data with public fields: it is filled in incrementally as the run
 * proceeds and handed straight to Gson at the end. {@code schema} is versioned so a consumer — a CI
 * step, an agent, the {@code verifyUiTest} Gradle task — can fail loudly on a format it does not
 * understand instead of silently reading nothing.
 */
public class RunReport {

    /** Bump when a field changes meaning or disappears. Additive fields do not need a bump. */
    public static final int SCHEMA = 1;

    public int schema = SCHEMA;
    public String runId = "";
    public String status = Status.PASS;
    public long startedAt;
    public long finishedAt;
    public long durationMs;
    public String selection = "";
    public Environment environment = new Environment();
    public Totals totals = new Totals();
    public List<ScenarioReport> scenarios = new ArrayList<>();
    /** Set only by a process running one slice of a parallel run; {@code null} for a serial one. */
    public ShardInfo shard;

    /** Statuses shared by runs, scenarios and steps so a consumer only learns one vocabulary. */
    public static final class Status {
        public static final String PASS = "PASS";
        /** An assertion did not hold. The run itself worked. */
        public static final String FAIL = "FAIL";
        /** A step threw, or a wait timed out. */
        public static final String ERROR = "ERROR";
        public static final String SKIPPED = "SKIPPED";
        /** No frame for the watchdog interval — the game hung or crashed mid-run. */
        public static final String HUNG = "HUNG";

        private Status() {
        }

        /** FAIL beats PASS, ERROR beats FAIL, HUNG beats everything. */
        public static String worst(String a, String b) {
            return rank(b) > rank(a) ? b : a;
        }

        private static int rank(String status) {
            return switch (status) {
                case SKIPPED -> -1;
                case PASS -> 0;
                case FAIL -> 1;
                case ERROR -> 2;
                case HUNG -> 3;
                default -> 0;
            };
        }
    }

    public static class Environment {
        public String minecraft = "";
        public String neoforge = "";
        public String java = "";
        public String os = "";
        /**
         * Every loaded mod. A dev runtime pulls in the whole {@code localImplementation} set — JEI,
         * AE2, Sodium, Iris, KubeJS — and any of them can change layout or the render pipeline. When
         * a capture regresses, this is the first thing worth checking.
         */
        public List<String> mods = new ArrayList<>();
        public int guiScale;
        public int windowWidth;
        public int windowHeight;
        public int framebufferWidth;
        public int framebufferHeight;
        public String inputMode = "";
        /**
         * Whether the window was ever shown. Sits next to the frame size for the same reason: when
         * two machines disagree about a capture, the mode the frame was produced in is the first
         * thing worth ruling out.
         */
        public boolean headless;
    }

    /**
     * One process's view of a parallel run.
     *
     * <p>{@link #known} is the whole selection <em>this</em> process resolved, not the slice it ran,
     * and it is the only thing that makes a lost scenario detectable. Shards agree on who runs what
     * by each computing the same split rather than by being told, so the failure to guard against is
     * two processes computing different splits — which drops a scenario from the run entirely and
     * still reports green. The merger compares these lists and refuses a run where they disagree.
     */
    public static class ShardInfo {
        public int index;
        public int count;
        public List<String> known = new ArrayList<>();
    }

    public static class Totals {
        public int scenarios;
        public int passed;
        public int failed;
        public int errored;
        public int skipped;
        public int steps;
        public int checks;
        public int checksFailed;
        public int captures;
    }

    public static class ScenarioReport {
        public String name = "";
        public String group = "";
        public String className = "";
        public List<String> tags = new ArrayList<>();
        public String status = Status.PASS;
        public long durationMs;
        public List<StepReport> steps = new ArrayList<>();
        public ErrorInfo error;
    }

    public static class StepReport {
        public int index;
        public String name = "";
        public String kind = "";
        /** Which process produced this step in a merged multi-process report; {@code null} in solo runs. */
        public String role;
        public String group;
        public String status = Status.PASS;
        public long durationMs;
        public int attempts;
        public long settleMs;
        /** What the step was still waiting for when it timed out. */
        public String waitingFor;
        public TargetInfo target;
        /** Concurrent because server-thread steps record checks from off the render thread. */
        public List<CheckResult> checks = new CopyOnWriteArrayList<>();
        public List<CaptureRef> captures = new ArrayList<>();
        public ErrorInfo error;
        public List<String> log = new CopyOnWriteArrayList<>();
        public Map<String, String> attachments = new LinkedHashMap<>();
    }

    public static class TargetInfo {
        public String selector = "";
        public String path = "";
        public float x;
        public float y;
        public float width;
        public float height;
        /**
         * Whether hit-testing the target's centre resolved back to the same element. False means the
         * element is occluded or clipped, so clicking it would have been a silent no-op.
         */
        public boolean hitTestOk;
        public String hitTestActual;
    }

    public static class CheckResult {
        public String desc = "";
        public boolean passed;
        public String expected;
        public String actual;
        public String target;
    }

    public static class CaptureRef {
        /** {@code FULL}, {@code ELEMENT} or {@code ERROR}. */
        public String kind = "FULL";
        /** Path relative to the run's output directory, using forward slashes. */
        public String path = "";
        public String elementPath;
        /**
         * The image was a single flat colour. Almost always means the wrong framebuffer was bound
         * rather than that the UI really is one colour, so it is worth surfacing rather than
         * letting someone stare at a black PNG wondering what broke.
         */
        public boolean suspect;
    }

    public static class ErrorInfo {
        public String type = "";
        public String message = "";
        public String stackTrace = "";

        public static ErrorInfo of(Throwable throwable) {
            var info = new ErrorInfo();
            info.type = throwable.getClass().getName();
            info.message = String.valueOf(throwable.getMessage());
            var writer = new java.io.StringWriter();
            throwable.printStackTrace(new java.io.PrintWriter(writer));
            info.stackTrace = writer.toString();
            return info;
        }
    }
}
