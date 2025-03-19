package com.lowdragmc.lowdraglib.syncdata.accessor;


import com.lowdragmc.lowdraglib.syncdata.managed.IRef;

public interface IArrayLikeAccessor<Ref extends IRef> extends IAccessor<Ref> {
    /**
     * Get the accessor for the child elements of the array-like object
     * @return the accessor for the child elements
     */
    IAccessor<?> getChildAccessor();
}
