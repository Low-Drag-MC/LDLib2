package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import org.appliedenergistics.yoga.YogaUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class YogaValueTests {

    @Test
    public void testYogaValuePoints() {
        var value = new YogaValueValue("100");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.POINT, result.unit);
        assertEquals(100f, result.value, 0.001f);
    }

    @Test
    public void testYogaValuePercent() {
        var value = new YogaValueValue("50%");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.PERCENT, result.unit);
        assertEquals(50f, result.value, 0.001f);
    }

    @Test
    public void testYogaValueAuto() {
        var value = new YogaValueValue("auto");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.AUTO, result.unit);
    }

    @Test
    public void testYogaValueUndefined() {
        var value = new YogaValueValue("undefined");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.UNDEFINED, result.unit);
    }

    @Test
    public void testYogaValueFitContent() {
        var value = new YogaValueValue("fit-content");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.FIT_CONTENT, result.unit);
    }

    @Test
    public void testYogaValueMaxContent() {
        var value = new YogaValueValue("max-content");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.MAX_CONTENT, result.unit);
    }

    @Test
    public void testYogaValueStretch() {
        var value = new YogaValueValue("stretch");
        var result = value.compute();
        assertNotNull(result);
        assertEquals(YogaUnit.STRETCH, result.unit);
    }

    @Test
    public void testYogaValueInvalid() {
        var value = new YogaValueValue("invalid");
        assertNull(value.compute());
        
        var emptyValue = new YogaValueValue("");
        assertNull(emptyValue.compute());
    }

    @Test
    public void testStyleLengthValue() {
        var autoValue = new StyleLengthValue("auto");
        assertNotNull(autoValue.compute());
        
        var pointValue = new StyleLengthValue("100");
        assertNotNull(pointValue.compute());
        
        var percentValue = new StyleLengthValue("50%");
        assertNotNull(percentValue.compute());
    }

    @Test
    public void testStyleSizeLengthValue() {
        var autoValue = new StyleSizeLengthValue("auto");
        assertNotNull(autoValue.compute());
        
        var fitContentValue = new StyleSizeLengthValue("fit-content");
        assertNotNull(fitContentValue.compute());
        
        var maxContentValue = new StyleSizeLengthValue("max-content");
        assertNotNull(maxContentValue.compute());
        
        var stretchValue = new StyleSizeLengthValue("stretch");
        assertNotNull(stretchValue.compute());
    }
}
