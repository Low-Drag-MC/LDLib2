package com.lowdragmc.lowdraglib.syncdata.var;

/**
 * @author KilaBash
 * @date 2023/2/21
 * @implNote IManagedArrayVar
 */
public interface IManagedArrayVar<T> extends IVar<T> {
    T value(int index);

    void set(int index, T value);

    Class<T> getChildrenType();

}
