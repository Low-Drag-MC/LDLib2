package com.lowdragmc.lowdraglib2.gui.ui.bindings;

public interface IBindable<T> extends IObservable<T>, IDataSourceProvider<T> {
    default void bind(IBinding<T> binding) {
        bindObserver(binding);
        bindDataSource(binding);
    }
}
