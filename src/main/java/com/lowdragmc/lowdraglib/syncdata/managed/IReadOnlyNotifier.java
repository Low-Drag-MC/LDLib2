package com.lowdragmc.lowdraglib.syncdata.managed;

public interface IReadOnlyNotifier {
    void addReadOnlyListener(Runnable listener);
}
