package com.lowdragmc.lowdraglib2.gui.sync.bindings;

public interface IDataBindingHolder<T> {
    record Binding<T> (IBinding<T> binding) implements IDataBindingHolder<T> {
        @Override
        public IBinding<T> getBinding() {
            return binding;
        }
        @Override
        public IDataSource<T> getData() {
            throw new UnsupportedOperationException();
        }
    }

    record Data<T> (IDataSource<T> data) implements IDataBindingHolder<T> {
        @Override
        public IBinding<T> getBinding() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IDataSource<T> getData() {
            return data;
        }
    }

    IBinding<T> getBinding();
    IDataSource<T> getData();
}
