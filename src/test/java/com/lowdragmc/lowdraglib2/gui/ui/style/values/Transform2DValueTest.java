package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Transform2DValueTest {

    @Test
    public void testTranslate() {
        var value = new Transform2DValue("translate(10, 20)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(10f, transform.translate().getX().getValue(), 0.001f);
        assertFalse(transform.translate().getX().isPercent());
        assertEquals(20f, transform.translate().getY().getValue(), 0.001f);
        assertFalse(transform.translate().getY().isPercent());
    }

    @Test
    public void testTranslateX() {
        var value = new Transform2DValue("translateX(15)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(15f, transform.translate().getX().getValue(), 0.001f);
        assertEquals(0f, transform.translate().getY().getValue(), 0.001f);
    }

    @Test
    public void testTranslateY() {
        var value = new Transform2DValue("translateY(25)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(0f, transform.translate().getX().getValue(), 0.001f);
        assertEquals(25f, transform.translate().getY().getValue(), 0.001f);
    }

    @Test
    public void testTranslatePercent() {
        var value = new Transform2DValue("translate(50%, 20px)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(50f, transform.translate().getX().getValue(), 0.001f);
        assertTrue(transform.translate().getX().isPercent());
        assertEquals(20f, transform.translate().getY().getValue(), 0.001f);
        assertFalse(transform.translate().getY().isPercent());
    }

    @Test
    public void testTranslateXPercent() {
        var value = new Transform2DValue("translateX(50%)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(50f, transform.translate().getX().getValue(), 0.001f);
        assertTrue(transform.translate().getX().isPercent());
    }

    @Test
    public void testTranslateFullPercent() {
        var value = new Transform2DValue("translate(100%, 100%)");
        var transform = value.compute();
        assertNotNull(transform);
        assertTrue(transform.translate().getX().isPercent());
        assertTrue(transform.translate().getY().isPercent());
        // Resolve against a 200x100 element
        assertEquals(200f, transform.translate().resolveX(200f), 0.001f);
        assertEquals(100f, transform.translate().resolveY(100f), 0.001f);
    }

    @Test
    public void testTranslatePxResolve() {
        var value = new Transform2DValue("translate(10, 20)");
        var transform = value.compute();
        assertNotNull(transform);
        // px values resolve to themselves regardless of dimension
        assertEquals(10f, transform.translate().resolveX(500f), 0.001f);
        assertEquals(20f, transform.translate().resolveY(500f), 0.001f);
    }

    @Test
    public void testScale() {
        var value = new Transform2DValue("scale(2)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(2f, transform.scale().x, 0.001f);
        assertEquals(2f, transform.scale().y, 0.001f);

        var nonUniformScale = new Transform2DValue("scale(2, 3)");
        var transform2 = nonUniformScale.compute();
        assertNotNull(transform2);
        assertEquals(2f, transform2.scale().x, 0.001f);
        assertEquals(3f, transform2.scale().y, 0.001f);
    }

    @Test
    public void testRotation() {
        var value = new Transform2DValue("rotate(45)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(45f, transform.rotation(), 0.001f);

        var degValue = new Transform2DValue("rotate(90deg)");
        var transform2 = degValue.compute();
        assertNotNull(transform2);
        assertEquals(90f, transform2.rotation(), 0.001f);
    }

    @Test
    public void testPivot() {
        var value = new Transform2DValue("pivot(50, 50)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(50f, transform.pivot().x, 0.001f);
        assertEquals(50f, transform.pivot().y, 0.001f);
    }

    @Test
    public void testMultipleTransforms() {
        var value = new Transform2DValue("translate(10, 20) rotate(45) scale(2)");
        var transform = value.compute();
        assertNotNull(transform);
        assertEquals(10f, transform.translate().getX().getValue(), 0.001f);
        assertEquals(20f, transform.translate().getY().getValue(), 0.001f);
        assertEquals(45f, transform.rotation(), 0.001f);
        assertEquals(2f, transform.scale().x, 0.001f);
        assertEquals(2f, transform.scale().y, 0.001f);
    }

    @Test
    public void testEmptyTransform() {
        var value = new Transform2DValue("");
        var transform = value.compute();
        assertNotNull(transform);
        assertTrue(transform.translate().isZero());
        assertEquals(0f, transform.rotation(), 0.001f);
        assertEquals(1f, transform.scale().x, 0.001f);
        assertEquals(1f, transform.scale().y, 0.001f);
        assertEquals(0.5f, transform.pivot().x, 0.001f);
        assertEquals(0.5f, transform.pivot().y, 0.001f);
    }
}
