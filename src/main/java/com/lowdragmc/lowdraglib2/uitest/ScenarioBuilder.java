package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.MinecraftAccessor;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.uitest.input.Keys;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
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
            ctx.mc().setScreen(new ModularUIScreen(ui, Component.empty()));
        });
    }

    /** Opens an arbitrary screen. The factory runs at step time, so it can read live state. */
    public ScenarioBuilder openScreen(String name, Function<TestContext, Screen> factory) {
        return add("openScreen " + name, StepKind.OPEN, ctx -> ctx.mc().setScreen(factory.apply(ctx)));
    }

    /** Wraps a {@link ModularUI} in a {@code ModularUIScreen} and opens it. */
    public ScenarioBuilder openModularUI(String name, Function<TestContext, ModularUI> factory) {
        return add("openModularUI " + name, StepKind.OPEN,
                ctx -> ctx.mc().setScreen(new ModularUIScreen(factory.apply(ctx), Component.empty())));
    }

    /**
     * Right-clicks a block <b>on the server</b>, through {@code ServerPlayerGameMode#useItemOn}.
     *
     * <p>This is the real path a player takes: the block's own {@code useWithoutItem} runs, opens
     * its menu, and the server sends a genuine open-screen packet the client turns into a
     * {@code ModularUIContainerScreen}. Nothing about the interaction is faked, so a machine UI test
     * exercises the same code as a real click — without depending on a raycast landing correctly.
     */
    public ScenarioBuilder useBlock(BlockPos pos) {
        return server("useBlock " + pos.toShortString(), sc -> {
            var player = sc.player();
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            player.gameMode.useItemOn(player, sc.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                    InteractionHand.MAIN_HAND, hit);
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
            ctx.mc().setScreen(null);
        });
    }

    // endregion

    // region input

    /**
     * Moves the cursor onto an element. Its own step, so the hover resolves before anything is
     * dispatched at that position.
     */
    public ScenarioBuilder hover(String selector) {
        return add("hover " + selector, StepKind.INPUT, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().moveTo(target.centerX(), target.centerY());
        });
    }

    public ScenarioBuilder hoverAt(float x, float y) {
        return add("hoverAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT, ctx -> ctx.input().moveTo(x, y));
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
        add("press " + selector, StepKind.INPUT, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().mouseDown(target.centerX(), target.centerY(), button);
        });
        add("release " + selector, StepKind.INPUT, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().mouseUp(target.centerX(), target.centerY(), button);
        });
        return this;
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
        click(selector);
        settleMs(30);
        click(selector);
        return this;
    }

    public ScenarioBuilder clickAt(float x, float y, int button) {
        hoverAt(x, y);
        add("pressAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT,
                ctx -> ctx.input().mouseDown(x, y, button));
        add("releaseAt (%.0f, %.0f)".formatted(x, y), StepKind.INPUT,
                ctx -> ctx.input().mouseUp(x, y, button));
        return this;
    }

    public ScenarioBuilder scroll(String selector, double amount) {
        hover(selector);
        return add("scroll " + selector + " " + amount, StepKind.INPUT, ctx -> {
            var target = resolveClickable(ctx, selector);
            ctx.input().scroll(target.centerX(), target.centerY(), amount);
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
        return dragInternal(fromSelector, ctx -> {
            var target = resolveClickable(ctx, toSelector);
            return new float[]{target.centerX(), target.centerY()};
        }, "drag " + fromSelector + " -> " + toSelector);
    }

    public ScenarioBuilder dragTo(String fromSelector, float x, float y) {
        return dragInternal(fromSelector, ctx -> new float[]{x, y},
                "drag " + fromSelector + " -> (%.0f, %.0f)".formatted(x, y));
    }

    private ScenarioBuilder dragInternal(String fromSelector, Function<TestContext, float[]> destination, String label) {
        var from = new float[2];
        var to = new float[2];

        add(label + " :aim", StepKind.INPUT, ctx -> {
            var source = resolveClickable(ctx, fromSelector);
            from[0] = source.centerX();
            from[1] = source.centerY();
            var target = destination.apply(ctx);
            to[0] = target[0];
            to[1] = target[1];
            ctx.input().moveTo(from[0], from[1]);
        });
        add(label + " :press", StepKind.INPUT, ctx -> ctx.input().mouseDown(from[0], from[1], Keys.MOUSE_LEFT));
        // Still inside the source: this move is what makes it the remembered hover, so the leave
        // event fires against it later.
        add(label + " :nudge", StepKind.INPUT, ctx -> ctx.input().dragTo(from[0] + 1, from[1] + 1, Keys.MOUSE_LEFT));
        add(label + " :leave", StepKind.INPUT, ctx -> {
            ctx.input().dragTo(Mth.lerp(0.35f, from[0], to[0]), Mth.lerp(0.35f, from[1], to[1]), Keys.MOUSE_LEFT);
            var ui = ctx.ui();
            if (ui != null && !ui.getDragHandler().isDragging()) {
                ctx.log("drag did not start after leaving the source - does '" + fromSelector
                        + "' call startDrag() from a MOUSE_LEAVE listener guarded by isMouseDown(0)?");
            }
        });
        add(label + " :move", StepKind.INPUT,
                ctx -> ctx.input().dragTo(Mth.lerp(0.75f, from[0], to[0]), Mth.lerp(0.75f, from[1], to[1]), Keys.MOUSE_LEFT));
        add(label + " :arrive", StepKind.INPUT, ctx -> ctx.input().dragTo(to[0], to[1], Keys.MOUSE_LEFT));
        // A jitter at the destination so the target sees a DRAG_UPDATE while hovered, not only the
        // enter that arrived with it.
        add(label + " :settle", StepKind.INPUT, ctx -> ctx.input().dragTo(to[0] + 1, to[1], Keys.MOUSE_LEFT));
        add(label + " :drop", StepKind.INPUT, ctx -> ctx.input().mouseUp(to[0], to[1], Keys.MOUSE_LEFT));
        return this;
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
        for (var modifierKey : modifierKeys) {
            keyDown(modifierKey);
        }
        add("keyDown " + label, StepKind.INPUT, ctx -> ctx.input().keyDown(keyCode, modifiers));
        add("keyUp " + label, StepKind.INPUT, ctx -> ctx.input().keyUp(keyCode, modifiers));
        for (int i = modifierKeys.size() - 1; i >= 0; i--) {
            keyUp(modifierKeys.get(i));
        }
        return this;
    }

    /** Presses and holds. Pair with {@link #keyUp(int)} — the runner releases anything left held. */
    public ScenarioBuilder keyDown(int keyCode) {
        return add("keyDown " + Keys.nameOf(keyCode), StepKind.INPUT, ctx -> ctx.input().keyDown(keyCode, 0));
    }

    public ScenarioBuilder keyUp(int keyCode) {
        return add("keyUp " + Keys.nameOf(keyCode), StepKind.INPUT, ctx -> ctx.input().keyUp(keyCode, 0));
    }

    /** Types into whatever currently has focus, one character per step. */
    public ScenarioBuilder type(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Keys.isPrintable(c)) {
                add("type '" + c + "'", StepKind.INPUT, ctx -> ctx.input().charTyped(c, 0));
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

    // endregion

    // region wait / sync

    /** Pumps {@code n} rendered frames. Prefer a condition wait; frame counts are not time. */
    public ScenarioBuilder frames(int n) {
        var remaining = new int[]{n};
        return add("frames " + n, StepKind.WAIT, ctx -> {
            if (remaining[0]-- > 0) {
                ctx.repeat("frame " + (n - remaining[0]) + "/" + n);
            } else {
                remaining[0] = n;
            }
        });
    }

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

    public ScenarioBuilder waitMs(long ms) {
        var until = new long[]{-1};
        return add("waitMs " + ms, StepKind.WAIT, ctx -> {
            long now = System.nanoTime();
            if (until[0] < 0) until[0] = now + ms * 1_000_000L;
            if (now < until[0]) {
                ctx.repeat(ms + "ms");
            } else {
                until[0] = -1;
            }
        });
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

    // endregion

    /**
     * Resolves a selector to a rectangle and verifies the element can actually be clicked.
     *
     * <p>Hit-testing the centre and comparing catches the entire class of "the test clicked nothing
     * and passed anyway" bugs — an element scrolled out of its container, covered by a dialog, or
     * sized to zero all look fine to a plain selector lookup.
     */
    private static ElementBounds resolveClickable(TestContext ctx, String selector) {
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
        return bounds;
    }

    private static long clientTick(TestContext ctx) {
        return Minecraft.getInstance() instanceof MinecraftAccessor accessor
                ? accessor.ldlib2$getClientTickCount()
                : 0L;
    }
}
