package com.lowdragmc.lowdraglib2.syncdata.accessor.arraylike;


import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;

public interface IArrayLikeAccessor<TYPE, TYPE_ARRAY> extends IAccessor<TYPE_ARRAY> {
    /**
     * Get the accessor for the child elements of the array-like object
     * @return the accessor for the child elements
     */
    IAccessor<TYPE> getChildAccessor();

    Class<TYPE> getChildType();

    @Override
    default boolean isReadOnly() {
        return getChildAccessor().isReadOnly();
    }
}
