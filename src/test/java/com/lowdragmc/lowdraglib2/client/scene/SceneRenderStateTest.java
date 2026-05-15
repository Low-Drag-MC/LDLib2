package com.lowdragmc.lowdraglib2.client.scene;

import org.joml.Matrix3x2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SceneRenderStateTest {
    @Test
    void boundsUseTransformedScreenCoordinates() {
        var pose = new Matrix3x2f().translation(40.0f, 30.0f);

        var state = new SceneRenderState(
                null,
                10.0f, 20.0f, 100.0f, 50.0f,
                0, 0,
                pose,
                10, 20, 110, 70,
                1.0f,
                null
        );

        var bounds = state.bounds();

        assertNotNull(bounds);
        assertEquals(50, bounds.left());
        assertEquals(50, bounds.top());
        assertEquals(100, bounds.width());
        assertEquals(50, bounds.height());
    }
}
