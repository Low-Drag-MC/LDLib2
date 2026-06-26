package com.lowdragmc.lowdraglib2.async;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.serialization.MapCodec;
import lombok.Getter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

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
    private final CopyOnWriteArrayList<IAsyncLogic> asyncLogics = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService executorService;
    private final static ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setNameFormat(THREAD_NAME_FORMAT)
            .setDaemon(true)
            .build();
    private static final ThreadLocal<Boolean> IN_SERVICE = ThreadLocal.withInitial(() -> false);
    @Getter
    private long periodID = Long.MIN_VALUE;

    public void createExecutorService() {
        if (serverLevel == null) return;
        if (executorService != null && !executorService.isShutdown()) return;
        executorService = Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
        executorService.scheduleAtFixedRate(this::searchingTask, 0, DEFAULT_SCHEDULE_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * add a async logic runnable
     * @param logic runnable
     */
    public void addAsyncLogic(IAsyncLogic logic) {
        if (serverLevel == null) return;
        asyncLogics.add(logic);
        createExecutorService();
    }

    /**
     * remove logic runnable
     * @param logic runnable
     */
    public void removeAsyncLogic(IAsyncLogic logic) {
        if (serverLevel == null) return;
        asyncLogics.remove(logic);
        if (asyncLogics.isEmpty()) {
            releaseExecutorService();
        }
    }

    private void searchingTask() {
        try {
            if (serverLevel == null || serverLevel.get() == null) return;
            if (Platform.isServerNotSafe()) return;
            IN_SERVICE.set(true);
            for (IAsyncLogic logic : asyncLogics) {
                try {
                    logic.asyncTick(periodID);
                } catch (Throwable e) {
                    LDLib2.LOGGER.error("asyncThreadLogic error with an async logic {}", logic, e);
                }
            }
        } finally {
            IN_SERVICE.set(false);
        }
        periodID++;
    }

    public static boolean isThreadService() {
        return IN_SERVICE.get();
    }

    public void releaseExecutorService() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        executorService = null;
    }
}
