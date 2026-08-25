package com.lowdragmc.lowdraglib2.uitest.mp;

import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * One unit of a {@link MPScenario}: a piece of work owned by exactly one side, with a barrier after
 * it. Every process builds the same segment list from {@code define()}; a process executes the
 * segments it owns and waits for the rest.
 *
 * <p>The {@code clientBlock} is deliberately opaque to non-owners — the dedicated-server process
 * stores it without ever invoking it, which is what keeps client-only classes out of that process.
 */
public final class MPSegment {

    public enum Kind {
        /** One task on the dedicated server thread. */
        SERVER,
        /** A predicate re-evaluated every server tick until true or timed out. */
        SERVER_WAIT,
        /** A block of ordinary {@link ScenarioBuilder} steps, run by one client. */
        CLIENT,
        /** The same block run by every client concurrently; the barrier waits for all of them. */
        ALL_CLIENTS,
        /**
         * The cross-process sync primitive: the owning client(s) poll until their local value equals
         * the dedicated server's, fetched as a probe through the control channel.
         */
        SYNC,
        /**
         * Copies one value from the dedicated server into the owning client(s)' scratch state, once,
         * and moves on.
         *
         * <p>The sibling of {@link #SYNC} for the case it cannot express: <b>an assertion with a
         * tolerance</b>. {@code SYNC} compares by JSON equality, which is exactly right for a synced
         * integer and useless for a body angle — two processes that agree to a twentieth of a degree
         * are agreeing, and no rounding of a live value converges reliably. Fetching the number
         * instead lets the client write the comparison it actually means, in its own step, with its
         * own message: {@code "server -90.0, here -89.7, 0.3 apart"}.
         *
         * <p>The transport is the same probe request {@code SYNC} polls with; the difference is that
         * the answer is stored rather than compared.
         */
        FETCH
    }

    public final int index;
    public final Kind kind;
    public final String name;
    /**
     * Owner role for {@link Kind#CLIENT}; the polling role for {@link Kind#SYNC} ({@code null}
     * means every client must converge). {@code null} for server-owned and all-client segments.
     */
    @Nullable
    public final String role;
    @Nullable
    public final Consumer<ServerContext> serverBody;
    @Nullable
    public final Predicate<ServerContext> serverCondition;
    @Nullable
    public final Consumer<ScenarioBuilder> clientBlock;
    @Nullable
    public final Function<ServerContext, ?> serverProbe;
    @Nullable
    public final Function<TestContext, ?> clientProbe;
    /** {@link Kind#FETCH}: the scratch-state key the fetched value is stored under. */
    @Nullable
    public final String stateKey;
    /** {@link Kind#FETCH}: what the fetched JSON is decoded back into. */
    @Nullable
    public final Class<?> valueType;
    /** Budget for this segment's own work. {@code -1} means the scenario default. */
    public final long timeoutMs;

    MPSegment(int index, Kind kind, String name, @Nullable String role,
              @Nullable Consumer<ServerContext> serverBody,
              @Nullable Predicate<ServerContext> serverCondition,
              @Nullable Consumer<ScenarioBuilder> clientBlock,
              @Nullable Function<ServerContext, ?> serverProbe,
              @Nullable Function<TestContext, ?> clientProbe,
              @Nullable String stateKey, @Nullable Class<?> valueType,
              long timeoutMs) {
        this.index = index;
        this.kind = kind;
        this.name = name;
        this.role = role;
        this.serverBody = serverBody;
        this.serverCondition = serverCondition;
        this.clientBlock = clientBlock;
        this.serverProbe = serverProbe;
        this.clientProbe = clientProbe;
        this.stateKey = stateKey;
        this.valueType = valueType;
        this.timeoutMs = timeoutMs;
    }

    /** Whether the dedicated server owns (executes) this segment. */
    public boolean serverOwned() {
        return kind == Kind.SERVER || kind == Kind.SERVER_WAIT;
    }

    /** Whether the given client role executes this segment. */
    public boolean ownedByClient(String clientRole) {
        return switch (kind) {
            case SERVER, SERVER_WAIT -> false;
            case ALL_CLIENTS -> true;
            case CLIENT, SYNC, FETCH -> role == null || role.equals(clientRole);
        };
    }

    /** Which roles announce this segment, and therefore who a barrier on it waits for. */
    public Set<String> reporters(String serverRole, List<String> allClients) {
        return switch (kind) {
            case SERVER, SERVER_WAIT -> Set.of(serverRole);
            case CLIENT -> Set.of(Objects.requireNonNull(role));
            case ALL_CLIENTS -> new LinkedHashSet<>(allClients);
            case SYNC, FETCH -> role == null ? new LinkedHashSet<>(allClients) : Set.of(role);
        };
    }

    @Override
    public String toString() {
        return "[" + index + " " + kind + (role == null ? "" : ":" + role) + "] " + name;
    }
}
