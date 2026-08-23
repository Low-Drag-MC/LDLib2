package com.lowdragmc.lowdraglib2.uitest.mp;

import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ServerContext;
import com.lowdragmc.lowdraglib2.uitest.TestContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Records the segments of a {@link MPScenario}. Runs in every process; must be deterministic.
 *
 * <p>Segments are the unit of cross-process ordering. Within a {@code client(..)} block the owning
 * client runs ordinary {@link ScenarioBuilder} steps one per frame, exactly as a solo scenario
 * would; the other processes see the whole block as a single barrier. Note that inside such a block
 * the integrated-server helpers ({@code b.server(..)}, {@code b.setBlock(..)}, {@code
 * b.waitForSync(..)} and friends) have no server to talk to — world mutation and authoritative
 * assertions belong in {@link #server} segments, and cross-process value convergence in
 * {@link #sync}.
 */
public final class MPScenarioBuilder {

    private final List<MPSegment> segments = new ArrayList<>();
    private final List<MPSegment> teardownSegments = new ArrayList<>();
    private long nextTimeoutMs = -1;

    public List<MPSegment> segments() {
        return segments;
    }

    /**
     * Teardown segments always run — after success, failure or abort — but <b>without barriers</b>:
     * each process runs only the teardown segments it owns, best effort, so a dead process cannot
     * hang everyone else's cleanup.
     */
    public List<MPSegment> teardownSegments() {
        return teardownSegments;
    }

    private MPScenarioBuilder add(List<MPSegment> target, MPSegment.Kind kind, String name,
                                  @Nullable String role,
                                  @Nullable Consumer<ServerContext> serverBody,
                                  @Nullable Predicate<ServerContext> serverCondition,
                                  @Nullable Consumer<ScenarioBuilder> clientBlock,
                                  @Nullable Function<ServerContext, ?> serverProbe,
                                  @Nullable Function<TestContext, ?> clientProbe,
                                  @Nullable String stateKey, @Nullable Class<?> valueType) {
        target.add(new MPSegment(target.size(), kind, name, role, serverBody, serverCondition,
                clientBlock, serverProbe, clientProbe, stateKey, valueType, nextTimeoutMs));
        nextTimeoutMs = -1;
        return this;
    }

    /** Overrides the timeout for the next segment only. */
    public MPScenarioBuilder timeoutMs(long ms) {
        nextTimeoutMs = ms;
        return this;
    }

    /** One task on the dedicated server thread. */
    public MPScenarioBuilder server(String name, Consumer<ServerContext> body) {
        return add(segments, MPSegment.Kind.SERVER, name, null, body, null, null, null, null, null, null);
    }

    /** Re-evaluated every server tick until true, then the barrier releases. */
    public MPScenarioBuilder serverWaitUntil(String description, Predicate<ServerContext> condition) {
        return add(segments, MPSegment.Kind.SERVER_WAIT, description, null, null, condition, null, null, null,
                null, null);
    }

    /**
     * Lets {@code ticks} server ticks pass before the barrier releases.
     *
     * <p>The cross-process counterpart of {@code ScenarioBuilder#settleMs}, and it counts <b>ticks
     * rather than milliseconds</b> because that is what the thing being waited for is measured in:
     * a teleport takes effect on a tick, an attachment reaches its watchers on a tick, and a
     * dedicated server under a cold-start chunk load can spend a second on one of them.
     *
     * <p>The first evaluation records the starting tick in the scenario's scratch state, under a key
     * derived from this segment's position — so it is fixed at definition time, which keeps
     * {@code define()} deterministic, and it is per scenario, so two settles never see each other.
     */
    public MPScenarioBuilder serverSettle(int ticks) {
        String key = "ldlib2.mp.settle." + segments.size();
        return serverWaitUntil("settle " + ticks + " server ticks", sc -> {
            Integer from = sc.get(key);
            if (from == null) {
                sc.put(key, sc.server().getTickCount());
                return false;
            }
            return sc.server().getTickCount() - from >= ticks;
        });
    }

    /** A block of ordinary {@link ScenarioBuilder} steps run by the given client role. */
    public MPScenarioBuilder client(String role, String name, Consumer<ScenarioBuilder> block) {
        return add(segments, MPSegment.Kind.CLIENT, name, role, null, null, block, null, null, null, null);
    }

    /** The same block run by every client concurrently; the barrier waits for all of them. */
    public MPScenarioBuilder allClients(String name, Consumer<ScenarioBuilder> block) {
        return add(segments, MPSegment.Kind.ALL_CLIENTS, name, null, null, null, block, null, null, null, null);
    }

    /**
     * Waits until one client's view of a value equals the dedicated server's — the cross-process
     * counterpart of {@code ScenarioBuilder#waitForSync}. The client polls; each round trip fetches
     * the server value as a probe over the control channel. Values are compared by their Gson JSON
     * form, so keep them to primitives, strings and simple data shapes.
     */
    public <T> MPScenarioBuilder sync(String description, String clientRole,
                                      Function<ServerContext, T> serverValue,
                                      Function<TestContext, T> clientValue) {
        return add(segments, MPSegment.Kind.SYNC, description, clientRole, null, null, null,
                serverValue, clientValue, null, null);
    }

    /** Like {@link #sync}, but every client must converge before the barrier releases. */
    public <T> MPScenarioBuilder syncAll(String description,
                                         Function<ServerContext, T> serverValue,
                                         Function<TestContext, T> clientValue) {
        return add(segments, MPSegment.Kind.SYNC, description, null, null, null, null,
                serverValue, clientValue, null, null);
    }

    /**
     * Copies one value from the dedicated server into every client's scratch state, so a later
     * client step can assert against it <b>with a tolerance</b>.
     *
     * <p>{@link #sync} is the right tool when the two sides must end up holding the same value:
     * a synced integer, a block state, a count. It is the wrong tool the moment the quantity is
     * continuous — two processes whose body angles differ by a twentieth of a degree agree about
     * everything that matters, and no amount of rounding makes a live float converge on the nose.
     * This hands the client the server's number instead and gets out of the way:
     *
     * <pre>{@code
     * .fetch("server.yaw", "the body angle the server holds", Double.class,
     *         sc -> (double) mover(sc).yaw())
     * .client("B", "and this client agrees", b -> b.check("within a tick of turning", ctx -> {
     *     double server = ctx.get("server.yaw");
     *     double here = myMover(ctx).yaw();
     *     ctx.log("server " + server + ", here " + here);          // in the report either way
     *     return Math.abs(server - here) < 2.0;
     * }))
     * }</pre>
     *
     * <p>Read once, when the segment runs, and stored — so what a later step compares against is
     * the server as it was at a known point in the scenario rather than whenever the step happened
     * to fire. Put the fetch where you want the reading taken.
     *
     * <p>The value travels as JSON over the control channel and is decoded back into {@code type},
     * so the same rule as {@link #sync} applies: primitives, strings and simple data shapes.
     * {@code Double.class} rather than {@code Float.class} for anything numeric — Gson reads a bare
     * JSON number as a double, and asking for a float back is a needless place to lose one.
     *
     * @param stateKey where the value lands, for {@code ctx.get(stateKey)}
     * @param type     what to decode the value back into
     */
    public <T> MPScenarioBuilder fetch(String stateKey, String description, Class<T> type,
                                       Function<ServerContext, T> serverValue) {
        return add(segments, MPSegment.Kind.FETCH, description, null, null, null, null,
                serverValue, null, stateKey, type);
    }

    /** Like {@link #fetch}, but only one client takes the reading. */
    public <T> MPScenarioBuilder fetchOn(String clientRole, String stateKey, String description,
                                         Class<T> type, Function<ServerContext, T> serverValue) {
        return add(segments, MPSegment.Kind.FETCH, description, clientRole, null, null, null,
                serverValue, null, stateKey, type);
    }

    /** Server-side cleanup. See {@link #teardownSegments()} for the no-barrier semantics. */
    public MPScenarioBuilder teardownServer(String name, Consumer<ServerContext> body) {
        return add(teardownSegments, MPSegment.Kind.SERVER, name, null, body, null, null, null, null,
                null, null);
    }

    /** Cleanup on one client. */
    public MPScenarioBuilder teardownClient(String role, String name, Consumer<ScenarioBuilder> block) {
        return add(teardownSegments, MPSegment.Kind.CLIENT, name, role, null, null, block, null, null,
                null, null);
    }

    /** Cleanup on every client. */
    public MPScenarioBuilder teardownAllClients(String name, Consumer<ScenarioBuilder> block) {
        return add(teardownSegments, MPSegment.Kind.ALL_CLIENTS, name, null, null, null, block, null, null,
                null, null);
    }
}
