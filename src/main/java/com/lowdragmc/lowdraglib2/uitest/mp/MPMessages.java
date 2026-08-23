package com.lowdragmc.lowdraglib2.uitest.mp;

import com.google.gson.Gson;

import java.util.List;

/**
 * The control-channel protocol: JSON lines over TCP, one {@link Msg} per line.
 *
 * <p>Deliberately free of any Minecraft or LDLib2 class reference: the orchestrator process (which
 * has no game on its classpath) speaks the same protocol as the game processes. The hub itself is a
 * dumb relay — every message a child sends is rebroadcast to the other children, and each process
 * decides locally what a segment's completion means. That keeps segment semantics (who reports an
 * {@code ALL_CLIENTS} segment, what a sync segment waits for) entirely inside the processes that
 * defined the scenario, where the information already lives.
 */
public final class MPMessages {

    public static final Gson GSON = new Gson();

    // child -> hub (relayed to the other children)
    public static final String HELLO = "hello";
    public static final String SERVER_READY = "serverReady";
    public static final String JOINED = "joined";
    public static final String SEGMENT_DONE = "segmentDone";
    public static final String SCENARIO_DONE = "scenarioDone";
    public static final String PROBE_REQUEST = "probeRequest";
    public static final String PROBE_RESPONSE = "probeResponse";
    public static final String RUN_DONE = "runDone";

    // hub -> children
    public static final String CONFIG = "config";
    public static final String BEGIN = "begin";
    /** Broadcast by the hub when a registered child's connection dies. */
    public static final String FATAL = "fatal";
    public static final String SHUTDOWN = "shutdown";

    // segment statuses carried by SEGMENT_DONE
    /** The owner ran the segment to its end (soft check failures included — they do not gate others). */
    public static final String DONE = "DONE";
    /** The owner's segment threw; waiting processes should abort the scenario. */
    public static final String ERROR = "ERROR";
    /** The owner aborted before reaching the segment; waiting processes should abort too. */
    public static final String ABORTED = "ABORTED";

    /** The dedicated server's role name on the wire; client roles are scenario-defined labels. */
    public static final String SERVER_ROLE = "server";

    private MPMessages() {
    }

    /**
     * One flat message. A single class with nullable fields rather than a type hierarchy: the
     * protocol is tiny, and Gson round-trips it without adapters.
     */
    public static final class Msg {
        public String type;
        /** Sender's role, or the subject role for {@link #FATAL}. */
        public String role;
        public Long pid;
        // config / serverReady
        public Integer gamePort;
        public String selection;
        public List<String> clientRoles;
        // segment / scenario progress
        public String scenario;
        public Integer segment;
        public String status;
        // probes
        public String probeId;
        public String valueJson;
        public String error;

        public static Msg of(String type) {
            var msg = new Msg();
            msg.type = type;
            return msg;
        }

        @Override
        public String toString() {
            return encode(this);
        }
    }

    public static String encode(Msg msg) {
        return GSON.toJson(msg);
    }

    public static Msg decode(String line) {
        return GSON.fromJson(line, Msg.class);
    }
}
