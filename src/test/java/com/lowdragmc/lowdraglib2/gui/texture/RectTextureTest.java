package com.lowdragmc.lowdraglib2.gui.texture;

import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectTextureTest {
    @Test
    void usesPlainRectPathWhenAllEffectiveCornerRadiiAreZero() {
        var texture = new RectTexture().setRadius(new Vector4f(0, 0, 0, 0));

        assertTrue(texture.canUsePlainRectPath(100, 40));
    }

    @Test
    void skipsPlainRectPathWhenAnyEffectiveCornerRadiusIsPositive() {
        var texture = new RectTexture().setRadius(new Vector4f(0, 2, 0, 0));

        assertFalse(texture.canUsePlainRectPath(100, 40));
    }

    @Test
    void usesPlainRectPathWhenPositiveRadiiClampToZeroSizedRect() {
        var texture = new RectTexture().setRadius(new Vector4f(2, 2, 2, 2));

        assertTrue(texture.canUsePlainRectPath(0, 40));
        assertTrue(texture.canUsePlainRectPath(100, 0));
    }
}
