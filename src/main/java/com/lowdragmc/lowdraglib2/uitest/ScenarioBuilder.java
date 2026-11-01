package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.MinecraftAccessor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Menu;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.UISurface;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.record.CursorPath;
import com.lowdragmc.lowdraglib2.uitest.record.Pacing;
import com.lowdragmc.lowdraglib2.uitest.record.TakeOptions;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/**
 * Records the steps of a {@link UIScenario}.
 *
 * <p>Everything here is convenience over three primitives — {@link #step}, {@link #server} and
 * {@link #waitUntil} — so a case the named steps do not cover is still one lambda away rather than a
 * dead end.
 *
 * <p>Gestures expand into several steps on purpose. The runner runs one step per rendered frame, and
 * hover, drag state and layout are only recomputed while rendering: a click issued in the same frame
 * as the cursor move would resolve against the previous hover and silently do nothing.
 *
 * <p>Deliberately <b>not</b> {@code @OnlyIn(Dist.CLIENT)} even though this is a client API: a
 * {@link com.lowdragmc.lowdraglib2.uitest.mp.MPScenario} class carries {@code Consumer<ScenarioBuilder>}
 * lambdas in its synthetic method signatures, and linking that class on the dedicated-server process
 * must be able to <em>load</em> this one. It is never linked there — client blocks only execute on
 * the client that owns them — so the client-only types in the method bodies are never resolved.
 */
public final class ScenarioBuilder {

    private final List<Step> steps = new ArrayList<>();
    private final List<Step> teardownSteps = new ArrayList<>();
    private long nextSettleMs = -1;
    private long nextTimeoutMs = -1;
    private String currentGroup;

    List<Step> steps() {
        return steps;
    }

    List<Step> teardownSteps() {
        return teardownSteps;
    }

    private ScenarioBuilder add(String name, StepKind kind, Step.Body body) {
        var step = new Step(name, kind, body);
        step.settleMs = nextSettleMs;
        step.timeoutMs = nextTimeoutMs;
        step.group = currentGroup;
        nextSettleMs = -1;
        nextTimeoutMs = -1;
        steps.add(step);
        return this;
    }

    // region structure & escape hatches

    /** Arbitrary client-thread code. The general-purpose escape hatch. */
    public ScenarioBuilder step(String name, Consumer<TestContext> body) {
        return add(name, StepKind.CUSTOM, body::accept);
    }

    /**
     * Arbitrary server-thread code, submitted and awaited.
     *
     * <p>Polls rather than blocks: the server thread may be generating chunks, and blocking the
     * render thread would stall the frame pump the runner itself depends on.
     */
    public ScenarioBuilder server(String name, Consumer<ServerContext> body) {
        return serverStep(name, StepKind.WORLD, body);
    }

    private ScenarioBuilder serverStep(String name, StepKind kind, Consumer<ServerContext> body) {
        return add(name, kind, awaitServer("server task: " + name,
                ctx -> ctx.onServer(body), (ctx, ignored) -> {}));
    }

    /** Runs a server-thread function and stores its result in the scratch map under {@code key}. */
    public <T> ScenarioBuilder serverGet(String name, String key, Function<ServerContext, T> body) {
        return add(name, StepKind.WORLD, awaitServer("server task: " + name,
                ctx -> ctx.onServerGet(body), (ctx, value) -> ctx.put(key, value)));
    }

    /**
     * A step body that submits work to the server thread once and then polls until it completes.
     *
     * <p>Polling rather than blocking is not a style choice: the render thread is what drives the
     * step machine, and the server thread it would be waiting on can itself be waiting on chunk
     * generation.
     */
    private static <T> Step.Body awaitServer(String waitingFor,
                                             Function<TestContext, CompletableFuture<T>> submit,
                                             java.util.function.BiConsumer<TestContext, T> onDone) {
        var pending = new ArrayList<CompletableFuture<T>>(1);
        return ctx -> {
            if (pending.isEmpty()) {
                pending.add(submit.apply(ctx));
            }
            var future = pending.getFirst();
            if (!future.isDone()) {
                ctx.repeat(waitingFor);
                return;
            }
            pending.clear();
            onDone.accept(ctx, future.join());
        };
    }

    /**
     * A step body that re-asks the server until the answer is acceptable. Each round trip is a fresh
     * submission, so the check always sees current state.
     *
     * @param unsatisfiedReason why the value is not acceptable yet, or {@code null} if it is. It
     *                          supplies the wait message rather than the caller so a timeout can
     *                          report the actual values it was comparing, not just what it was doing.
     */
    private static <T> Step.Body pollServer(String waitingFor,
                                            Function<TestContext, CompletableFuture<T>> submit,
                                            java.util.function.BiFunction<TestContext, T, String> unsatisfiedReason) {
        var pending = new ArrayList<CompletableFuture<T>>(1);
        return ctx -> {
            if (pending.isEmpty()) {
                pending.add(submit.apply(ctx));
            }
            var future = pending.getFirst();
            if (!future.isDone()) {
                ctx.repeat(waitingFor + " (reading server)");
                return;
            }
            pending.clear();
            var reason = unsatisfiedReason.apply(ctx, future.join());
            if (reason != null) {
                ctx.repeat(reason);
            }
        };
    }

    /** Labels a block of steps in the report. Groups do not nest. */
    public ScenarioBuilder group(String label, Consumer<ScenarioBuilder> body) {
        var previous = currentGroup;
        currentGroup = label;
        body.accept(this);
        currentGroup = previous;
        return this;
    }

    /**
     * Records the steps in {@code body} as one video, {@code build/ldlib2-recordings/<recording>/<name>.mp4},
     * when they run as part of a {@link UIRecording} ({@code -PldRecord}). Anywhere else, a test included,
     * the steps run exactly as if the block were not there.
     *
     * <p>While a take records, time is stepped: every rendered frame is one video frame and advances
     * Minecraft's clock ({@code Util.getNanos()}) by exactly {@code 1/fps} seconds, however long the
     * frame really took. Ticks, transitions, {@link #waitMs} and {@link #hold} all follow it, so the
     * video plays at the right speed even when 4K readback and encoding run far slower than real time.
     * Code that reads {@code System.nanoTime()} directly is not slowed with it.
     *
     * <p>The video covers the first frame rendered after the take opens up to the last one before its
     * closing step. That step waits for ffmpeg to finish the file, then adds the video to the report as
     * a {@code VIDEO} capture, with frame timings in the step log. A step that throws inside the block
     * aborts the scenario as usual; the take is then closed during cleanup and keeps what was recorded.
     *
     * <p>Takes do not nest, and one take is open at a time: starting a second before the first closes
     * fails that step. The window must not be resized while a take records.
     *
     * <p>Gestures inside a take keep a person's rhythm: the cursor travels to each target along a gentle
     * curve (and across-then-down inside a menu), rests before pressing, and text is typed at typing
     * speed. The video shows the pointer, a ripple for each click and the keys being pressed. None of
     * that happens outside a take, where gestures run at test speed.
     *
     * @param name file name of the video, without extension; unique within the scenario
     * @see #take(String, Consumer, Consumer)
     * @see #hold(long)
     */
    public ScenarioBuilder take(String name, Consumer<ScenarioBuilder> body) {
        return take(name, options -> {}, body);
    }

    /**
     * {@link #take(String, Consumer)}, recording only the part of the screen around one element: its
     * bounds when the take starts, 12 GUI pixels around them, and a strip below for the key display, so
     * it never covers the UI being shown. Shorthand for {@code take(name, o -> o.region(selector), body)}.
     *
     * @param regionSelector the element to frame; it must be laid out when the take starts
     */
    public ScenarioBuilder take(String name, String regionSelector, Consumer<ScenarioBuilder> body) {
        return take(name, options -> options.region(regionSelector), body);
    }

