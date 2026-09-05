package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The run's configuration, read from system properties set by the Gradle {@code runClient} task.
 *
 * <p>System properties rather than program arguments, because Minecraft's own argument parser owns
 * the command line and rejects what it does not know — the same reason NeoForge passes
 * {@code neoforge.enabledGameTestNamespaces} this way.
 */
public final class RunConfig {

    public static final String PROP_RUN = "ldlib2.uitest.run";
    public static final String PROP_EXCLUDE = "ldlib2.uitest.exclude";
    public static final String PROP_OUT = "ldlib2.uitest.out";
    public static final String PROP_GUI_SCALE = "ldlib2.uitest.guiScale";
    public static final String PROP_WINDOW = "ldlib2.uitest.window";
    public static final String PROP_INPUT_MODE = "ldlib2.uitest.inputMode";
    public static final String PROP_WATCHDOG_SEC = "ldlib2.uitest.watchdogSec";
    public static final String PROP_KEEP_OPEN = "ldlib2.uitest.keepOpen";
    /** Run without a visible window, on a machine that may have no monitor at all. See {@link #headless()}. */
    public static final String PROP_HEADLESS = "ldlib2.uitest.headless";
    /** {@code <index>/<count>} — which slice of the selection this process runs. See {@link ShardPlan}. */
    public static final String PROP_SHARD = "ldlib2.uitest.shard";
    /** Path to the previous run's scenario durations, used to balance the slices. Optional. */
    public static final String PROP_WEIGHTS = "ldlib2.uitest.weights";

    /**
     * The frame a headless run gets when {@link #PROP_WINDOW} does not say otherwise. Maximising is
     * not an option there — it reads the primary monitor's work area, and there may be no monitor —
     * so headless always runs at a pinned size, and this is it.
     */
    private static final int HEADLESS_WIDTH = 1920;
    private static final int HEADLESS_HEIGHT = 1080;

    private final String selection;
    private final String exclusion;
    private final Path outDir;
    private final int guiScale;
    private final int windowWidth;
    private final int windowHeight;
    private final InputMode inputMode;
    private final int watchdogSeconds;
    private final boolean keepOpen;
    private final boolean headless;
    private final int shardIndex;
    private final int shardCount;
    @Nullable
    private final Path weightsFile;

    private RunConfig(String selection, String exclusion, Path outDir, int guiScale,
                      int windowWidth, int windowHeight, InputMode inputMode,
                      int watchdogSeconds, boolean keepOpen, boolean headless,
                      int shardIndex, int shardCount, @Nullable Path weightsFile) {
        this.selection = selection;
        this.exclusion = exclusion;
        this.outDir = outDir;
        this.guiScale = guiScale;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.inputMode = inputMode;
        this.watchdogSeconds = watchdogSeconds;
        this.keepOpen = keepOpen;
        this.headless = headless;
        this.shardIndex = shardIndex;
        this.shardCount = shardCount;
        this.weightsFile = weightsFile;
    }

