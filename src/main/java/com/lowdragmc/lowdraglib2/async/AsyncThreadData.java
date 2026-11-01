package com.lowdragmc.lowdraglib2.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/9/7
 * @implNote AsyncWorldSavedData
 * used for Async logic, it's world-related.
 * all logic runnable {@link IAsyncLogic} will be constantly executed in a async thread per tick.
 * warning, you have to add and remove runnable manually.
 */
@ParametersAreNonnullByDefault
public class AsyncThreadData extends SavedData {
    public static final SavedDataType<AsyncThreadData> TYPE = new SavedDataType<>(
            LDLib2.id("async_thread"),
            AsyncThreadData::new,
            serverLevel -> MapCodec.unitCodec(() -> new AsyncThreadData(serverLevel)),
            DataFixTypes.LEVEL
    );
    private static final String THREAD_NAME_FORMAT = "LDLib Async Thread-%d";
    private static final int DEFAULT_SCHEDULE_PERIOD_MS = 50;

    public final @Nullable WeakReference<ServerLevel> serverLevel;

    public static AsyncThreadData getOrCreate(ServerLevel serverLevel) {
        return serverLevel.getDataStorage().computeIfAbsent(TYPE);
    }

    private AsyncThreadData(@Nullable ServerLevel serverLevel) {
        this.serverLevel = new WeakReference<>(serverLevel);
    }

    // ********************************* async thread ********************************* //
    // how many times each logic was added, by identity
    private final Map<LogicKey, Integer> asyncLogics = new ConcurrentHashMap<>();
    private ScheduledExecutorService executorService;
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat(THREAD_NAME_FORMAT)
            .setDaemon(true)
            .build();
    private static final ThreadLocal<Map<Object, PeriodTask>> PERIOD_TASKS = new ThreadLocal<>();
    @Getter
    private long periodID = Long.MIN_VALUE;

    public void createExecutorService() {
        if (serverLevel == null) return;
        if (executorService != null && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        executorService.scheduleAtFixedRate(this::searchingTask, 0, DEFAULT_SCHEDULE_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    private record LogicKey(IAsyncLogic logic) {
        @Override
        public boolean equals(Object other) {
            return other instanceof LogicKey key && key.logic == logic;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(logic);
        }
    }

    /**
     * add a async logic runnable. A logic added n times runs n times per period, until it has been removed n times
     * @param logic runnable
     */
    public void addAsyncLogic(IAsyncLogic logic) {
        if (serverLevel == null) return;
        asyncLogics.merge(new LogicKey(logic), 1, Integer::sum);
        createExecutorService();
    }

    /**
     * remove logic runnable
     * @param logic runnable
     */
    public void removeAsyncLogic(IAsyncLogic logic) {
        if (serverLevel == null) return;
        asyncLogics.computeIfPresent(new LogicKey(logic), (key, count) -> count > 1 ? count - 1 : null);
        if (asyncLogics.isEmpty()) {
            releaseExecutorService();
        }
    }

    private void searchingTask() {
        var tasks = new LinkedHashMap<Object, PeriodTask>();
        try {
            if (serverLevel == null || serverLevel.get() == null) return;
            if (Platform.isServerNotSafe()) return;
            PERIOD_TASKS.set(tasks);
            asyncLogics.forEach((key, count) -> {
                for (int i = 0; i < count; i++) {
                    try {
                        key.logic.asyncTick(periodID);
                    } catch (Throwable e) {
                        LDLib2.LOGGER.error("asyncThreadLogic error with an async logic {}", key.logic, e);
                    }
                }
            });
        } finally {
            PERIOD_TASKS.remove();
            // the executor stops scheduling this task for good once it throws
            try {
                runOnServer(tasks.values());
            } catch (Throwable e) {
                LDLib2.LOGGER.error("Failed to hand the async period's work to the server", e);
            }
        }
        periodID++;
    }

    public static boolean isThreadService() {
        return PERIOD_TASKS.get() != null;
    }

    /**
     * Work an async period gathers for the server thread. Once the period ends, every task it registered through
     * {@link #periodTask} runs on the server thread, all in one scheduled task, in the order they were registered.
     */
    public interface PeriodTask {
        void run(MinecraftServer server);
    }

    /**
     * The task registered under {@code key} for the current async period, created on first use. Call it from
     * {@link IAsyncLogic#asyncTick} to gather work that should reach the server thread in one go when the period ends.
     *
     * @return {@code null} if the calling thread is not running an async period
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T extends PeriodTask> T periodTask(Object key, Supplier<T> factory) {
        var tasks = PERIOD_TASKS.get();
        return tasks == null ? null : (T) tasks.computeIfAbsent(key, k -> factory.get());
    }

    /**
     * Calls {@link BlockEntity#setChanged()} once on the server thread at the end of the current async period, however
     * many times the block entity asks during the period. Use it instead of scheduling {@code setChanged} directly from
     * {@link IAsyncLogic#asyncTick}.
     *
     * @return {@code false} if the calling thread is not running an async period; nothing is scheduled then, and the
     * caller has to call {@code setChanged} itself
     */
    public static boolean deferSetChanged(BlockEntity blockEntity) {
        var task = periodTask(ChangedBlockEntities.class, ChangedBlockEntities::new);
        if (task == null) return false;
        task.blockEntities.add(blockEntity);
        return true;
    }

    private static final class ChangedBlockEntities implements PeriodTask {
        private final Set<BlockEntity> blockEntities = new ReferenceOpenHashSet<>();

        @Override
        public void run(MinecraftServer server) {
            for (var blockEntity : blockEntities) {
                if (blockEntity.isRemoved()) continue;
                try {
                    blockEntity.setChanged();
                } catch (Throwable e) {
                    LDLib2.LOGGER.error("Failed to mark block entity {} changed", blockEntity, e);
                }
            }
        }
    }

    private void runOnServer(Collection<PeriodTask> tasks) {
        if (tasks.isEmpty()) return;
        var level = serverLevel == null ? null : serverLevel.get();
        if (level == null) return;
        var server = level.getServer();
        if (!Platform.serverSafe(server)) return;
        try {
            server.executeIfPossible(() -> {
                if (!Platform.serverSafe(server)) return;
                for (var task : tasks) {
                    try {
                        task.run(server);
                    } catch (Throwable e) {
                        LDLib2.LOGGER.error("Async period task {} failed", task, e);
                    }
                }
            });
        } catch (RejectedExecutionException ignored) {
            // The server can begin shutting down between the safety check and task submission.
        }
    }

    public void releaseExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = null;
    }
}
