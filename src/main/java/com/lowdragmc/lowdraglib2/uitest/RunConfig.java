package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;

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

    private final String selection;
    private final String exclusion;
    private final Path outDir;
    private final int guiScale;
    private final int windowWidth;
    private final int windowHeight;
    private final InputMode inputMode;
    private final int watchdogSeconds;
    private final boolean keepOpen;

    private RunConfig(String selection, String exclusion, Path outDir, int guiScale,
                      int windowWidth, int windowHeight, InputMode inputMode,
                      int watchdogSeconds, boolean keepOpen) {
        this.selection = selection;
        this.exclusion = exclusion;
        this.outDir = outDir;
        this.guiScale = guiScale;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.inputMode = inputMode;
        this.watchdogSeconds = watchdogSeconds;
        this.keepOpen = keepOpen;
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
                Boolean.getBoolean(PROP_KEEP_OPEN));
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
                2, 1280, 720, InputMode.SYNTHETIC, 90, true);
    }

    /**
     * Config for one client of a multi-process run. The selection is a placeholder: it arrives over
     * the control channel, and the MP scenario collection reads it from there, not from here. The
     * watchdog is more generous than solo because three game processes share the machine.
     */
    static RunConfig forMultiProcess(java.nio.file.Path outDir) {
        return new RunConfig("<multi-process>", "", outDir,
                2, 0, 0, InputMode.SYNTHETIC, 180, false);
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
}