    /**
     * {@link #take(String, Consumer)} with its framing and encoding set by {@link TakeOptions}: the region
     * recorded, the video's size, its frame rate, which overlays it shows, which caption language is burned
     * in, and where the poster still comes from.
     *
     * <p>Next to the video, a take writes {@code <name>.png} (the poster), {@code <name>.<language>.vtt} for
     * every language {@link #caption(String, String)} was given, and {@code <name>.timeline.json} with the
     * video time of every step, press, key and caption. All are listed in the report.
     */
    public ScenarioBuilder take(String name, Consumer<TakeOptions> options, Consumer<ScenarioBuilder> body) {
        var configured = new TakeOptions();
        options.accept(configured);
        direction("take " + name, StepKind.CAPTURE, -1, ctx -> ctx.runner().startTake(ctx, name, configured));
        body.accept(this);
        // wall-clock time for ffmpeg to flush, which a software encoder at 4K can take a while to do
        return direction("take " + name + " :end", StepKind.CAPTURE, 120_000, ctx -> {
            if (!ctx.runner().finishTake(ctx)) ctx.repeat("ffmpeg to finish '" + name + "'");
        });
    }

    /** {@link #caption(String, String)} in English, {@code en}. */
    public ScenarioBuilder caption(String text) {
        return caption("en", text);
    }

    /**
     * Shows a caption from the next frame of the take, written to {@code <take>.<language>.vtt} and, if the
     * take burns that language in, drawn into the video. It lasts until the next caption in the same
     * language, an empty {@code text}, or the end of the take. Give each language its own call at the same
     * point to get matching tracks:
     *
     * <pre>{@code
     * .caption("Right click the canvas to add a node")
     * .caption("zh", "在画布上右键添加节点")
     * }</pre>
     *
     * <p>Does nothing outside a recording take, so captions cost a test run nothing.
     *
     * @param language a WebVTT language tag such as {@code en} or {@code zh}; also the file suffix
     */
    public ScenarioBuilder caption(String language, String text) {
        return direction("caption " + language + " " + text, StepKind.CUSTOM, -1, ctx -> {
            var take = ctx.runner().currentTake();
            if (take != null) take.caption(language, text);
        });
    }

    /**
     * Dims everything in the video but one element, following it if it moves, until
     * {@link #clearSpotlight()}. Fades in and out. Does nothing outside a recording take.
     */
    public ScenarioBuilder spotlight(String selector) {
        return spotlight(selector, ctx -> ctx.el(selector).element());
    }

    /** {@link #spotlight(String)} on an element found when the step runs. */
    public ScenarioBuilder spotlight(String description, Function<TestContext, UIElement> element) {
        return direction("spotlight " + description, StepKind.CUSTOM, -1, ctx -> {
            var take = ctx.runner().currentTake();
            if (take == null) return;
            var target = element.apply(ctx);
            take.spotlight(() -> {
                if (target.getModularUI() == null) return null;
                var bounds = ElementBounds.of(target);
                return new float[]{bounds.x(), bounds.y(), bounds.width(), bounds.height()};
            }, description);
        });
    }

    public ScenarioBuilder clearSpotlight() {
        return direction("clear spotlight", StepKind.CUSTOM, -1, ctx -> {
            var take = ctx.runner().currentTake();
            if (take != null) take.spotlight(null, "");
        });
    }

    /**
     * Glides the video's camera in until the element, with {@value #ZOOM_PADDING} GUI pixels around it,
     * fills the frame, keeping the frame's aspect ratio, over {@code millis} of video time; the step ends
     * when the camera arrives. The UI and pointer are magnified; the key display and captions keep their
     * size. Record a larger window than the video ({@link TakeOptions#size}) for a zoom that stays sharp.
     * Does nothing outside a recording take.
     */
    public ScenarioBuilder zoom(String selector, long millis) {
        return zoom(selector, ctx -> ctx.el(selector).element(), millis);
    }

    /** {@link #zoom(String, long)} on an element found when the step runs. */
    public ScenarioBuilder zoom(String description, Function<TestContext, UIElement> element, long millis) {
        return zoomTo(description, ctx -> {
            var bounds = ElementBounds.of(element.apply(ctx));
            return new float[]{bounds.x(), bounds.y(), bounds.width(), bounds.height()};
        }, millis);
    }

    /**
     * {@link #zoom(String, long)} on a rectangle computed when the step runs, {@code x, y, width, height}
     * in GUI-scaled screen space: room for a submenu that has not opened yet, two elements at once.
     */
    public ScenarioBuilder zoomTo(String description, Function<TestContext, float[]> rect, long millis) {
        var started = new boolean[1];
        return direction("zoom " + description, StepKind.CUSTOM, millis + 5_000, ctx -> {
            var take = ctx.runner().currentTake();
            if (take == null) {
                started[0] = false;
                return;
            }
            if (!started[0]) {
                take.zoomTo(rect.apply(ctx), ZOOM_PADDING, millis);
                started[0] = true;
            }
            if (take.isCameraMoving()) {
                ctx.repeat("camera zooming");
            } else {
                started[0] = false;
            }
        });
    }

    /** Glides the camera back to the take's own framing; the step ends when it arrives. */
    public ScenarioBuilder zoomOut(long millis) {
        var started = new boolean[1];
        return direction("zoom out", StepKind.CUSTOM, millis + 5_000, ctx -> {
            var take = ctx.runner().currentTake();
            if (take == null) {
                started[0] = false;
                return;
            }
            if (!started[0]) {
                take.zoomOut(millis);
                started[0] = true;
            }
            if (take.isCameraMoving()) {
                ctx.repeat("camera zooming");
            } else {
                started[0] = false;
            }
        });
    }

    private static final int ZOOM_PADDING = 16;

    /**
     * A step that only directs the video. It adds no settle pause, and leaves a pending
     * {@link #settleMs}/{@link #timeoutMs} to the next real step, so a test runs as if it were not there.
     */
    private ScenarioBuilder direction(String name, StepKind kind, long timeoutMs, Step.Body body) {
        var step = new Step(name, kind, body);
        step.settleMs = 0;
        step.timeoutMs = timeoutMs;
        step.group = currentGroup;
        steps.add(step);
        return this;
    }

    public ScenarioBuilder repeat(int times, Consumer<ScenarioBuilder> body) {
        for (int i = 0; i < times; i++) {
            body.accept(this);
        }
        return this;
    }

    /**
     * Cleanup that always runs — after success, after a failure, and after an abort. Use it to close
     * screens and restore the world so the next scenario starts from a known state.
     */
    public ScenarioBuilder teardown(String name, Consumer<TestContext> body) {
        var step = new Step(name, StepKind.TEARDOWN, body::accept);
        step.group = "teardown";
        teardownSteps.add(step);
        return this;
    }

    /** Server-thread cleanup. See {@link #teardown(String, Consumer)}. */
    public ScenarioBuilder teardownServer(String name, Consumer<ServerContext> body) {
        var step = new Step(name, StepKind.TEARDOWN, awaitServer("server teardown: " + name,
                ctx -> ctx.onServer(body), (ctx, ignored) -> {}));
        step.group = "teardown";
        teardownSteps.add(step);
        return this;
    }

    /** Overrides the settle delay for the next step only. */
    public ScenarioBuilder settleMs(long ms) {
        nextSettleMs = ms;
        return this;
    }

