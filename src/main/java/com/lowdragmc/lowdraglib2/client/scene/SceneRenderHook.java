package com.lowdragmc.lowdraglib2.client.scene;

@FunctionalInterface
public interface SceneRenderHook {
    void apply(SceneRenderContext ctx);
}
