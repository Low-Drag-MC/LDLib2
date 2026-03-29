package com.lowdragmc.lowdraglib2.client.renderer;

/**
 * Stub for NeoForge TriState.
 */
public enum TriState {
    TRUE,
    FALSE,
    DEFAULT;

    public boolean get() {
        return this == TRUE;
    }
}