    /** Overrides the wait timeout for the next step only. */
    public ScenarioBuilder timeoutMs(long ms) {
        nextTimeoutMs = ms;
        return this;
    }

    public ScenarioBuilder log(String message) {
        return add("log: " + message, StepKind.CUSTOM, ctx -> ctx.log(message));
    }

    // endregion

    // region world / server

    public ScenarioBuilder setBlock(BlockPos pos, BlockState state) {
        return server("setBlock " + pos.toShortString(), sc -> sc.level().setBlockAndUpdate(pos, state));
    }

    public ScenarioBuilder setBlock(BlockPos pos, Block block) {
        return setBlock(pos, block.defaultBlockState());
    }

    public ScenarioBuilder fill(BlockPos from, BlockPos to, BlockState state) {
        return server("fill " + from.toShortString() + " -> " + to.toShortString(), sc -> {
            for (var pos : BlockPos.betweenClosed(from, to)) {
                sc.level().setBlockAndUpdate(pos.immutable(), state);
            }
        });
    }

    /** Clears a cube of blocks, so a scenario is not affected by whatever the previous one left. */
    public ScenarioBuilder clearArea(BlockPos center, int radius) {
        return server("clearArea " + center.toShortString() + " r" + radius, sc -> {
            for (var pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                    center.offset(radius, radius, radius))) {
                sc.level().setBlockAndUpdate(pos.immutable(), Blocks.AIR.defaultBlockState());
            }
        });
    }

    /**
     * Seeds a freshly placed block entity. Runs on the server thread after the block exists, which
     * is the only place its fields can be set safely.
     */
    public <T extends BlockEntity> ScenarioBuilder withBlockEntity(BlockPos pos, Class<T> type, Consumer<T> init) {
        return server("init " + type.getSimpleName() + " @ " + pos.toShortString(),
                sc -> init.accept(sc.blockEntity(pos, type)));
    }

    public ScenarioBuilder teleportPlayer(double x, double y, double z, float yaw, float pitch) {
        return server("teleport (%.1f %.1f %.1f)".formatted(x, y, z),
                sc -> sc.player().connection.teleport(x, y, z, yaw, pitch));
    }

    public ScenarioBuilder setGameMode(GameType gameType) {
        return server("gameMode " + gameType.getName(), sc -> sc.player().setGameMode(gameType));
    }

    public ScenarioBuilder giveItem(ItemStack stack) {
        return server("give " + stack, sc -> sc.player().getInventory().add(stack.copy()));
    }

    public ScenarioBuilder setHeldItem(ItemStack stack) {
        return server("hold " + stack, sc -> sc.player().setItemInHand(InteractionHand.MAIN_HAND, stack.copy()));
    }

    /**
     * Runs a command as the server console, at permission level 4.
     *
     * <p>Output is suppressed rather than broadcast: command feedback lands in chat, and chat is in
     * frame for every screenshot taken afterwards. The command itself is recorded as the step name.
     */
    public ScenarioBuilder runCommand(String command) {
        return server("/" + command, sc -> sc.server().getCommands().performPrefixedCommand(
                sc.server().createCommandSourceStack().withSuppressedOutput(), command));
    }

    /** Waits for the given number of server ticks to elapse. */
    public ScenarioBuilder serverTicks(int ticks) {
        var target = new long[]{-1};
        return add("serverTicks " + ticks, StepKind.WAIT, ctx -> {
            var server = ctx.requireServer();
            if (target[0] < 0) {
                target[0] = server.getTickCount() + ticks;
            }
            if (server.getTickCount() < target[0]) {
                ctx.repeat("server tick " + server.getTickCount() + "/" + target[0]);
            } else {
                target[0] = -1;
            }
        });
    }

    /** Waits until the client has actually received the chunk containing {@code pos}. */
    public ScenarioBuilder awaitClientChunk(BlockPos pos) {
        return add("awaitClientChunk " + pos.toShortString(), StepKind.WAIT, ctx -> {
            var level = ctx.level();
            if (level == null || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                ctx.repeat("client chunk at " + pos.toShortString());
            }
        });
    }

    /**
     * Waits until the client's copy of a block entity exists.
     *
     * <p>Placing a block on the server and immediately asserting on the client is the classic sync
     * race: the block-entity packet takes at least a tick to arrive.
     */
    public ScenarioBuilder awaitClientBlockEntity(BlockPos pos) {
        return add("awaitClientBlockEntity " + pos.toShortString(), StepKind.WAIT, ctx -> {
            if (ctx.clientBlockEntity(pos, BlockEntity.class) == null) {
                ctx.repeat("client block entity at " + pos.toShortString());
            }
        });
    }

    // endregion

    // region open / close UI

    /**
     * Opens a screen from the {@code ldlib2:screen_test} registry — the same path
     * {@code /ldlib2_screen_test <name>} takes.
     */
    public ScenarioBuilder openScreenTest(String name) {
        return add("openScreenTest " + name, StepKind.OPEN, ctx -> {
            var registry = LDLib2ClientRegistries.SCREEN_TESTS;
            if (registry == null) {
                throw new IllegalStateException("The ldlib2:screen_test registry is not available "
                        + "(it only exists in a development environment)");
            }
            var holder = registry.get(name);
            if (holder == null) {
                throw new IllegalStateException("No screen test registered as '" + name + "'. Known: "
                        + registry.values().stream().map(h -> h.annotation().name()).sorted().toList());
            }
            var ui = holder.value().get().createUI(ctx.requirePlayer());
            ctx.mc().gui.setScreen(new ModularUIScreen(ui, Component.empty()));
        });
    }

    /** Opens an arbitrary screen. The factory runs at step time, so it can read live state. */
    public ScenarioBuilder openScreen(String name, Function<TestContext, Screen> factory) {
        return add("openScreen " + name, StepKind.OPEN, ctx -> ctx.mc().gui.setScreen(factory.apply(ctx)));
    }

    /** Wraps a {@link ModularUI} in a {@code ModularUIScreen} and opens it. */
    public ScenarioBuilder openModularUI(String name, Function<TestContext, ModularUI> factory) {
        return add("openModularUI " + name, StepKind.OPEN,
                ctx -> ctx.mc().gui.setScreen(new ModularUIScreen(factory.apply(ctx), Component.empty())));
    }

    /**
     * Right-clicks a block through the real interaction path, without depending on a raycast
     * landing correctly.
     *
     * <p>Solo runs go through {@code ServerPlayerGameMode#useItemOn} on the integrated server: the
     * block's own {@code useWithoutItem} runs, opens its menu, and the server sends a genuine
     * open-screen packet. A multi-process client goes through {@code MultiPlayerGameMode#useItemOn}
     * instead, which sends the same {@code ServerboundUseItemOnPacket} a player's right click
     * produces — so the dedicated server's reach validation and menu-open path are exercised over
     * the actual wire.
     */
    public ScenarioBuilder useBlock(BlockPos pos) {
        var integrated = awaitServer("server task: useBlock " + pos.toShortString(),
                ctx -> ctx.onServer(sc -> {
                    var player = sc.player();
                    var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                    player.gameMode.useItemOn(player, sc.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                            InteractionHand.MAIN_HAND, hit);
                }), (ctx, ignored) -> {});
        return add("useBlock " + pos.toShortString(), StepKind.WORLD, ctx -> {
            if (ctx.isMultiProcess()) {
                var player = ctx.requirePlayer();
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                ctx.mc().gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
                return;
            }
            integrated.run(ctx);
        });
    }

    /** Waits until a screen of the given type is open. */
    public ScenarioBuilder awaitScreen(Class<? extends Screen> type) {
        return add("awaitScreen " + type.getSimpleName(), StepKind.WAIT, ctx -> {
            if (!type.isInstance(ctx.screen())) {
                ctx.repeat("screen " + type.getSimpleName() + " (current: "
                        + (ctx.screen() == null ? "none" : ctx.screen().getClass().getSimpleName()) + ")");
            }
        });
    }

    /** Waits until the open screen has a {@link ModularUI} behind it and its layout has been built. */
    public ScenarioBuilder awaitModularUI() {
        return add("awaitModularUI", StepKind.WAIT, ctx -> {
            var ui = ctx.ui();
            if (ui == null) {
                ctx.repeat("a ModularUI on the open screen");
                return;
            }
            // Width stays zero until the first layout pass, and every bound depends on it.
            if (ui.getWidth() <= 0 && ui.getHeight() <= 0) {
                ctx.repeat("the ModularUI to be laid out");
            }
        });
    }

    /** Waits until at least one element matches. */
    public ScenarioBuilder awaitElement(String selector) {
        return add("awaitElement " + selector, StepKind.WAIT, ctx -> {
            if (ctx.ui() == null || ctx.count(selector) == 0) {
                ctx.repeat("element " + selector);
            }
        });
    }

    public ScenarioBuilder closeScreen() {
        return add("closeScreen", StepKind.OPEN, ctx -> {
            var player = ctx.player();
            if (player != null) player.closeContainer();
            ctx.mc().gui.setScreen(null);
        });
    }

    // endregion

    // region input

    /**
     * Moves the cursor onto an element. Its own step, so the hover resolves before anything is
     * dispatched at that position. While a take records, the cursor travels there instead of jumping,
     * hovering whatever it crosses on the way.
     */
    public ScenarioBuilder hover(String selector) {
        return paced("hover " + selector, StepKind.INPUT, glide(aimAt(selector), Carry.MOVE));
    }

    public ScenarioBuilder hoverAt(float x, float y) {
        return paced("hoverAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT, glide(aimAt(x, y), Carry.MOVE));
    }

    /**
     * Moves the cursor to a point computed when the step runs, in GUI-scaled screen space: a spot on a
     * canvas, a point on a curve. Travels there while a take records, exactly like {@link #hover}.
     */
    public ScenarioBuilder hoverAt(String description, Function<TestContext, float[]> point) {
        return paced("hoverAt " + description, StepKind.INPUT, glide(ctx -> {
            var p = point.apply(ctx);
            return new Aim(p[0], p[1], 0, null);
        }, Carry.MOVE));
    }

    /**
     * Moves onto an element found when the step runs, for one no selector can name: a menu entry, a
     * list row picked by its text. Inside a menu, the cursor walks it the way a menu has to be walked.
     */
    public ScenarioBuilder hover(String description, Function<TestContext, UIElement> element) {
        return paced("hover " + description, StepKind.INPUT, glide(aimAt(element, null), Carry.MOVE));
    }

    /**
     * Clicks an element found when the steps run. Unlike {@link #click(String, int)} its position is
     * resolved once, when the cursor arrives, and the release goes there - so a click that closes or
     * removes what it clicked, such as a menu entry or a dropdown candidate, still completes.
     */
    public ScenarioBuilder click(String description, Function<TestContext, UIElement> element, int button) {
        var at = new float[2];
        paced("hover " + description, StepKind.INPUT, glide(aimAt(element, at), Carry.MOVE));
        paced("press " + description, StepKind.INPUT,
                rhythm(Pacing::beforePress, NONE, ctx -> ctx.input().mouseDown(at[0], at[1], button)));
        return paced("release " + description, StepKind.INPUT,
                rhythm(Pacing::pressHold, Pacing::afterRelease, ctx -> ctx.input().mouseUp(at[0], at[1], button)));
    }

    public ScenarioBuilder click(String selector) {
        return click(selector, Keys.MOUSE_LEFT);
    }

    public ScenarioBuilder rightClick(String selector) {
        return click(selector, Keys.MOUSE_RIGHT);
    }

    public ScenarioBuilder middleClick(String selector) {
        return click(selector, Keys.MOUSE_MIDDLE);
    }

    /** Hover, then press, then release — three steps, so each gets its own frame. */
    public ScenarioBuilder click(String selector, int button) {
        hover(selector);
        press(selector, button, Pacing::beforePress);
        return release(selector, button, Pacing::pressHold, Pacing::afterRelease);
    }

    private ScenarioBuilder press(String selector, int button, ToLongFunction<Pacing> before) {
        return paced("press " + selector, StepKind.INPUT, rhythm(before, NONE, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().mouseDown(target.centerX(), target.centerY(), button);
        }));
    }

    private ScenarioBuilder release(String selector, int button, ToLongFunction<Pacing> hold, ToLongFunction<Pacing> after) {
        return paced("release " + selector, StepKind.INPUT, rhythm(hold, after, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().mouseUp(target.centerX(), target.centerY(), button);
        }));
    }

    /**
     * Shift-click, with shift genuinely held rather than only declared.
     *
     * <p>Worth having as its own step because slot quick-move is the single most common
     * modifier-dependent interaction in a Minecraft UI, and it reads held key state to decide.
     */
    public ScenarioBuilder shiftClick(String selector) {
        keyDown(Keys.LEFT_SHIFT);
        click(selector);
        return keyUp(Keys.LEFT_SHIFT);
    }

    /**
     * Two clicks close enough together to register as a double click.
     *
     * <p>{@code ModularUI} uses the HTML5 threshold of 300 ms between the two, so the intermediate
     * settle has to stay well under it.
     */
    public ScenarioBuilder doubleClick(String selector) {
        // ModularUI measures release to release, so in a take the second click is timed from the first
        // release and let go within 220 ms of it, which holds down to 15 fps.
        var firstRelease = new long[1];
        hover(selector);
        press(selector, Keys.MOUSE_LEFT, Pacing::beforePress);
        paced("release " + selector, StepKind.INPUT, rhythm(Pacing::quickHold, NONE, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().mouseUp(target.centerX(), target.centerY(), Keys.MOUSE_LEFT);
            firstRelease[0] = Util.getNanos();
        }));
        settleMs(30);
        hover(selector);
        press(selector, Keys.MOUSE_LEFT, pacing -> sinceFirst(firstRelease, pacing.doubleClickGap()));
        return release(selector, Keys.MOUSE_LEFT,
                pacing -> Math.min(pacing.quickHold(), sinceFirst(firstRelease, 220)), Pacing::afterRelease);
    }

    /** Milliseconds from now until {@code millis} after the first release, never negative. */
    private static long sinceFirst(long[] firstRelease, long millis) {
        return Math.max(0, (firstRelease[0] + millis * 1_000_000L - Util.getNanos()) / 1_000_000L);
    }

    public ScenarioBuilder clickAt(float x, float y, int button) {
        hoverAt(x, y);
        paced("pressAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT,
                rhythm(Pacing::beforePress, NONE, ctx -> ctx.input().mouseDown(x, y, button)));
        return paced("releaseAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT,
                rhythm(Pacing::pressHold, Pacing::afterRelease, ctx -> ctx.input().mouseUp(x, y, button)));
    }

    /** {@link #clickAt(float, float, int)} at a point computed when the steps run, resolved once. */
    public ScenarioBuilder clickAt(String description, Function<TestContext, float[]> point, int button) {
        var at = new float[2];
        hoverAt(description, ctx -> {
            var p = point.apply(ctx);
            at[0] = p[0];
            at[1] = p[1];
            return p;
        });
        paced("pressAt " + description, StepKind.INPUT,
                rhythm(Pacing::beforePress, NONE, ctx -> ctx.input().mouseDown(at[0], at[1], button)));
        return paced("releaseAt " + description, StepKind.INPUT,
                rhythm(Pacing::pressHold, Pacing::afterRelease, ctx -> ctx.input().mouseUp(at[0], at[1], button)));
    }

    /** Scrolls over an element. While a take records, a whole-number amount turns the wheel one notch at a time. */
    public ScenarioBuilder scroll(String selector, double amount) {
        hover(selector);
        var at = new float[2];
        var sent = new int[1];
        var nextAt = new long[]{-1};
        return paced("scroll " + selector + " " + amount, StepKind.INPUT, ctx -> {
            var pacing = ctx.runner().pacing();
            if (pacing == null) {
                // a take that failed between notches: the rest are not worth replaying the whole amount for
                if (nextAt[0] >= 0) {
                    nextAt[0] = -1;
                    return;
                }
                var target = resolveClickable(ctx, selector);
                ctx.input().scroll(target.centerX(), target.centerY(), amount);
                return;
            }
            long now = Util.getNanos();
            if (nextAt[0] < 0) {
                var target = resolveClickable(ctx, selector);
                at[0] = target.centerX();
                at[1] = target.centerY();
                sent[0] = 0;
                nextAt[0] = now + pacing.beforePress() * 1_000_000L;
            }
            int notches = amount == Math.rint(amount) ? (int) Math.max(1, Math.abs(amount)) : 1;
            if (now >= nextAt[0] && sent[0] < notches) {
                ctx.input().scroll(at[0], at[1], amount / notches);
                sent[0]++;
                nextAt[0] = now + (sent[0] < notches ? pacing.scrollGap() : pacing.afterRelease()) * 1_000_000L;
            }
            if (sent[0] < notches || now < nextAt[0]) {
                ctx.repeat("scrolling");
            } else {
                nextAt[0] = -1;
            }
        });
    }

    /**
     * Drag and drop, expanded across frames.
     *
     * <p>A drag only begins when the source element sees {@code MOUSE_LEAVE} while a button is held,
     * and {@code MOUSE_LEAVE} is only emitted when the cached hover has already moved off the source.
     * So the sequence has to be: hover the source, press, nudge while still over it, then move away —
     * each in its own frame. Collapse any of that into one frame and the drag never starts, the drop
     * does nothing, and the test passes while exercising nothing at all.
     */
    public ScenarioBuilder drag(String fromSelector, String toSelector) {
        return dragInternal(fromSelector, ctx -> resolveClickable(ctx, fromSelector), ctx -> {
            var target = resolveClickable(ctx, toSelector);
            return new float[]{target.centerX(), target.centerY()};
        }, "drag " + fromSelector + " -> " + toSelector);
    }

    public ScenarioBuilder dragTo(String fromSelector, float x, float y) {
        return dragInternal(fromSelector, ctx -> resolveClickable(ctx, fromSelector), ctx -> new float[]{x, y},
                "drag " + fromSelector + " -> (%.0f, %.0f)".formatted(x, y));
    }

    /** Drags to a point computed when the drag starts, in GUI-scaled screen space. */
    public ScenarioBuilder dragTo(String fromSelector, String description, Function<TestContext, float[]> destination) {
        return dragInternal(fromSelector, ctx -> resolveClickable(ctx, fromSelector), destination,
                "drag " + fromSelector + " -> " + description);
    }

    /**
     * Drags an element found when the drag starts, by its centre, to a point computed then too: a node by
     * its title bar, a row by its handle.
     */
    public ScenarioBuilder drag(String description, Function<TestContext, UIElement> source,
                                Function<TestContext, float[]> destination) {
        return dragInternal(description, ctx -> ElementBounds.of(source.apply(ctx)), destination, "drag " + description);
    }

    private ScenarioBuilder dragInternal(String fromSelector, Function<TestContext, ElementBounds> sourceBounds,
                                         Function<TestContext, float[]> destination, String label) {
        var from = new float[3];
        var to = new float[2];

        var reachSource = glide(ctx -> new Aim(from[0], from[1], from[2], null), Carry.MOVE);
        paced(label + " :aim", StepKind.INPUT, ctx -> {
            if (ctx.firstAttempt()) {
                var source = sourceBounds.apply(ctx);
                from[0] = source.centerX();
                from[1] = source.centerY();
                from[2] = Math.min(source.width(), source.height());
                var target = destination.apply(ctx);
                to[0] = target[0];
                to[1] = target[1];
            }
            reachSource.run(ctx);
        });
        paced(label + " :press", StepKind.INPUT,
                rhythm(Pacing::beforePress, NONE, ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT)));
        // Still inside the source: this move is what makes it the remembered hover, so the leave
        // event fires against it later.
        paced(label + " :nudge", StepKind.INPUT,
                rhythm(pacing -> 60, NONE, ctx -> ctx.input().dragTo(from[0] + 1, from[1] + 1, Keys.MOUSE_LEFT)));
        var carry = glide(ctx -> new Aim(to[0], to[1], 0, null), Carry.DRAG);
        paced(label + " :leave", StepKind.INPUT, ctx -> {
            if (ctx.runner().pacing() != null) {
                carry.run(ctx);
                if (ctx.run().repeatRequested) return;
            } else {
                ctx.input().dragTo(Mth.lerp(0.35f, from[0], to[0]), Mth.lerp(0.35f, from[1], to[1]), Keys.MOUSE_LEFT);
            }
            var ui = ctx.ui();
            if (ui != null && !ui.getDragHandler().isDragging()) {
                ctx.log("drag did not start after leaving the source - does '" + fromSelector
                        + "' call startDrag() from a MOUSE_LEAVE listener guarded by isMouseDown(0)?");
            }
        });
        paced(label + " :move", StepKind.INPUT, ctx -> {
            if (ctx.runner().pacing() != null) {
                ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT);
            } else {
                ctx.input().dragTo(Mth.lerp(0.75f, from[0], to[0]), Mth.lerp(0.75f, from[1], to[1]), Keys.MOUSE_LEFT);
            }
        });
        paced(label + " :arrive", StepKind.INPUT, ctx -> ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT));
        // A jitter at the destination so the target sees a DRAG_UPDATE while hovered, not only the
        // enter that arrived with it.
        paced(label + " :settle", StepKind.INPUT, ctx -> ctx.input().dragTo(to[0] + 1, to[1], Keys.MOUSE_LEFT));
        return paced(label + " :drop", StepKind.INPUT,
                rhythm(Pacing::beforeDrop, Pacing::afterRelease, ctx -> ctx.input().mouseUp(to[0], to[1], Keys.MOUSE_LEFT)));
    }

    public ScenarioBuilder focus(String selector) {
        return add("focus " + selector, StepKind.INPUT,
                ctx -> ctx.requireUI().requestFocus(ctx.el(selector).element()));
    }

    public ScenarioBuilder blur() {
        return add("blur", StepKind.INPUT, ctx -> ctx.requireUI().clearFocus());
    }

    public ScenarioBuilder key(int keyCode) {
        return key(keyCode, 0);
    }

    /**
     * A full press and release.
     *
     * <p>Any modifiers are held down around it as real keys, not just passed along in the event's
     * modifier mask. {@code UIEvent#isCtrlDown()} and friends read held key state rather than the
     * mask, so a mask alone would make {@code key(GLFW_KEY_A, MOD_CONTROL)} behave like a bare A —
     * select-all would quietly do nothing.
     */
    public ScenarioBuilder key(int keyCode, int modifiers) {
        var label = Keys.describe(keyCode, modifiers);
        var modifierKeys = Keys.modifierKeysOf(modifiers);
        for (int i = 0; i < modifierKeys.size(); i++) {
            int modifierKey = modifierKeys.get(i);
            paced("keyDown " + Keys.nameOf(modifierKey), StepKind.INPUT, rhythm(i == 0 ? Pacing::beforeKey : Pacing::modifierLead,
                    NONE, ctx -> ctx.input().keyDown(modifierKey, 0)));
        }
        paced("keyDown " + label, StepKind.INPUT, rhythm(modifierKeys.isEmpty() ? Pacing::beforeKey : Pacing::modifierLead,
                NONE, ctx -> ctx.input().keyDown(keyCode, modifiers)));
        paced("keyUp " + label, StepKind.INPUT, rhythm(Pacing::keyHold, modifierKeys.isEmpty() ? Pacing::afterKey : NONE,
                ctx -> ctx.input().keyUp(keyCode, modifiers)));
        for (int i = modifierKeys.size() - 1; i >= 0; i--) {
            int modifierKey = modifierKeys.get(i);
            paced("keyUp " + Keys.nameOf(modifierKey), StepKind.INPUT, rhythm(pacing -> 30, i == 0 ? Pacing::afterKey : NONE,
                    ctx -> ctx.input().keyUp(modifierKey, 0)));
        }
        return this;
    }

    /** Presses and holds. Pair with {@link #keyUp(int)} — the runner releases anything left held. */
    public ScenarioBuilder keyDown(int keyCode) {
        return paced("keyDown " + Keys.nameOf(keyCode), StepKind.INPUT,
                rhythm(Pacing::beforeKey, NONE, ctx -> ctx.input().keyDown(keyCode, 0)));
    }

    public ScenarioBuilder keyUp(int keyCode) {
        return paced("keyUp " + Keys.nameOf(keyCode), StepKind.INPUT,
                rhythm(Pacing::keyHold, Pacing::afterKey, ctx -> ctx.input().keyUp(keyCode, 0)));
    }

    /** Types into whatever currently has focus, one character per step; at typing speed while a take records. */
    public ScenarioBuilder type(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char previous = i == 0 ? 0 : text.charAt(i - 1);
            boolean last = i == text.length() - 1;
            if (Keys.isPrintable(c)) {
                paced("type '" + c + "'", StepKind.INPUT, rhythm(pacing -> pacing.beforeChar(previous),
                        last ? Pacing::afterTyping : NONE, ctx -> ctx.input().charTyped(c, 0)));
            } else if (c == '\n') {
                key(Keys.ENTER);
            } else if (c == '\t') {
                key(Keys.TAB);
            }
        }
        return this;
    }

    /**
     * Focus, select all, delete, then type — the "replace this field's contents" gesture.
     *
     * <p>Select-all goes through ctrl+A rather than the {@code SELECT_ALL} command event so it
     * exercises the same key handling a user would.
     */
    public ScenarioBuilder typeInto(String selector, String text) {
        focus(selector);
        key(GLFW.GLFW_KEY_A, Keys.MOD_CONTROL);
        key(Keys.BACKSPACE);
        return type(text);
    }

    private ScenarioBuilder paced(String name, StepKind kind, Step.Body body) {
        add(name, kind, body);
        steps.getLast().paced = true;
        return this;
    }

    private static final ToLongFunction<Pacing> NONE = pacing -> 0;

    /** At test speed just {@code action}; while a take records, wait {@code before}, act, then wait {@code after}. */
    private static Step.Body rhythm(ToLongFunction<Pacing> before, ToLongFunction<Pacing> after, Step.Body action) {
        var acted = new boolean[1];
        var until = new long[]{-1};
        return ctx -> {
            var pacing = ctx.runner().pacing();
            if (pacing == null) {
                // the take stopped mid-step; acting again would repeat a gesture that already happened
                boolean done = acted[0];
                acted[0] = false;
                until[0] = -1;
                if (!done) action.run(ctx);
                return;
            }
            long now = Util.getNanos();
            if (until[0] < 0) until[0] = now + before.applyAsLong(pacing) * 1_000_000L;
            if (!acted[0]) {
                if (now < until[0]) {
                    ctx.repeat("pacing the gesture");
                    return;
                }
                acted[0] = true;
                until[0] = now + after.applyAsLong(pacing) * 1_000_000L;
                try {
                    action.run(ctx);
                } catch (Exception e) {
                    acted[0] = false;
                    until[0] = -1;
                    throw e;
                }
            }
            if (now < until[0]) {
                ctx.repeat("pacing the gesture");
                return;
            }
            acted[0] = false;
            until[0] = -1;
        };
    }

    private enum Carry {MOVE, DRAG}

    /** Where a move is headed, and what is there - the element decides whether a menu path is needed. */
    private record Aim(float x, float y, float size, UIElement element) {
    }

    private static Function<TestContext, Aim> aimAt(String selector) {
        return ctx -> {
            var ref = resolveClickableRef(ctx, selector);
            var bounds = ref.bounds();
            return new Aim(bounds.centerX(), bounds.centerY(), Math.min(bounds.width(), bounds.height()), ref.element());
        };
    }

    private static Function<TestContext, Aim> aimAt(float x, float y) {
        return ctx -> new Aim(x, y, 0, null);
    }

    /** @param resolved receives the point aimed at, when not {@code null} */
    private static Function<TestContext, Aim> aimAt(Function<TestContext, UIElement> element, float[] resolved) {
        return ctx -> {
            var target = element.apply(ctx);
            var bounds = ElementBounds.of(target);
            if (resolved != null) {
                resolved[0] = bounds.centerX();
                resolved[1] = bounds.centerY();
            }
            return new Aim(bounds.centerX(), bounds.centerY(), Math.min(bounds.width(), bounds.height()), target);
        };
    }

    private static final class Travel {
        private CursorPath path;
        private long startedAt;
        private int corrections;
    }

    /**
     * At test speed, puts the cursor on the aim in one move. While a take records, travels there along a
     * {@link CursorPath}, dispatching a move every frame, so whatever the pointer crosses really is
     * hovered on the way.
     */
    private static Step.Body glide(Function<TestContext, Aim> aim, Carry carry) {
        var travel = new Travel();
        var point = new float[2];
        return ctx -> {
            var pacing = ctx.runner().pacing();
            if (pacing == null) {
                var target = aim.apply(ctx);
                moveCursor(ctx, carry, target.x(), target.y());
                return;
            }
            if (travel.path == null) {
                travel.path = plan(ctx, aim.apply(ctx), carry, pacing);
                travel.startedAt = Util.getNanos();
                travel.corrections = 0;
            }
            var finished = travel.path.at((Util.getNanos() - travel.startedAt) / 1e9f, point);
            moveCursor(ctx, carry, point[0], point[1]);
            if (!finished) {
                ctx.repeat("cursor travelling");
                return;
            }
            // the target can move while the cursor travels: a list scrolling, a panel sliding in
            var target = aim.apply(ctx);
            if (Math.hypot(target.x() - point[0], target.y() - point[1]) > 1 && travel.corrections < 2) {
                travel.corrections++;
                travel.path = plan(ctx, target, carry, pacing);
                travel.startedAt = Util.getNanos();
                ctx.repeat("cursor correcting");
                return;
            }
            moveCursor(ctx, carry, target.x(), target.y());
            travel.path = null;
        };
    }

    private static CursorPath plan(TestContext ctx, Aim target, Carry carry, Pacing pacing) {
        float fromX = ctx.input().cursorX();
        float fromY = ctx.input().cursorY();
        float height = ctx.mc().getWindow().getGuiScaledHeight();
        boolean elbow = carry == Carry.MOVE && target.element() != null && sameMenu(ctx, fromX, fromY, target.element());
        return pacing.travel(fromX, fromY, target.x(), target.y(), target.size(), height, elbow, carry == Carry.DRAG ? 1.3f : 1f);
    }

    /** Whether the pointer is already inside the menu tree the target belongs to, so a diagonal would cross siblings. */
    private static boolean sameMenu(TestContext ctx, float x, float y, UIElement target) {
        var menu = outermostMenu(target);
        var ui = ctx.ui();
        if (menu == null || ui == null) return false;
        var under = ui.hitTestAtScreen(x, y);
        return under != null && outermostMenu(under) == menu;
    }

    private static UIElement outermostMenu(UIElement element) {
        UIElement menu = null;
        for (var current = element; current != null; current = current.getParent()) {
            if (current instanceof Menu<?, ?>) menu = current;
        }
        return menu;
    }

    private static void moveCursor(TestContext ctx, Carry carry, float x, float y) {
        if (carry == Carry.DRAG) {
            ctx.input().dragTo(x, y, Keys.MOUSE_LEFT);
        } else {
            ctx.input().moveTo(x, y);
        }
    }

    // endregion

    // region wait / sync

    /** Pumps {@code n} rendered frames. Prefer a condition wait; frame counts are not time. */
    public ScenarioBuilder frames(int n) {
        var remaining = new int[]{n};
        add("frames " + n, StepKind.WAIT, ctx -> {
            if (remaining[0]-- > 0) {
                ctx.repeat("frame " + (n - remaining[0]) + "/" + n);
            } else {
                remaining[0] = n;
            }
        });
        // a count of frames, not a time: several clients at once on one GPU can draw at a few frames a second
        steps.getLast().minTimeoutMs = n * FRAME_BUDGET_MS;
        return this;
    }

    /** What a frame may take before {@link #frames} gives up on it, over the scenario's default timeout. */
    private static final long FRAME_BUDGET_MS = 250;

    /**
     * Waits {@code n} client ticks.
     *
     * <p>The unit that matters for anything data-bound: {@code ModularUI#tick()} runs off the client
     * tick at 20 Hz, and that is what refreshes supplier-bound labels. Frame counts are meaningless
     * here because a dev world renders far faster than it ticks.
     */
    public ScenarioBuilder ticks(int n) {
        var target = new long[]{-1};
        return add("ticks " + n, StepKind.WAIT, ctx -> {
            long now = clientTick(ctx);
            if (target[0] < 0) target[0] = now + n;
            if (now < target[0]) {
                ctx.repeat("client tick " + now + "/" + target[0]);
            } else {
                target[0] = -1;
            }
        });
    }

    /** Waits {@code ms} milliseconds; inside a recorded take, milliseconds of video time. */
    public ScenarioBuilder waitMs(long ms) {
        return add("waitMs " + ms, StepKind.WAIT, waitBody(ms));
    }

    /**
     * Pauses for the viewer: waits {@code ms} milliseconds of video time inside a recording, and not at
     * all otherwise, so steps shared between a recording and a test cost the test nothing.
     *
     * <p>Use it for beats that only matter to someone watching - letting a result sit on screen,
     * a pause before the next gesture. Anything the UI itself needs time for (a transition finishing,
     * a server round trip) belongs in {@link #waitMs}, {@link #ticks} or {@link #waitUntil} instead,
     * which wait in both modes.
     *
     * @see #take(String, Consumer)
     */
    public ScenarioBuilder hold(long ms) {
        var wait = waitBody(ms);
        return add("hold " + ms, StepKind.WAIT, ctx -> {
            if (ctx.runner().isRecording()) wait.run(ctx);
        });
    }

    private static Step.Body waitBody(long ms) {
        var until = new long[]{-1};
        return ctx -> {
            long now = Util.getNanos();
            if (until[0] < 0) until[0] = now + ms * 1_000_000L;
            if (now < until[0]) {
                ctx.repeat(ms + "ms");
            } else {
                until[0] = -1;
            }
        };
    }

    public ScenarioBuilder waitUntil(String description, Predicate<TestContext> condition) {
        return add("waitUntil " + description, StepKind.WAIT, ctx -> {
            if (!condition.test(ctx)) ctx.repeat(description);
        });
    }

    /** Waits on authoritative server state. Runs the predicate on the server thread. */
    public ScenarioBuilder waitUntilServer(String description, Predicate<ServerContext> condition) {
        return add("waitUntilServer " + description, StepKind.WAIT,
                pollServer(description, ctx -> ctx.onServerGet(condition::test),
                        (ctx, satisfied) -> Boolean.TRUE.equals(satisfied) ? null : description));
    }

    /**
     * Waits until the client's view of a value matches the server's — the sync primitive.
     *
     * <p>Comparing both sides rather than polling the client alone is what distinguishes "the change
     * has not arrived yet" from "the server never made the change". The first is a wait; the second
     * is a bug, and a client-only assertion reports them identically.
     */
    public <T> ScenarioBuilder waitForSync(String description,
                                           Function<ServerContext, T> serverSide,
                                           Function<TestContext, T> clientSide) {
        return add("waitForSync " + description, StepKind.WAIT,
                pollServer(description, ctx -> ctx.onServerGet(serverSide::apply), (ctx, serverValue) -> {
                    var clientValue = clientSide.apply(ctx);
                    if (!java.util.Objects.equals(serverValue, clientValue)) {
                        // Both values in the message, so a timeout says which side is behind.
                        return description + " (server=" + serverValue + ", client=" + clientValue + ")";
                    }
                    ctx.log(description + " settled at " + serverValue);
                    return null;
                }));
    }

    public ScenarioBuilder waitForText(String selector, String expected) {
        return add("waitForText " + selector + " == " + expected, StepKind.WAIT, ctx -> {
            var found = ctx.elOpt(selector).map(ElementRef::text).orElse(null);
            if (!expected.equals(found)) {
                ctx.repeat(selector + " text (current: " + found + ")");
            }
        });
    }

    public ScenarioBuilder waitForTextContains(String selector, String part) {
        return add("waitForTextContains " + selector + " ~ " + part, StepKind.WAIT, ctx -> {
            var found = ctx.elOpt(selector).map(ElementRef::text).orElse(null);
            if (found == null || !found.contains(part)) {
                ctx.repeat(selector + " text (current: " + found + ")");
            }
        });
    }

    // endregion

    // region assertions

    public ScenarioBuilder check(String description, Predicate<TestContext> condition) {
        return add("check " + description, StepKind.ASSERT,
                ctx -> ctx.check(description, condition.test(ctx)));
    }

    public ScenarioBuilder checkServer(String description, Predicate<ServerContext> condition) {
        return serverStep("checkServer " + description, StepKind.ASSERT,
                sc -> sc.check(description, condition.test(sc)));
    }

    public <T> ScenarioBuilder checkEquals(String description, T expected, Function<TestContext, T> actual) {
        return add("checkEquals " + description, StepKind.ASSERT, ctx -> {
            var value = actual.apply(ctx);
            ctx.check(description, java.util.Objects.equals(expected, value), expected, value);
        });
    }

    public ScenarioBuilder checkExists(String selector) {
        return add("checkExists " + selector, StepKind.ASSERT,
                ctx -> ctx.check(selector + " exists", ctx.count(selector) > 0, ">0", ctx.count(selector)));
    }

    public ScenarioBuilder checkNotExists(String selector) {
        return add("checkNotExists " + selector, StepKind.ASSERT,
                ctx -> ctx.check(selector + " does not exist", ctx.count(selector) == 0, 0, ctx.count(selector)));
    }

    public ScenarioBuilder checkCount(String selector, int expected) {
        return add("checkCount " + selector + " == " + expected, StepKind.ASSERT, ctx -> {
            var actual = ctx.count(selector);
            ctx.check(selector + " count", actual == expected, expected, actual);
        });
    }

    public ScenarioBuilder checkText(String selector, String expected) {
        return add("checkText " + selector, StepKind.ASSERT, ctx -> {
            var actual = ctx.elOpt(selector).map(ElementRef::text).orElse(null);
            ctx.check(selector + " text", expected.equals(actual), expected, actual);
        });
    }

    public ScenarioBuilder checkTextContains(String selector, String part) {
        return add("checkTextContains " + selector, StepKind.ASSERT, ctx -> {
            var actual = ctx.elOpt(selector).map(ElementRef::text).orElse(null);
            ctx.check(selector + " text contains '" + part + "'",
                    actual != null && actual.contains(part), "contains " + part, actual);
        });
    }

    public ScenarioBuilder checkVisible(String selector) {
        return add("checkVisible " + selector, StepKind.ASSERT, ctx -> {
            var element = ctx.elOpt(selector);
            ctx.check(selector + " is visible", element.isPresent() && element.get().isVisible());
        });
    }

    public ScenarioBuilder checkHidden(String selector) {
        return add("checkHidden " + selector, StepKind.ASSERT, ctx -> {
            var element = ctx.elOpt(selector);
            ctx.check(selector + " is hidden", element.isEmpty() || !element.get().isVisible());
        });
    }

    public ScenarioBuilder checkFocused(String selector) {
        return add("checkFocused " + selector, StepKind.ASSERT, ctx -> {
            var element = ctx.elOpt(selector);
            ctx.check(selector + " is focused", element.isPresent() && element.get().isFocused());
        });
    }

    public ScenarioBuilder checkHovered(String selector) {
        return add("checkHovered " + selector, StepKind.ASSERT, ctx -> {
            var element = ctx.elOpt(selector);
            ctx.check(selector + " is hovered", element.isPresent() && element.get().isHovered());
        });
    }

    public ScenarioBuilder checkClass(String selector, String styleClass) {
        return add("checkClass " + selector + "." + styleClass, StepKind.ASSERT, ctx -> {
            var element = ctx.elOpt(selector);
            ctx.check(selector + " has class " + styleClass,
                    element.isPresent() && element.get().hasClass(styleClass),
                    styleClass, element.map(e -> String.join(" ", e.stateClasses())).orElse("<missing>"));
        });
    }

    public ScenarioBuilder checkBounds(String selector, Predicate<ElementBounds> condition) {
        return add("checkBounds " + selector, StepKind.ASSERT, ctx -> {
            var bounds = ctx.el(selector).bounds();
            ctx.check(selector + " bounds " + bounds, condition.test(bounds));
        });
    }

    public ScenarioBuilder checkScreen(Class<? extends Screen> type) {
        return add("checkScreen " + type.getSimpleName(), StepKind.ASSERT, ctx -> {
            var screen = ctx.screen();
            ctx.check("open screen is a " + type.getSimpleName(), type.isInstance(screen),
                    type.getSimpleName(), screen == null ? "none" : screen.getClass().getSimpleName());
        });
    }

    public ScenarioBuilder checkValue(String selector, Object expected) {
        return add("checkValue " + selector, StepKind.ASSERT, ctx -> {
            var actual = ctx.el(selector).value();
            ctx.check(selector + " value", java.util.Objects.equals(expected, actual), expected, actual);
        });
    }

    // endregion

    // region capture

    public ScenarioBuilder screenshot(String label) {
        return add("screenshot " + label, StepKind.CAPTURE, ctx -> ctx.screenshot(label));
    }

    public ScenarioBuilder screenshotElement(String label, String selector) {
        return add("screenshotElement " + label + " " + selector, StepKind.CAPTURE,
                ctx -> ctx.screenshotElement(label, ctx.el(selector)));
    }

    /**
     * Screenshots a UI drawn somewhere other than the game's frame — the off-screen target behind an
     * operating-system window. The surface is resolved at step time, because the window it belongs to
     * usually does not exist yet when the scenario is defined.
     *
     * @see TestContext#screenshotSurface
     */
    public ScenarioBuilder screenshotSurface(String label, Function<TestContext, UISurface> surface) {
        return add("screenshotSurface " + label, StepKind.CAPTURE,
                ctx -> ctx.screenshotSurface(label, surface.apply(ctx)));
    }

    // endregion

    /**
     * Resolves a selector to a rectangle and verifies the element can actually be clicked.
     *
     * <p>Hit-testing the centre and comparing catches the entire class of "the test clicked nothing
     * and passed anyway" bugs — an element scrolled out of its container, covered by a dialog, or
     * sized to zero all look fine to a plain selector lookup.
     */
    private static ElementBounds resolveClickable(TestContext ctx, String selector) {
        return resolveClickableRef(ctx, selector).bounds();
    }

    private static ElementRef resolveClickableRef(TestContext ctx, String selector) {
        var ref = ctx.el(selector);
        var bounds = ref.bounds();
        var report = ctx.stepReport();
        var target = new RunReport.TargetInfo();
        target.selector = selector;
        target.path = ref.path();
        target.x = bounds.x();
        target.y = bounds.y();
        target.width = bounds.width();
        target.height = bounds.height();

        if (bounds.isEmpty()) {
            report.target = target;
            throw new IllegalStateException("Target " + selector + " has zero size " + bounds
                    + " - it is probably not laid out yet; add awaitElement/awaitModularUI first");
        }
        var window = ctx.mc().getWindow();
        if (!bounds.isCenterOnScreen(window.getGuiScaledWidth(), window.getGuiScaledHeight())) {
            report.target = target;
            throw new IllegalStateException("Target " + selector + " is off screen at " + bounds
                    + " (viewport " + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight() + ")");
        }

        // A pure hit test, deliberately: probing with refreshHoveredElement would leave __hovered__ on
        // the target, so a later checkHovered could pass on the probe's own side effect rather than
        // on the interaction under test.
        var ui = ctx.requireUI();
        var hovered = ui.hitTestAtScreen(bounds.centerX(), bounds.centerY());
        var hit = hovered != null
                && (hovered == ref.element() || hovered.getStructurePath().contains(ref.element()));
        target.hitTestOk = hit;
        target.hitTestActual = hovered == null ? "none" : hovered.getElementName()
                + (hovered.getId().isEmpty() ? "" : "#" + hovered.getId());
        report.target = target;
        if (!hit) {
            throw new IllegalStateException("Target " + selector + " resolved to " + ref
                    + " but hit testing its centre " + bounds + " returned " + target.hitTestActual
                    + " - the element is occluded, clipped, or has hit testing disabled");
        }
        return ref;
    }

    private static long clientTick(TestContext ctx) {
        return Minecraft.getInstance() instanceof MinecraftAccessor accessor
                ? accessor.ldlib2$getClientTickCount()
                : 0L;
    }
}
