package com.lowdragmc.lowdraglib2.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.uitest.report.RunReport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
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
     * The single-player player. Scenarios run against an integrated server with exactly one player.
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
    @SuppressWarnings("unchecked")
    public <T> T getField(Object target, String fieldName) {
        try {
            return (T) findField(target.getClass(), fieldName).get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read field '" + fieldName + "' on "
                    + target.getClass().getName(), e);
        }
    }

    /** Writes a private field by name. See {@link #getField(Object, String)}. */
    public void setField(Object target, String fieldName, @Nullable Object value) {
        try {
            findField(target.getClass(), fieldName).set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot write field '" + fieldName + "' on "
                    + target.getClass().getName(), e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                var field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // keep walking up
            }
        }
        throw new NoSuchFieldException(fieldName + " not found on " + type.getName() + " or its supertypes");
    }
}
