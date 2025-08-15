package com.lowdragmc.lowdraglib2.gui.bindings;

public interface IDataBindingHolder<T> {
    IBinding<T> getBinding();
    void setBinding(IBinding<T> binding);
    IData<T> getData();
}
