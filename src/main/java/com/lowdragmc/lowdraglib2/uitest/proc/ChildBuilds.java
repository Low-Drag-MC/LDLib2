package com.lowdragmc.lowdraglib2.uitest.proc;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Launching and reaping child Gradle builds, shared by the test orchestrators that run several game
 * processes at once.
 *
 * <p>Both orchestrators run <b>outside</b> the game — plain JDK, no Minecraft on the class path — so
 * this stays free of game references too.
 *
 * <p>Extracted rather than copied because the interesting part is not the {@code ProcessBuilder}: it
 * is {@link #spawn}'s removal of {@code GRADLE_USER_HOME}, which is a trap that cost a whole session
 * once and would otherwise have to be remembered separately in each orchestrator.
 */
public final class ChildBuilds {

    private ChildBuilds() {
    }

    /**
     * Starts {@code gradlew <task> <extraArgs...>} in the project directory, with everything it
     * prints redirected to {@code logFile}.
     *
     * <p>{@code GRADLE_USER_HOME} is deliberately dropped from the child's environment. A
     * project-local Gradle home is fine for compiling but breaks a game launch: the run ends up with
     * bootstraplauncher on both the module path and the class path, and the JVM dies on boot with
     * "Module named cpw.mods.bootstraplauncher was already on the JVMs module path" — long before
     * anything could write a report explaining itself.
     */
    public static Process spawn(Path projectDir, String task, List<String> extraArgs, Path logFile)
            throws IOException {
        Files.createDirectories(logFile.getParent());
        var command = new ArrayList<String>();
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(projectDir.resolve("gradlew.bat").toString());
        } else {
            command.add(projectDir.resolve("gradlew").toString());
        }
        command.add(task);
        command.addAll(extraArgs);
        command.add("--console=plain");
        var builder = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());
        builder.environment().remove("GRADLE_USER_HOME");
        return builder.start();
    }

    /** Kills the processes and everything they started. The second line of defence, never the first. */
    public static void killAll(Map<String, Process> processes) {
        for (var process : processes.values()) {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }

    public static boolean anyDied(Map<String, Process> processes) {
        return processes.values().stream().anyMatch(process -> !process.isAlive());
    }

    public interface Check {
        boolean stop();
    }

    /** Polls until the condition holds, the abort check fires, or the timeout elapses. */
    public static boolean waitUntil(Check condition, long timeoutMs, Check abort) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (condition.stop()) return true;
            if (abort.stop()) return condition.stop();
            Thread.sleep(500);
        }
        return condition.stop();
    }

    public static int pickFreePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** Best-effort recursive delete; reports what it could not remove rather than throwing. */
    public static void deleteRecursively(Path root, java.util.function.Consumer<String> log) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.accept("could not delete " + path + ": " + e);
                }
            });
        }
    }
}
