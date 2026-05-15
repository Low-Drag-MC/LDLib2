package com.lowdragmc.lowdraglib2.syncdata.accessor.maplike;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;

public interface IMapLikeAccessor<K, V, MAP> extends IAccessor<MAP> {
    IAccessor<K> getKeyAccessor();

    Class<K> getKeyType();

    IAccessor<V> getValueAccessor();

    Class<V> getValueType();

    @Override
    default boolean isReadOnly() {
        return true;
    }
}
