package com.lowdragmc.lowdraglib2.gui.bindings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IBindable<T> extends IObservable<T>, IDataSourceProvider<T> {
    default void bind(@Nullable IBinding<T> binding) {
        if (binding == null) return;
        bindObserver(binding);
        bindDataSource(binding);
    }

    default void unbind(@Nonnull IBinding<T> binding) {
        unbindObserver(binding);
        unbindDataSource(binding);
    }
}
