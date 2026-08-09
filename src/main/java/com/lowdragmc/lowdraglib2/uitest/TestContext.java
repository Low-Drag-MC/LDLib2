package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.uitest.input.InputDriver;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The handle every client-thread step body receives. Everything a scenario can reach at runtime goes
 * through here: the client, the world, the open UI, element lookup, assertions, captures, and the
 * hop to the server thread.
 *
 * <p>Nothing here is a closed set. {@link #mc()}, {@link #server()} and {@link #onServer} exist so
 * that a scenario the framework's named steps do not cover can still be written as plain Java —
 * which is the point: the fluent vocabulary is convenience over these primitives, not a cage.
 */
@OnlyIn(Dist.CLIENT)
public final class TestContext {

    private final ScenarioRun run;
    private final UITestRunner runner;
    private RunReport.StepReport stepReport;

    TestContext(ScenarioRun run, UITestRunner runner) {
        this.run = run;
        this.runner = runner;
    }

    void bindStep(RunReport.StepReport stepReport) {
        this.stepReport = stepReport;
    }

    RunReport.StepReport stepReport() {
        return stepReport;
    }

    ScenarioRun run() {
        return run;
    }

    // region environment

    public Minecraft mc() {
        return Minecraft.getInstance();
    }

    @Nullable
    public LocalPlayer player() {
        return mc().player;
    }

    public LocalPlayer requirePlayer() {
        var player = player();
        if (player == null) throw new IllegalStateException("No client player - is a world loaded?");
        return player;
    }

    @Nullable
    public ClientLevel level() {
        return mc().level;
    }

    /** The integrated server. {@code null} on the title screen or in multiplayer. */
    @Nullable
    public MinecraftServer server() {
        return mc().getSingleplayerServer();
    }

    public MinecraftServer requireServer() {
        var server = server();
        if (server == null) throw new IllegalStateException("No integrated server - is a world loaded?");
        return server;
    }

    /**
     * The server-side player, for <em>reading</em> from the client thread. Mutate server state only
     * inside {@link #onServer(Consumer)} — touching it from here races the server tick.
     */
    @Nullable
    public ServerPlayer serverPlayer() {
        var server = server();
        if (server == null) return null;
        var players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.getFirst();
    }

    @Nullable
    public ServerLevel serverLevel() {
        var player = serverPlayer();
        return player == null ? null : player.serverLevel();
    }

    @Nullable
    public Screen screen() {
        return mc().screen;
    }

    /** @throws IllegalStateException if the open screen is not of that type */
    public <T extends Screen> T screenAs(Class<T> type) {
        var screen = screen();
        if (!type.isInstance(screen)) {
            throw new IllegalStateException("Expected a " + type.getSimpleName() + " but the open screen is "
                    + (screen == null ? "null" : screen.getClass().getSimpleName()));
        }
        return type.cast(screen);
    }

    /**
     * The {@link ModularUI} behind the open screen, however it got there — a plain
     * {@code ModularUIScreen}, a menu-backed {@code ModularUIContainerScreen}, or a widget attached
     * to some other screen.
     */
    @Nullable
    public ModularUI ui() {
        return ModularUI.of(screen());
    }

    public ModularUI requireUI() {
        var ui = ui();
        if (ui == null) {
            throw new IllegalStateException("No ModularUI on the open screen ("
                    + (screen() == null ? "no screen" : screen().getClass().getSimpleName()) + ")");
        }
        return ui;
    }

    /**
     * The client's copy of a block entity — what the player can actually see. Comparing this against
     * {@link ServerContext#blockEntity} is how a sync test proves a change crossed the wire.
     */
    @Nullable
    public <T extends BlockEntity> T clientBlockEntity(BlockPos pos, Class<T> type) {
        var level = level();
        if (level == null) return null;
        var blockEntity = level.getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    // endregion

    // region scratch state

    /** Shared with {@link ServerContext} for the whole scenario; cleared between scenarios. */
    public Map<String, Object> state() {
        return run.state;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) run.state.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T fallback) {
        return (T) run.state.getOrDefault(key, fallback);
    }

    public <T> T put(String key, T value) {
        run.state.put(key, value);
        return value;
    }

    // endregion

    // region targeting

    /**
     * Resolves a selector to exactly one element.
     *
     * @throws IllegalStateException if it matches zero elements or more than one — both mean the
     *         step would otherwise act on something the author did not intend
     */
    public ElementRef el(String selector) {
        return query(selector).one();
    }

    public Optional<ElementRef> elOpt(String selector) {
        return query(selector).optional();
    }

    public List<ElementRef> all(String selector) {
        return query(selector).list();
    }

    public boolean exists(String selector) {
        return query(selector).count() > 0;
    }

    public int count(String selector) {
        return query(selector).count();
    }

    /** Starts a filter chain for anything CSS selectors cannot express. See {@link ElementQuery}. */
    public ElementQuery query() {
        return new ElementQuery(requireUI());
    }

    public ElementQuery query(String selector) {
        return new ElementQuery(requireUI()).select(selector);
    }

    /** Resolves a stable structural path from a report back to a live element. */
    @Nullable
    public ElementRef byPath(String path) {
        var element = com.lowdragmc.lowdraglib2.uitest.target.ElementPath.resolve(requireUI(), path);
        return element == null ? null : new ElementRef(element, path);
    }

    // endregion

    // region assertions

    /** Records an expectation. A failure marks the step failed and the scenario keeps going. */
    public void check(String description, boolean condition) {
        var result = new RunReport.CheckResult();
        result.desc = description;
        result.passed = condition;
        stepReport.checks.add(result);
    }

    public void check(String description, boolean condition, Object expected, Object actual) {
        var result = new RunReport.CheckResult();
        result.desc = description;
        result.passed = condition;
        result.expected = String.valueOf(expected);
        result.actual = String.valueOf(actual);
        stepReport.checks.add(result);
    }

    /**
     * A hard precondition: throws instead of recording a failure, so the error policy applies and
     * the rest of the scenario does not run against a state it cannot handle.
     */
    public void require(String description, boolean condition) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    public void log(String message) {
        stepReport.log.add(message);
    }

    /** Attaches an arbitrary named value to this step in the report. */
    public void attach(String key, String value) {
        stepReport.attachments.put(key, value);
    }

    // endregion

    // region step machine control

    /**
     * Asks for this step to run again on the next frame.
     *
     * <p>The only waiting primitive. The step body should call it and return; the runner re-invokes
     * it next frame without applying the settle delay, until {@link ScenarioOptions#defaultTimeoutMs}
     * elapses, at which point the step fails with {@code waitingFor} in the message.
     */
    public void repeat(String waitingFor) {
        run.repeatRequested = true;
        run.waitingFor = waitingFor;
    }

    public void repeat() {
        repeat(run.current() == null ? "unspecified" : run.current().name);
    }

    /** {@code true} on the step's first attempt — use it to do setup once before polling. */
    public boolean firstAttempt() {
        return run.attempt() <= 1;
    }

    public int attempt() {
        return run.attempt();
    }

    /** Milliseconds since this step's first attempt. */
    public long elapsedMs() {
        return run.waitedNanos(System.nanoTime()) / 1_000_000L;
    }

    // endregion

    // region thread hops

    /**
     * Runs a body on the server thread and completes when it finishes.
     *
     * <p>The returned future must be polled with {@link #repeat(String)}, never blocked on: the
     * server thread can be busy generating chunks, and blocking the render thread would stall the
     * very frame pump the runner needs to make progress. The {@code server(..)} step does this for
     * you; this method is for scenarios that need finer control.
     */
    public CompletableFuture<Void> onServer(Consumer<ServerContext> body) {
        var server = requireServer();
        var future = new CompletableFuture<Void>();
        var serverContext = new ServerContext(server, run.state, stepReport);
        server.execute(() -> {
            try {
                body.accept(serverContext);
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    public <T> CompletableFuture<T> onServerGet(Function<ServerContext, T> body) {
        var server = requireServer();
        var future = new CompletableFuture<T>();
        var serverContext = new ServerContext(server, run.state, stepReport);
        server.execute(() -> {
            try {
                future.complete(body.apply(serverContext));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    // endregion

    // region capture

    /** Requests a full-frame screenshot. Taken on the next frame, so it shows post-step state. */
    public void screenshot(String label) {
        runner.requestFullCapture(run, stepReport, label);
    }

    public void screenshotElement(String label, ElementRef element) {
        runner.requestElementCapture(run, stepReport, label, element);
    }

    // endregion

    public InputDriver input() {
        return runner.input();
    }

    public ScenarioOptions options() {
        return run.options;
    }

    public UITestRunner runner() {
        return runner;
    }
}
