package com.lowdragmc.lowdraglib2.async;

/**
 * @author KilaBash
 * @date 2022/9/7
 * @implNote IAsyncLogic
 */
public interface IAsyncLogic {
    /**
     * runnable logic in a async thread.
     * @param periodID id of current period. added per tick.
     */
    void asyncTick(long periodID);
}
