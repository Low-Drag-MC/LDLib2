package com.lowdragmc.lowdraglib.syncdata.managed;

/**
 * @author KilaBash
 * @date 2023/2/21
 * @implNote IManagedArrayVar
 */
public interface IManagedArrayVar<T> extends IDirectVar<T> {
    T value(int index);

    void set(int index, T value);

    Class<T> getChildrenType();

}
