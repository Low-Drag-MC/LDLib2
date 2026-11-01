package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;

/**
 * How the auto sync tells whether values of the classes it accepts changed, for values it would otherwise serialize to
 * compare (see {@link MarkStrategies}). Register one with {@link MarkStrategies#register}.
 * <br>
 * The marks must capture everything the values save and sync: anything they leave out changes without being synced.
 * Marks are taken and compared on the async sync thread.
 */
public interface IMarkStrategy<T, MARK> extends IMarkFunction<T, MARK> {
    /**
     * Whether this strategy captures what values of exactly this runtime class save and sync. Asked once per class, the
     * first strategy to accept a class is used for it.
     */
    boolean test(Class<?> type);
}
