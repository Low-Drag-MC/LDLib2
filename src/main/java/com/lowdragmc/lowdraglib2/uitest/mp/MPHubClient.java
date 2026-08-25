package com.lowdragmc.lowdraglib2.uitest.mp;

import com.lowdragmc.lowdraglib2.LDLib2;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A game process's connection to the orchestrator's control hub: one socket, a reader thread, and a
 * thread-safe mirror of everything the run has broadcast so far.
 *
 * <p>The mirror is the barrier mechanism. Steps and the server tick loop never block on the socket —
 * they poll {@link #segmentStatus} and friends from their own thread, and the reader thread keeps
 * the mirror current. A lost connection flips {@link #isLost()}, which every process treats as
 * "shut down now": the orchestrator dying must never leave game processes running.
 */
public final class MPHubClient {

    private final AtomicLong probeCounter = new AtomicLong();

    /** Set at {@link #connect}; used to drop relayed traffic addressed to other processes. */
    private volatile String selfRole = "";
    @Nullable
    private volatile Socket socket;
    @Nullable
    private volatile PrintWriter writer;
    private volatile boolean lost;
    private volatile boolean shutdownRequested;
    private volatile boolean begun;
    @Nullable
    private volatile MPMessages.Msg config;

    /** {@code scenario + '\0' + segment + '\0' + role} → status. */
    private final Map<String, String> segmentStatus = new ConcurrentHashMap<>();
    /** Probe responses by probe id, consumed by the requesting step. */
    private final Map<String, MPMessages.Msg> probeResponses = new ConcurrentHashMap<>();
    /** Probe requests awaiting service; only the dedicated server drains this. */
    private final Queue<MPMessages.Msg> probeRequests = new ConcurrentLinkedQueue<>();
    /** Roles whose process died, per the hub. */
    private final Set<String> deadRoles = ConcurrentHashMap.newKeySet();
    /** Roles that finished their whole run. */
    private final Set<String> runDoneRoles = ConcurrentHashMap.newKeySet();

    /**
     * Connects and says hello. Blocking, so call it off the render thread (a daemon thread at
     * bootstrap); everything after is push-driven by the reader thread this starts.
     */
    public void connect(String host, int port, String role) throws IOException {
        this.selfRole = role;
        var newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), 10_000);
        newSocket.setTcpNoDelay(true);
        this.socket = newSocket;
        this.writer = new PrintWriter(newSocket.getOutputStream(), false, StandardCharsets.UTF_8);

        var hello = MPMessages.Msg.of(MPMessages.HELLO);
        hello.role = role;
        hello.pid = ProcessHandle.current().pid();
        send(hello);

        var reader = new Thread(() -> readLoop(newSocket), "ldlib2-mptest-hub-reader");
        reader.setDaemon(true);
        reader.start();
        LDLib2.LOGGER.info("[mptest] '{}' connected to the control hub at {}:{}", role, host, port);
    }

    private void readLoop(Socket socket) {
        try (var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                MPMessages.Msg msg;
                try {
                    msg = MPMessages.decode(line);
                } catch (Exception e) {
                    LDLib2.LOGGER.warn("[mptest] undecodable hub message: {}", line, e);
                    continue;
                }
                handle(msg);
            }
        } catch (IOException e) {
            // fall through to the lost flag below
        }
        if (!shutdownRequested) {
            LDLib2.LOGGER.error("[mptest] the control hub connection dropped");
        }
        lost = true;
    }

    private void handle(MPMessages.Msg msg) {
        switch (msg.type == null ? "" : msg.type) {
            case MPMessages.CONFIG -> config = msg;
            case MPMessages.BEGIN -> begun = true;
            case MPMessages.SEGMENT_DONE -> {
                if (msg.scenario != null && msg.segment != null && msg.role != null) {
                    segmentStatus.put(statusKey(msg.scenario, msg.segment, msg.role),
                            msg.status == null ? MPMessages.DONE : msg.status);
                }
            }
            case MPMessages.PROBE_REQUEST -> {
                // The hub relays everything to everyone; only the server services probes. Without
                // this filter every client would accumulate the other clients' requests forever.
                if (MPMessages.SERVER_ROLE.equals(selfRole)) {
                    probeRequests.add(msg);
                }
            }
            case MPMessages.PROBE_RESPONSE -> {
                // Same story: probe ids are prefixed with the requesting role, so keep only ours.
                if (msg.probeId != null && msg.probeId.startsWith(selfRole + "#")) {
                    probeResponses.put(msg.probeId, msg);
                }
            }
            case MPMessages.RUN_DONE -> {
                if (msg.role != null) runDoneRoles.add(msg.role);
            }
            case MPMessages.FATAL -> {
                if (msg.role != null) {
                    deadRoles.add(msg.role);
                    LDLib2.LOGGER.error("[mptest] hub reports the '{}' process died", msg.role);
                }
            }
            case MPMessages.SHUTDOWN -> {
                shutdownRequested = true;
                lost = true;
            }
            default -> {
                // hellos/joins/scenarioDone from peers are progress noise a process does not act on
            }
        }
    }

    public void send(MPMessages.Msg msg) {
        var out = writer;
        if (out == null) return;
        synchronized (out) {
            out.println(MPMessages.encode(msg));
            out.flush();
        }
    }

    // region convenience senders

    public void sendServerReady(int gamePort) {
        var msg = MPMessages.Msg.of(MPMessages.SERVER_READY);
        msg.role = MPMessages.SERVER_ROLE;
        msg.gamePort = gamePort;
        send(msg);
    }

    public void sendJoined(String role) {
        var msg = MPMessages.Msg.of(MPMessages.JOINED);
        msg.role = role;
        send(msg);
    }

    public void sendSegmentDone(String role, String scenario, int segment, String status) {
        var msg = MPMessages.Msg.of(MPMessages.SEGMENT_DONE);
        msg.role = role;
        msg.scenario = scenario;
        msg.segment = segment;
        msg.status = status;
        send(msg);
        // Also mirror locally: a process's own completions take part in the same barrier arithmetic
        // as everyone else's, and the hub does not echo messages back to their sender.
        segmentStatus.put(statusKey(scenario, segment, role), status);
    }

    public void sendScenarioDone(String role, String scenario, String status) {
        var msg = MPMessages.Msg.of(MPMessages.SCENARIO_DONE);
        msg.role = role;
        msg.scenario = scenario;
        msg.status = status;
        send(msg);
    }

    public void sendRunDone(String role, String status) {
        var msg = MPMessages.Msg.of(MPMessages.RUN_DONE);
        msg.role = role;
        msg.status = status;
        send(msg);
    }

    /** @return the probe id to poll {@link #probeResponse} with */
    public String sendProbeRequest(String role, String scenario, int segment) {
        var probeId = role + "#" + probeCounter.incrementAndGet();
        var msg = MPMessages.Msg.of(MPMessages.PROBE_REQUEST);
        msg.role = role;
        msg.scenario = scenario;
        msg.segment = segment;
        msg.probeId = probeId;
        send(msg);
        return probeId;
    }

    public void sendProbeResponse(String probeId, @Nullable String valueJson, @Nullable String error) {
        var msg = MPMessages.Msg.of(MPMessages.PROBE_RESPONSE);
        msg.role = MPMessages.SERVER_ROLE;
        msg.probeId = probeId;
        msg.valueJson = valueJson;
        msg.error = error;
        send(msg);
    }

    // endregion

    // region mirror queries

    @Nullable
    public MPMessages.Msg config() {
        return config;
    }

    public boolean isBegun() {
        return begun;
    }

    public boolean isLost() {
        return lost;
    }

    /** For the connect path: a connection that never came up counts as lost too. */
    public void markLost() {
        lost = true;
    }

    @Nullable
    public String segmentStatus(String scenario, int segment, String role) {
        return segmentStatus.get(statusKey(scenario, segment, role));
    }

    public boolean isRoleDead(String role) {
        return deadRoles.contains(role);
    }

    /** Whether the role's whole run finished, or its process died — either way, no longer waiting on us. */
    public boolean isRoleFinished(String role) {
        return runDoneRoles.contains(role) || deadRoles.contains(role);
    }

    @Nullable
    public MPMessages.Msg probeResponse(String probeId) {
        return probeResponses.get(probeId);
    }

    public void clearProbeResponse(String probeId) {
        probeResponses.remove(probeId);
    }

    @Nullable
    public MPMessages.Msg pollProbeRequest() {
        return probeRequests.poll();
    }

    // endregion

    public void close() {
        shutdownRequested = true;
        var s = socket;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static String statusKey(String scenario, int segment, String role) {
        return scenario + '\0' + segment + '\0' + role;
    }
}
