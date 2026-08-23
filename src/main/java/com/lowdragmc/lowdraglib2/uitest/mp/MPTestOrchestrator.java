package com.lowdragmc.lowdraglib2.uitest.mp;

import com.google.gson.GsonBuilder;
import com.lowdragmc.lowdraglib2.uitest.report.ReportWriter;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The entry point of {@code gradlew runMpTest}: spawns one dedicated server and N clients as child
 * Gradle builds, hosts the TCP control hub they coordinate through, and merges their reports into
 * one {@code report.json} when they exit.
 *
 * <p>Runs <b>outside</b> the game — plain JDK plus Gson, never touching a Minecraft class — which
 * is why the protocol classes it shares with the game processes are kept free of game references.
 *
 * <p>Lifecycle safety: children treat a dropped hub connection as "shut down now", so this process
 * dying (or timing out and closing the hub) cannot leave game processes running. Force-killing the
 * child process trees is only the second line of defence.
 */
public final class MPTestOrchestrator {

    private static final String SERVER_TASK = "runMpServer";
    private static final String CLIENT_TASK_PREFIX = "runMpClient";

    public static void main(String[] args) throws Exception {
        var options = parseArgs(args);
        var projectDir = Path.of(options.get("projectDir")).toAbsolutePath();
        var outDir = Path.of(options.get("out")).toAbsolutePath();
        var selection = options.getOrDefault("selection", "all");
        var clients = Stream.of(options.getOrDefault("clients", "A,B").split(","))
                .map(String::trim).filter(role -> !role.isEmpty()).toList();
        if (clients.isEmpty()) throw new IllegalArgumentException("--clients resolved to no roles");
        long runStartedMs = System.currentTimeMillis();
        var serverDir = Path.of(options.getOrDefault("serverDir",
                projectDir.resolve("runs").resolve("mpServer").toString()));
        long timeoutMs = Long.parseLong(options.getOrDefault("timeoutSec", "1500")) * 1000L;
        long serverStartTimeoutMs = Long.parseLong(options.getOrDefault("serverStartTimeoutSec", "420")) * 1000L;

        log("multi-process run: selection '" + selection + "', clients " + clients);
        log("output: " + outDir);

        deleteRecursively(outDir);
        Files.createDirectories(outDir);

        int gamePort = pickFreePort();
        prepareServerDir(serverDir, gamePort);
        log("prepared " + serverDir + " (game port " + gamePort + ")");

        long deadline = System.currentTimeMillis() + timeoutMs;
        var processes = new LinkedHashMap<String, Process>();
        int exitCode;
        try (var hub = new Hub(selection, clients, gamePort)) {
            log("control hub listening on 127.0.0.1:" + hub.port());

            processes.put(MPMessages.SERVER_ROLE,
                    spawnChild(projectDir, SERVER_TASK, hub.port(), outDir.resolve("server")));
            log("spawned the dedicated server (" + SERVER_TASK + "); waiting for it to come up...");

            if (!waitUntil(() -> hub.serverReady, Math.min(serverStartTimeoutMs, deadline - System.currentTimeMillis()),
                    () -> processDied(processes))) {
                fail(processes, hub, outDir, "the dedicated server never reported ready - see "
                        + outDir.resolve("server").resolve("gradle.log"));
                System.exit(2);
            }
            log("server ready; spawning " + clients.size() + " client(s)...");
            for (var role : clients) {
                processes.put(role, spawnChild(projectDir, CLIENT_TASK_PREFIX + role, hub.port(),
                        outDir.resolve("client" + role)));
            }

            // From here the children run the show; this side just enforces the global deadline.
            boolean timedOut = false;
            for (var entry : processes.entrySet()) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0 || !entry.getValue().waitFor(remaining, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                    timedOut = true;
                    break;
                }
                log("'" + entry.getKey() + "' exited with code " + entry.getValue().exitValue());
            }
            if (timedOut) {
                log("GLOBAL TIMEOUT - closing the hub so survivors shut themselves down");
                hub.close();
                waitUntil(() -> processes.values().stream().noneMatch(Process::isAlive), 30_000, () -> false);
                killAll(processes);
            }
            exitCode = mergeAndReport(outDir, selection, clients, timedOut, runStartedMs);
        } finally {
            killAll(processes);
        }
        System.exit(exitCode);
    }

    // region child processes

    private static Process spawnChild(Path projectDir, String task, int hubPort, Path logDir) throws IOException {
        Files.createDirectories(logDir);
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        var command = new ArrayList<String>();
        if (windows) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(projectDir.resolve("gradlew.bat").toString());
        } else {
            command.add(projectDir.resolve("gradlew").toString());
        }
        command.add(task);
        command.add("-PldMpHub=127.0.0.1:" + hubPort);
        command.add("--console=plain");
        var builder = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logDir.resolve("gradle.log").toFile());
        // The project-local Gradle user home (fine for compiles) breaks game launches: it puts
        // bootstraplauncher on both the module path and the class path, and the JVM dies on boot.
        builder.environment().remove("GRADLE_USER_HOME");
        return builder.start();
    }

    private static void killAll(Map<String, Process> processes) {
        for (var process : processes.values()) {
            if (process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        }
    }

    private static boolean processDied(Map<String, Process> processes) {
        return processes.values().stream().anyMatch(process -> !process.isAlive());
    }

    private static void fail(Map<String, Process> processes, Hub hub, Path outDir, String message) {
        log("FAILED: " + message);
        hub.close();
        killAll(processes);
        try {
            Files.writeString(outDir.resolve("report.txt"), "MP RUN FAILED\n" + message + "\n");
        } catch (IOException ignored) {
        }
    }

    // endregion

    // region server dir preparation

    private static void prepareServerDir(Path serverDir, int gamePort) throws IOException {
        Files.createDirectories(serverDir);
        Files.writeString(serverDir.resolve("eula.txt"), "eula=true\n");
        Files.writeString(serverDir.resolve("server.properties"), """
                # Generated by LDLib2 runMpTest - edits are overwritten on every run.
                online-mode=false
                server-port=%d
                level-name=world
                level-type=minecraft\\:flat
                generate-structures=false
                gamemode=creative
                force-gamemode=true
                difficulty=peaceful
                spawn-protection=0
                view-distance=6
                sync-chunk-writes=false
                max-tick-time=0
                allow-flight=true
                enable-status=false
                snooper-enabled=false
                spawn-monsters=false
                motd=LDLib2 multi-process test
                max-players=16
                """.formatted(gamePort));
        // A fresh world per run is the multi-process analogue of solo's deleteStaleWorlds: leftover
        // state from an aborted run must never masquerade as a scenario bug.
        deleteRecursively(serverDir.resolve("world"));
    }

    // endregion

    // region hub

    /**
     * The relay. Deliberately dumb: it registers children by their hello, answers with the run
     * config, rebroadcasts everything else verbatim, and derives exactly one thing itself — the
     * {@code begin} moment, when the server is up and every client is in the world.
     */
    private static final class Hub implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final String selection;
        private final List<String> clients;
        private final int gamePort;
        private final Map<String, Conn> connections = new ConcurrentHashMap<>();
        private final Set<String> joined = ConcurrentHashMap.newKeySet();
        private volatile boolean serverReady;
        private volatile boolean beginSent;
        private volatile boolean closed;

        private record Conn(Socket socket, PrintWriter out) {
        }

        Hub(String selection, List<String> clients, int gamePort) throws IOException {
            this.selection = selection;
            this.clients = clients;
            this.gamePort = gamePort;
            this.serverSocket = new ServerSocket(0);
            var acceptor = new Thread(this::acceptLoop, "mptest-hub-accept");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (!closed) {
                try {
                    var socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);
                    var reader = new Thread(() -> readLoop(socket), "mptest-hub-conn");
                    reader.setDaemon(true);
                    reader.start();
                } catch (IOException e) {
                    return; // closed
                }
            }
        }

        private void readLoop(Socket socket) {
            String role = null;
            try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.isBlank()) continue;
                    MPMessages.Msg msg;
                    try {
                        msg = MPMessages.decode(line);
                    } catch (Exception e) {
                        log("hub: undecodable message " + line);
                        continue;
                    }
                    if (MPMessages.HELLO.equals(msg.type)) {
                        role = msg.role;
                        var out = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
                        connections.put(role, new Conn(socket, out));
                        log("hub: '" + role + "' connected (pid " + msg.pid + ")");
                        sendConfig(out);
                        continue;
                    }
                    onMessage(msg);
                    relay(role, line);
                }
            } catch (IOException ignored) {
                // fall through to the disconnect handling
            }
            if (role != null && connections.remove(role) != null && !closed) {
                log("hub: '" + role + "' disconnected");
                var fatal = MPMessages.Msg.of(MPMessages.FATAL);
                fatal.role = role;
                broadcast(MPMessages.encode(fatal));
            }
        }

        private void onMessage(MPMessages.Msg msg) {
            switch (msg.type) {
                case MPMessages.SERVER_READY -> {
                    serverReady = true;
                    log("hub: server ready on game port " + msg.gamePort);
                }
                case MPMessages.JOINED -> {
                    joined.add(msg.role);
                    log("hub: '" + msg.role + "' joined the world (" + joined.size() + "/" + clients.size() + ")");
                    maybeBegin();
                }
                case MPMessages.SCENARIO_DONE ->
                        log("hub: scenario '" + msg.scenario + "' " + msg.status + " on '" + msg.role + "'");
                case MPMessages.RUN_DONE -> log("hub: '" + msg.role + "' finished its run: " + msg.status);
                default -> {
                }
            }
        }

        private synchronized void maybeBegin() {
            if (beginSent || !serverReady || !joined.containsAll(clients)) return;
            beginSent = true;
            log("hub: everyone is in - begin");
            broadcast(MPMessages.encode(MPMessages.Msg.of(MPMessages.BEGIN)));
        }

        private void sendConfig(PrintWriter out) {
            var config = MPMessages.Msg.of(MPMessages.CONFIG);
            config.selection = selection;
            config.clientRoles = clients;
            config.gamePort = gamePort;
            synchronized (out) {
                out.println(MPMessages.encode(config));
                out.flush();
            }
        }

        private void relay(String fromRole, String line) {
            for (var entry : connections.entrySet()) {
                if (entry.getKey().equals(fromRole)) continue;
                var out = entry.getValue().out();
                synchronized (out) {
                    out.println(line);
                    out.flush();
                }
            }
        }

        private void broadcast(String line) {
            for (var conn : connections.values()) {
                var out = conn.out();
                synchronized (out) {
                    out.println(line);
                    out.flush();
                }
            }
        }

        @Override
        public void close() {
            closed = true;
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            for (var conn : connections.values()) {
                try {
                    conn.socket().close();
                } catch (IOException ignored) {
                }
            }
            connections.clear();
        }
    }

    // endregion

    // region report merging

    private static int mergeAndReport(Path outDir, String selection, List<String> clients,
                                      boolean timedOut, long runStartedMs) {
        var roles = new ArrayList<String>();
        roles.add(MPMessages.SERVER_ROLE);
        roles.addAll(clients);

        var merged = new RunReport();
        merged.runId = "mp";
        merged.selection = selection;
        merged.startedAt = runStartedMs;

        var parts = new LinkedHashMap<String, RunReport>();
        for (var role : roles) {
            var dir = MPMessages.SERVER_ROLE.equals(role) ? "server" : "client" + role;
            var reportFile = outDir.resolve(dir).resolve(ReportWriter.REPORT_FILE);
            if (!Files.isRegularFile(reportFile)) {
                log("MISSING report from '" + role + "' (" + reportFile + ")");
                merged.status = RunReport.Status.worst(merged.status, RunReport.Status.ERROR);
                var missing = new RunReport.ScenarioReport();
                missing.name = "<" + role + ">";
                missing.status = RunReport.Status.ERROR;
                missing.error = RunReport.ErrorInfo.of(new IllegalStateException(
                        "the '" + role + "' process left no report - it crashed or never got that far"));
                merged.scenarios.add(missing);
                continue;
            }
            try (var reader = Files.newBufferedReader(reportFile, StandardCharsets.UTF_8)) {
                parts.put(role, MPMessages.GSON.fromJson(reader, RunReport.class));
            } catch (IOException | RuntimeException e) {
                log("UNREADABLE report from '" + role + "': " + e);
                merged.status = RunReport.Status.worst(merged.status, RunReport.Status.ERROR);
            }
        }
        if (timedOut) {
            merged.status = RunReport.Status.worst(merged.status, RunReport.Status.HUNG);
        }

        // Scenario order: the server's report is authoritative (every process sorts identically);
        // anything only a client knows about is appended defensively.
        var names = new LinkedHashSet<String>();
        for (var part : parts.values()) {
            part.scenarios.stream().map(scenario -> scenario.name).forEach(names::add);
        }
        for (var name : names) {
            RunReport.ScenarioReport mergedScenario = null;
            for (var entry : parts.entrySet()) {
                var partScenario = entry.getValue().scenarios.stream()
                        .filter(scenario -> scenario.name.equals(name)).findFirst().orElse(null);
                if (partScenario == null) continue;
                if (mergedScenario == null) {
                    mergedScenario = new RunReport.ScenarioReport();
                    mergedScenario.name = partScenario.name;
                    mergedScenario.group = partScenario.group;
                    mergedScenario.className = partScenario.className;
                    mergedScenario.tags.addAll(partScenario.tags);
                    merged.scenarios.add(mergedScenario);
                }
                mergedScenario.status = RunReport.Status.worst(mergedScenario.status, partScenario.status);
                mergedScenario.durationMs = Math.max(mergedScenario.durationMs, partScenario.durationMs);
                if (mergedScenario.error == null) mergedScenario.error = partScenario.error;
                for (var step : partScenario.steps) {
                    if (step.role == null) step.role = entry.getKey();
                    mergedScenario.steps.add(step);
                }
            }
        }

        if (merged.scenarios.isEmpty()) {
            // The solo harness treats "selection matched nothing" as an error, and so does this one:
            // a typo'd -PldMpTest passing green is exactly the silent failure a gate must not allow.
            log("the selection '" + selection + "' matched no scenarios in any process");
            merged.status = RunReport.Status.worst(merged.status, RunReport.Status.ERROR);
            var empty = new RunReport.ScenarioReport();
            empty.name = "<selection>";
            empty.status = RunReport.Status.ERROR;
            empty.error = RunReport.ErrorInfo.of(new IllegalStateException(
                    "selection '" + selection + "' matched no MP scenarios"));
            merged.scenarios.add(empty);
        }

        // Before finalise, which derives durationMs from these two.
        merged.finishedAt = System.currentTimeMillis();
        ReportWriter.finalise(merged);
        // finalise recounts scenarios/steps/checks from the merged tree, but capture counts only
        // exist in the per-process totals.
        merged.totals.captures = parts.values().stream().mapToInt(part -> part.totals.captures).sum();

        try {
            var gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            Files.writeString(outDir.resolve(ReportWriter.REPORT_FILE), gson.toJson(merged));
            Files.writeString(outDir.resolve(ReportWriter.SUMMARY_FILE), summarise(merged, parts.keySet()));
        } catch (IOException e) {
            log("could not write the merged report: " + e);
            return 2;
        }

        log("");
        log(summarise(merged, parts.keySet()));
        return RunReport.Status.PASS.equals(merged.status) ? 0 : 1;
    }

    private static String summarise(RunReport merged, Set<String> reportedRoles) {
        var out = new StringBuilder();
        out.append("LDLib2 multi-process test: ").append(merged.status)
                .append(" (").append(merged.totals.passed).append('/').append(merged.totals.scenarios)
                .append(" scenarios, reports from ").append(reportedRoles).append(")\n");
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
                                out.append("      [").append(step.role).append("] step ").append(step.index)
                                        .append(" '").append(step.name).append("' ").append(step.status);
                                if (step.error != null) out.append(" - ").append(step.error.message);
                                out.append('\n');
                            });
                });
        return out.toString();
    }

    // endregion

    // region small utilities

    private static Map<String, String> parseArgs(String[] args) {
        var options = new LinkedHashMap<String, String>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (!args[i].startsWith("--")) throw new IllegalArgumentException("expected --key value pairs, got " + args[i]);
            options.put(args[i].substring(2), args[i + 1]);
        }
        if (!options.containsKey("projectDir") || !options.containsKey("out")) {
            throw new IllegalArgumentException("--projectDir and --out are required");
        }
        return options;
    }

    private static int pickFreePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private interface Check {
        boolean stop();
    }

    /** Polls until the condition holds, the abort check fires, or the timeout elapses. */
    private static boolean waitUntil(Check condition, long timeoutMs, Check abort) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Math.max(0, timeoutMs);
        while (System.currentTimeMillis() < deadline) {
            if (condition.stop()) return true;
            if (abort.stop()) return condition.stop();
            Thread.sleep(500);
        }
        return condition.stop();
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log("could not delete " + path + ": " + e);
                }
            });
        }
    }

    private static void log(String message) {
        System.out.println("[mptest] " + message);
    }

    // endregion
}
