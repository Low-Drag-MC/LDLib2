package com.lowdragmc.lowdraglib2.uitest;

/**
 * What the runner does with the rest of a scenario after a step throws.
 *
 * <p>Note this is about <em>exceptions</em>, not failed assertions: a failed
 * {@link TestContext#check(String, boolean)} marks the step failed and always continues, because a
 * scenario that keeps going usually produces several useful failures instead of one.
 */
public enum ErrorPolicy {
    /**
     * Skip the scenario's remaining steps. Its teardown still runs, and the next scenario still
     * starts. The default: once a step has thrown, later steps are usually operating on a UI that
     * is not in the state they assume, so their failures are noise.
     */
    ABORT_SCENARIO,
    /**
     * Run every remaining step regardless. Useful for a scenario that is really a batch of
     * independent probes rather than one flow.
     */
    CONTINUE
}
