package com.lowdragmc.lowdraglib2.syncdata.mark;

import com.lowdragmc.lowdraglib2.syncdata.IContentVersion;
import org.jetbrains.annotations.NotNull;

final class ContentVersionStrategy implements IMarkStrategy<IContentVersion, Long> {
    @Override
    public boolean test(Class<?> type) {
        return IContentVersion.class.isAssignableFrom(type);
    }

    @Override
    public @NotNull Long obtainManagedMark(@NotNull IContentVersion value) {
        return value.getContentVersion();
    }

    @Override
    public boolean areDifferent(@NotNull Long managedMark, @NotNull IContentVersion value) {
        return managedMark != value.getContentVersion();
    }

    @Override
    public Long updateMark(@NotNull Long managedMark, @NotNull IContentVersion value) {
        long version = value.getContentVersion();
        return managedMark == version ? null : version;
    }
}
