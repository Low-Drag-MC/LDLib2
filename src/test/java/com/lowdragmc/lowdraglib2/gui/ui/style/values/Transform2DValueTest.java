package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class Transform2DValueTest {

    @Test
    public void testTranslate() {
        var value = new Transform2DValue("translate(10, 20)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(10f, transform.translate().x, 0.001f);
        assertEquals(20f, transform.translate().y, 0.001f);
    }

    @Test
    public void testTranslateX() {
        var value = new Transform2DValue("translateX(15)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(15f, transform.translate().x, 0.001f);
        assertEquals(0f, transform.translate().y, 0.001f);
    }

    @Test
    public void testTranslateY() {
        var value = new Transform2DValue("translateY(25)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(0f, transform.translate().x, 0.001f);
        assertEquals(25f, transform.translate().y, 0.001f);
    }

    @Test
    public void testScale() {
        var value = new Transform2DValue("scale(2)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(2f, transform.scale().x, 0.001f);
        assertEquals(2f, transform.scale().y, 0.001f);
        
        var nonUniformScale = new Transform2DValue("scale(2, 3)");
        var transform2 = nonUniformScale.compute();
        assertNotNull(transform2);
        // check transform2D properties
        assertEquals(2f, transform2.scale().x, 0.001f);
        assertEquals(3f, transform2.scale().y, 0.001f);
    }

    @Test
    public void testRotation() {
        var value = new Transform2DValue("rotate(45)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(45f, transform.rotation(), 0.001f);
        
        var degValue = new Transform2DValue("rotate(90deg)");
        var transform2 = degValue.compute();
        assertNotNull(transform2);
        // check transform2D properties
        assertEquals(90f, transform2.rotation(), 0.001f);
    }

    @Test
    public void testPivot() {
        var value = new Transform2DValue("pivot(50, 50)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(50f, transform.pivot().x, 0.001f);
        assertEquals(50f, transform.pivot().y, 0.001f);
    }

    @Test
    public void testMultipleTransforms() {
        var value = new Transform2DValue("translate(10, 20) rotate(45) scale(2)");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(10f, transform.translate().x, 0.001f);
        assertEquals(20f, transform.translate().y, 0.001f);
        assertEquals(45f, transform.rotation(), 0.001f);
        assertEquals(2f, transform.scale().x, 0.001f);
        assertEquals(2f, transform.scale().y, 0.001f);
    }

    @Test
    public void testEmptyTransform() {
        var value = new Transform2DValue("");
        var transform = value.compute();
        assertNotNull(transform);
        // check transform2D properties
        assertEquals(0f, transform.translate().x, 0.001f);
        assertEquals(0f, transform.translate().y, 0.001f);
        assertEquals(0f, transform.rotation(), 0.001f);
        assertEquals(1f, transform.scale().x, 0.001f);
        assertEquals(1f, transform.scale().y, 0.001f);
        assertEquals(0.5f, transform.pivot().x, 0.001f);
        assertEquals(0.5f, transform.pivot().y, 0.001f);
        assertEquals(0f, transform.rotation(), 0.001f);
    }
}
