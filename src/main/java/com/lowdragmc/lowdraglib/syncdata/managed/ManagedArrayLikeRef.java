package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.SyncUtils;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Collection;

public class ManagedArrayLikeRef extends DirectRef implements IArrayRef {
    private final IntSet dirty = new IntOpenHashSet();
    protected Object oldValue;
    protected int oldLength;
    protected final boolean isArray;

    public ManagedArrayLikeRef(IDirectVar<?> field) {
        super(field);
        isArray = field.getType().isArray();
        if (!isArray && !Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("Field %s is not an array or collection".formatted(field));
        }
        Object value = getField().value();
        if (value != null) {
            var array = SyncUtils.copyArrayLike(value, isArray);
            this.oldValue = array;
            this.oldLength = Array.getLength(array);
        }
    }

    @Override
    public void update() {
        Object newValue = getField().value();
        if ((oldValue == null && newValue != null) || (oldValue != null && newValue == null) || (oldValue != null && checkArrayLikeChanges(oldValue, newValue))) {
            if (newValue != null) {
                var array = SyncUtils.copyArrayLike(newValue, isArray);
                this.oldValue = array;
                this.oldLength = Array.getLength(array);
            } else {
                this.oldValue = null;
                this.oldLength = 0;
            }
            this.markAsDirty();
        }
    }

    protected boolean checkArrayLikeChanges(@NotNull Object oldValue, @NotNull Object newValue) {
        if (isArray) {
            if(Array.getLength(newValue) != oldLength) {
                this.markAsDirty();
                dirty.clear();
                return true;
            }
            var dirty = false;
            for (int i = 0; i < Array.getLength(oldValue); i++) {
                if (SyncUtils.isChanged(Array.get(oldValue, i), Array.get(newValue, i))) {
                    setChanged(i);
                    dirty = true;
                }
            }
            return dirty;
        }
        if (newValue instanceof Collection<?> collection) {
            if(collection.size() != oldLength) {
                this.markAsDirty();
                dirty.clear();
                return true;
            }
            int i = 0;
            var dirty = false;
            for (var item : collection) {
                var oldItem = Array.get(oldValue, i);
                if ((oldItem == null && item != null) || (oldItem != null && item == null) || (oldItem != null && SyncUtils.isChanged(oldItem, item))) {
                    setChanged(i);
                    dirty = true;
                }
                i++;
            }
            return dirty;
        }
        throw new IllegalArgumentException("Value %s is not an array or collection".formatted(newValue));
    }

    @Override
    public void markAsDirty() {
        super.markAsDirty();
        //TODO
//        if (!changed) {
//            dirty.clear();
//        }
    }

    @Override
    public void setChanged(int index) {
        markAsDirty();
        dirty.add(index);
    }

    @Override
    public IntSet getChanged() {
        return dirty;
    }
}