    /** @return {@code null} when no selection was requested, i.e. this is an ordinary client launch */
    public static RunConfig fromSystemProperties() {
        var selection = System.getProperty(PROP_RUN, "").trim();
        if (selection.isEmpty()) return null;

        var out = System.getProperty(PROP_OUT, "").trim();
        var outDir = out.isEmpty() ? Path.of("ldlib2-uitest").toAbsolutePath() : Path.of(out).toAbsolutePath();

        // Maximised by default: a default-sized window makes a multi-pane UI unreadable in the
        // screenshots, which are the main thing a run produces. An explicit WxH pins the size
        // instead, which is what you want once captures are being compared between machines.
        int windowWidth = 0;
        int windowHeight = 0;
        var window = System.getProperty(PROP_WINDOW, "").trim().toLowerCase(Locale.ROOT);
        int x = window.indexOf('x');
        if (x > 0) {
            try {
                windowWidth = Integer.parseInt(window.substring(0, x));
                windowHeight = Integer.parseInt(window.substring(x + 1));
            } catch (NumberFormatException ignored) {
                // fall back to maximising; a malformed size is not worth aborting a run over
            }
        }

        var inputMode = "REAL".equalsIgnoreCase(System.getProperty(PROP_INPUT_MODE, "SYNTHETIC"))
                ? InputMode.REAL : InputMode.SYNTHETIC;

        var headless = Boolean.getBoolean(PROP_HEADLESS);
        if (headless) {
            // REAL drives the OS cursor and needs the window focused, neither of which exists here.
            // Silently downgrading would be worse than refusing: the run would pass while testing
            // nothing, because every synthetic gesture would land on a window nobody can focus.
            //
            // The Gradle wiring rejects this combination before launching anything, so reaching here
            // means the system properties were set by hand; this is the backstop for that.
            if (inputMode == InputMode.REAL) {
                throw new IllegalArgumentException(PROP_INPUT_MODE + "=REAL cannot be combined with "
                        + PROP_HEADLESS + "=true - real input needs a focusable window.");
            }
            // A hidden window is not clamped to the desktop, so an explicit -PldTestWindow larger
            // than any attached display is honoured - that is how a 4K capture comes off a machine
            // with a smaller monitor, or none.
            if (windowWidth <= 0 || windowHeight <= 0) {
                windowWidth = HEADLESS_WIDTH;
                windowHeight = HEADLESS_HEIGHT;
            }
        }

        // "<index>/<count>", absent for an ordinary serial run, which is shard 0 of 1.
        int shardIndex = 0;
        int shardCount = 1;
        var shard = System.getProperty(PROP_SHARD, "").trim();
        int slash = shard.indexOf('/');
        if (slash > 0) {
            try {
                shardIndex = Integer.parseInt(shard.substring(0, slash).trim());
                shardCount = Math.max(1, Integer.parseInt(shard.substring(slash + 1).trim()));
                if (shardIndex < 0 || shardIndex >= shardCount) {
                    // Running everything would be far worse than running nothing here: N shards would
                    // each run the whole suite, and the merged report would double-count silently.
                    throw new IllegalArgumentException("shard index " + shardIndex
                            + " is outside [0, " + shardCount + ")");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("malformed " + PROP_SHARD + " '" + shard
                        + "', expected <index>/<count>", e);
            }
        }
        var weights = System.getProperty(PROP_WEIGHTS, "").trim();

        return new RunConfig(
                selection,
                System.getProperty(PROP_EXCLUDE, "").trim(),
                outDir,
                // The vanilla default, and deliberately not auto: auto picks the largest scale the
                // window supports, which on a maximised 4K window is 8 - a 480x257 logical viewport
                // that collapses the layout of any full-screen UI. Erring small only makes things
                // small, which element crops already solve; erring large breaks what is being tested.
                // Scenarios showing a small panel should raise it via ScenarioOptions#guiScale.
                intProperty(PROP_GUI_SCALE, 2),
                windowWidth,
                windowHeight,
                inputMode,
                intProperty(PROP_WATCHDOG_SEC, 90),
                Boolean.getBoolean(PROP_KEEP_OPEN),
                headless,
                shardIndex,
                shardCount,
                weights.isEmpty() ? null : Path.of(weights).toAbsolutePath());
    }

    /**
     * Config for a scenario run started from inside an already-running game.
     *
     * <p>The point of this mode is iteration speed: a cold run pays for Gradle, mod loading and
     * world creation before a single step executes, and none of that changes between attempts. Here
     * the window is left alone, the loaded world is reused, and the game stays up afterwards.
     */
    public static RunConfig interactive(String selection) {
        return new RunConfig(selection, "", Path.of("ldlib2-uitest").toAbsolutePath(),
                2, 1280, 720, InputMode.SYNTHETIC, 90, true, false, 0, 1, null);
    }

    /**
     * Config for one client of a multi-process run. The selection is a placeholder: it arrives over
     * the control channel, and the MP scenario collection reads it from there, not from here. The
     * watchdog is more generous than solo because three game processes share the machine.
     */
    static RunConfig forMultiProcess(java.nio.file.Path outDir) {
        // Headless is a property of the machine, not of the run, so every client of a multi-process
        // run inherits it from the same system property the orchestrator passed down.
        var headless = Boolean.getBoolean(PROP_HEADLESS);
        return new RunConfig("<multi-process>", "", outDir,
                2, headless ? HEADLESS_WIDTH : 0, headless ? HEADLESS_HEIGHT : 0,
                InputMode.SYNTHETIC, 180, false, headless, 0, 1, null);
    }

    private static int intProperty(String key, int fallback) {
        try {
            return Integer.parseInt(System.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Whether a scenario is in the selection.
     *
     * <p>Grammar: {@code all} | {@code <name>} | {@code a,b,c} | {@code group:<g>} | {@code tag:<t>}
     * | {@code mod:<modid>} | {@code regex:<pattern>}. Terms are OR-ed.
     */
    public boolean matches(LDLRegisterClient annotation, Class<?> scenarioClass, ScenarioOptions options) {
        if (!selectionMatches(selection, annotation.name(), annotation.group(), options.tags(), scenarioClass)) {
            return false;
        }
        return exclusion.isEmpty()
                || !selectionMatches(exclusion, annotation.name(), annotation.group(), options.tags(), scenarioClass);
    }

    /**
     * The selection grammar, factored out of the client-side config so the multi-process runners —
     * the dedicated server included — evaluate exactly the same expression the same way.
     */
    public static boolean selectionMatches(String expression, String name, String group,
                                           java.util.Set<String> tags, Class<?> scenarioClass) {
        for (var term : expression.split(",")) {
            term = term.trim();
            if (term.isEmpty()) continue;
            if (term.equalsIgnoreCase("all") || term.equals("*")) return true;
            if (term.startsWith("group:")) {
                if (group.equalsIgnoreCase(term.substring(6))) return true;
            } else if (term.startsWith("tag:")) {
                var tag = term.substring(4);
                if (tags.stream().anyMatch(tag::equalsIgnoreCase)) return true;
            } else if (term.startsWith("mod:")) {
                // By package, not by the annotation's registry: every scenario declares the same
                // registry, so matching on that would select everything.
                if (scenarioClass.getName().contains("." + term.substring(4) + ".")) return true;
            } else if (term.startsWith("regex:")) {
                if (Pattern.compile(term.substring(6)).matcher(name).matches()) return true;
            } else if (term.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public String selection() {
        return selection;
    }

    public Path outDir() {
        return outDir;
    }

    public int guiScale() {
        return guiScale;
    }

    public int windowWidth() {
        return windowWidth;
    }

    public int windowHeight() {
        return windowHeight;
    }

    /** {@code true} unless an explicit {@code WxH} was requested. */
    public boolean maximizeWindow() {
        return windowWidth <= 0 || windowHeight <= 0;
    }

    public InputMode inputMode() {
        return inputMode;
    }

    public int watchdogSeconds() {
        return watchdogSeconds;
    }

    /** Leaves the game running after the report is written. For watching a run interactively. */
    public boolean keepOpen() {
        return keepOpen;
    }

    /**
     * Whether to run without ever showing the window.
     *
     * <p>For a machine with no monitor, a locked console, or a run triggered over SSH. Nothing about
     * the harness itself needs a visible window — captures are downloaded from the main render
     * target rather than the swap chain, and {@link InputMode#SYNTHETIC} dispatches straight into
     * {@code Screen} — so this only has to stop the runner from asking the window system for things
     * a headless machine cannot answer.
     */
    public boolean headless() {
        return headless;
    }

    /** Which slice of the selection this process runs; {@code 0} for a serial run. */
    public int shardIndex() {
        return shardIndex;
    }

    /** How many processes the selection is split across; {@code 1} for a serial run. */
    public int shardCount() {
        return shardCount;
    }

    /** {@code true} when this process is one of several sharing a selection. */
    public boolean isSharded() {
        return shardCount > 1;
    }

    /** Previous durations used to balance the slices, or {@code null} when there are none yet. */
    @Nullable
    public Path weightsFile() {
        return weightsFile;
    }
}
