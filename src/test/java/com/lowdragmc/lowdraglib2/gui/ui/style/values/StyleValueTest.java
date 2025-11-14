package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StyleValueTest {

    @Test
    public void testStringValue() {
        var value = new StringValue("hello");
        assertEquals("hello", value.compute());
        
        var emptyValue = new StringValue("");
        assertEquals("", emptyValue.compute());
    }

    @Test
    public void testFloatValue() {
        var value = new FloatValue("3.14");
        assertEquals(3.14f, value.compute(), 0.001f);
        
        var negativeValue = new FloatValue("-1.5");
        assertEquals(-1.5f, negativeValue.compute(), 0.001f);
        
        var zeroValue = new FloatValue("0");
        assertEquals(0f, zeroValue.compute(), 0.001f);
    }

    @Test
    public void testFloatValueInvalidInput() {
        assertNull(new FloatValue("invalid").compute());
    }

    @Test
    public void testIntValue() {
        var value = new IntValue("42");
        assertEquals(42, value.compute());
        
        var negativeValue = new IntValue("-10");
        assertEquals(-10, negativeValue.compute());
        
        var zeroValue = new IntValue("0");
        assertEquals(0, zeroValue.compute());
    }

    @Test
    public void testIntValueInvalidInput() {
        assertNull(new IntValue("3.14").compute());
    }

    @Test
    public void testBoolValue() {
        var trueValue = new BoolValue("true");
        assertTrue(trueValue.compute());
        
        var falseValue = new BoolValue("false");
        assertFalse(falseValue.compute());
        
        // parseBoolean 对非 "true" 的值都返回 false
        var invalidValue = new BoolValue("invalid");
        assertFalse(invalidValue.compute());
    }

    @Test
    public void testDoubleValue() {
        var value = new DoubleValue("3.141592");
        assertEquals(3.141592, value.compute(), 0.000001);
        
        var negativeValue = new DoubleValue("-2.5");
        assertEquals(-2.5, negativeValue.compute(), 0.000001);
    }

    @Test
    public void testDoubleValueInvalidInput() {
        assertNull(new DoubleValue("invalid").compute());
    }

    @Test
    public void testEnumValue() {
        enum TestEnum {
            VALUE_ONE, VALUE_TWO, VALUE_THREE
        }
        
        var value = new EnumValue<>(TestEnum.class, "VALUE_ONE");
        assertEquals(TestEnum.VALUE_ONE, value.compute());
        
        // 测试忽略大小写
        var lowerCaseValue = new EnumValue<>(TestEnum.class, "value_two");
        assertEquals(TestEnum.VALUE_TWO, lowerCaseValue.compute());
        
        // 测试无效值
        var invalidValue = new EnumValue<>(TestEnum.class, "INVALID");
        assertNull(invalidValue.compute());
    }

    @Test
    public void testFloatOptionalValue() {
        var definedValue = new FloatOptionalValue("3.14");
        var result = definedValue.compute();
        assertNotNull(result);
        assertTrue(result.isDefined());
        assertEquals(3.14f, result.getValue(), 0.001f);
        
        var undefinedValue = new FloatOptionalValue("undefined");
        var undefinedResult = undefinedValue.compute();
        assertNotNull(undefinedResult);
        assertTrue(undefinedResult.isUndefined());
    }

    @Test
    public void testResourceLocationValue() {
        var validLocation = new ResourceLocationValue("minecraft:stone");
        assertNotNull(validLocation.compute());
        assertEquals("minecraft", validLocation.compute().getNamespace());
        assertEquals("stone", validLocation.compute().getPath());
        
        var invalidLocation = new ResourceLocationValue("invalid::location");
        assertNull(invalidLocation.compute());
    }

}
