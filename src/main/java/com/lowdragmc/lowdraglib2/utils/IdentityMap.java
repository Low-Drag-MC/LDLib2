package com.lowdragmc.lowdraglib2.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class IdentityMap<T> {
    private final AtomicInteger nextID = new AtomicInteger(0);
    @Getter
    private final Int2ObjectMap<T> idToSyncValue;
    @Getter
    private final Object2IntMap<T> syncValueToID;
    
    public IdentityMap() {
        this.idToSyncValue = new Int2ObjectOpenHashMap<>();
        this.syncValueToID = new Object2IntOpenHashMap<>();
        this.syncValueToID.defaultReturnValue(-1);
    }
    
    public synchronized int getID(T value) {
        return syncValueToID.getInt(value);
    }
    
    public synchronized T getValue(int id) {
        return idToSyncValue.get(id);
    }
    
    public synchronized void clear() {
        idToSyncValue.clear();
        syncValueToID.clear();
        nextID.set(0);
    }
    
    public synchronized void remove(T value) {
        int id = syncValueToID.removeInt(value);
        if (id != syncValueToID.defaultReturnValue()) {
            idToSyncValue.remove(id);
        }
    }
    
    public synchronized int add(T value) {
        int existingId = syncValueToID.getInt(value);
        if (existingId != syncValueToID.defaultReturnValue()) {
            return existingId;
        }
        
        int id = nextID.getAndIncrement();
        idToSyncValue.put(id, value);
        syncValueToID.put(value, id);
        return id;
    }
    
    public synchronized boolean contains(T value) {
        return syncValueToID.containsKey(value);
    }
    
    public synchronized boolean containsId(int id) {
        return idToSyncValue.containsKey(id);
    }
    
    public synchronized int size() {
        return syncValueToID.size();
    }
    
    public synchronized boolean isEmpty() {
        return syncValueToID.isEmpty();
    }

    public Collection<T> values() {
        return idToSyncValue.values();
    }
}