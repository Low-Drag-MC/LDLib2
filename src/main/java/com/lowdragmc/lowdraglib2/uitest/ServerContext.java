package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.uitest.mp.MPRoles;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The handle a scenario gets inside {@code server(..)}, {@code checkServer(..)} and
 * {@code waitUntilServer(..)}. Everything here runs on the <b>integrated server thread</b>, so it
 * sees authoritative state: the real {@link BlockEntity}, the real menu, the real player.
 *
 * <p>Having both sides available is what makes sync testable at all. A client-only harness can only
 * observe what the client happens to have been told, which means it cannot tell "the server never
 * changed" from "the server changed but the change never arrived" — the two failures that matter.
 */
public final class ServerContext {

    private final MinecraftServer server;
    private final Map<String, Object> state;
    private final RunReport.StepReport stepReport;

    ServerContext(MinecraftServer server, Map<String, Object> state, RunReport.StepReport stepReport) {
        this.server = server;
        this.state = state;
        this.stepReport = stepReport;
    }

    public MinecraftServer server() {
        return server;
    }

    /**
     * The single-player player. Solo scenarios run against an integrated server with exactly one
     * player; in a multi-process scenario prefer {@link #player(String)} to say which client.
     *
     * @throws IllegalStateException if no player is connected yet
     */
    public ServerPlayer player() {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            throw new IllegalStateException("No player on the server yet");
        }
        return players.getFirst();
    }

    /** Every connected player. In a multi-process run, one per client role. */
    public List<ServerPlayer> players() {
        return server.getPlayerList().getPlayers();
    }

    /**
     * The player behind a client role. In a multi-process run roles map to usernames via
     * {@link MPRoles#usernameFor}; in a solo run there is only one player and any role returns it,
     * so a segment body can be written once and used in both modes.
     */
    public ServerPlayer player(String role) {
        var players = server.getPlayerList().getPlayers();
        if (players.size() == 1) {
            return players.getFirst();
        }
        var username = MPRoles.usernameFor(role);
        for (var player : players) {
            if (player.getGameProfile().getName().equals(username)) {
                return player;
            }
        }
        throw new IllegalStateException("No player for role '" + role + "' (looked for username '"
                + username + "'; online: "
                + players.stream().map(p -> p.getGameProfile().getName()).toList() + ")");
    }

    public ServerLevel level() {
        return player().serverLevel();
    }

    /** The menu the player currently has open, which is the server half of a machine UI. */
    @Nullable
    public AbstractContainerMenu menu() {
        var players = server.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.getFirst().containerMenu;
    }

    /**
     * The server-side {@link ModularUI} behind the open menu, if there is one. Server-driven UIs
     * build a mirror instance here; assertions on it prove the server actually opened what the
     * client thinks it opened.
     */
    @Nullable
    public ModularUI ui() {
        return menu() instanceof IModularUIHolder holder && holder.hasModularUI() ? holder.getModularUI() : null;
    }

    /**
     * @throws IllegalStateException if there is no block entity of that type at the position — a
     *         clearer failure than a {@code null} that only blows up two lines later
     */
    public <T extends BlockEntity> T blockEntity(BlockPos pos, Class<T> type) {
        var blockEntity = level().getBlockEntity(pos);
        if (blockEntity == null) {
            throw new IllegalStateException("No block entity at " + pos + " on the server");
        }
        if (!type.isInstance(blockEntity)) {
            throw new IllegalStateException("Block entity at " + pos + " is a "
                    + blockEntity.getClass().getSimpleName() + ", not a " + type.getSimpleName());
        }
        return type.cast(blockEntity);
    }

    @Nullable
    public <T extends BlockEntity> T blockEntityOrNull(BlockPos pos, Class<T> type) {
        var blockEntity = level().getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    /** Scratch storage, shared with {@link TestContext} for the whole scenario. */
    public Map<String, Object> state() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) state.get(key);
    }

    public <T> T put(String key, T value) {
        state.put(key, value);
        return value;
    }

    /** Records an expectation. A failure marks the step failed; the scenario keeps going. */
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

    public void log(String message) {
        stepReport.log.add(message);
    }

    /**
     * Reads a private field by name.
     *
     * <p>A deliberate escape hatch. Tests frequently need to observe or seed state on a class the
     * test author does not own and cannot add an accessor to. Without this, the alternative is
     * either not testing it or polluting production classes with test-only getters.
     */
    public <T> T getField(Object target, String fieldName) {
        return FieldAccess.get(target, fieldName);
    }

    /** Writes a private field by name. See {@link #getField(Object, String)}. */
    public void setField(Object target, String fieldName, @Nullable Object value) {
        FieldAccess.set(target, fieldName, value);
    }
}
