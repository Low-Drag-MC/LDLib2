package com.lowdragmc.lowdraglib2.syncdata;

/**
 * @author KilaBash
 * @date 2023/2/17
 * @implNote Subscription
 * @port ELB_GG 
 * @date_port 2026/03/29 
 * @port_to fabric
 */
@FunctionalInterface
public interface ISubscription {
    void unsubscribe();

    default ISubscription andThen(ISubscription other) {
        return () -> {
            unsubscribe();
            other.unsubscribe();
        };
    }
}
