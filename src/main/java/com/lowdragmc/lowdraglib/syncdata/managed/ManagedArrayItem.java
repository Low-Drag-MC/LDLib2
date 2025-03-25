package com.lowdragmc.lowdraglib.syncdata.managed;

import com.lowdragmc.lowdraglib.syncdata.var.IVar;

@SuppressWarnings("unchecked")
public class ManagedArrayItem<T> implements IVar<T> {
    protected Object array;
    protected Class<T> type;
    protected int index;

    @Override
    public T value() {
        return ((T[]) array)[index];
    }

    @Override
    public void set(T value) {
        ((T[]) array)[index] = value;
    }

    @Override
    public boolean isPrimitive() {
        return false;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    public ManagedArrayItem(Object array, int index) {
        this.array = array;
        this.type = (Class<T>) array.getClass().getComponentType();
        this.index = index;
    }


    public static <T> ManagedArrayItem<T> of(Object array, int index) {
        if(!array.getClass().isArray()) {
            throw new IllegalArgumentException("Not an array");
        }
        var type = array.getClass().getComponentType();
        if (type == int.class) {
            return (ManagedArrayItem<T>) new IntArrayItem(array, index);
        }
        if (type == long.class) {
            return (ManagedArrayItem<T>) new LongArrayItem(array, index);
        }
        if (type == float.class) {
            return (ManagedArrayItem<T>) new FloatArrayItem(array, index);
        }
        if (type == double.class) {
            return (ManagedArrayItem<T>) new DoubleArrayItem(array, index);
        }
        if (type == boolean.class) {
            return (ManagedArrayItem<T>) new BooleanArrayItem(array, index);
        }
        if (type == byte.class) {
            return (ManagedArrayItem<T>) new ByteArrayItem(array, index);
        }
        if (type == char.class) {
            return (ManagedArrayItem<T>) new CharArrayItem(array, index);
        }
        if (type == short.class) {
            return (ManagedArrayItem<T>) new ShortArrayItem(array, index);
        }
        return new ManagedArrayItem<>(array, index);
    }

    static class IntArrayItem extends ManagedArrayItem<Integer> implements Int {

        public IntArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public Integer value() {
            return intValue();
        }

        @Override
        public void set(Integer value) {
            setInt(value);
        }

        @Override
        public void setInt(int value) {
            ((int[]) array)[index] = value;
        }

        @Override
        public int intValue() {
            return ((int[])array)[index];
        }
    }

    static class LongArrayItem extends ManagedArrayItem<java.lang.Long> implements Long {

        public LongArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Long value() {
            return longValue();
        }

        @Override
        public void set(java.lang.Long value) {
            setLong(value);
        }

        @Override
        public void setLong(long value) {
            ((long[]) array)[index] = value;
        }

        @Override
        public long longValue() {
            return ((long[])array)[index];
        }
    }

    static class FloatArrayItem extends ManagedArrayItem<java.lang.Float> implements Float {

        public FloatArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Float value() {
            return floatValue();
        }

        @Override
        public void set(java.lang.Float value) {
            setFloat(value);
        }

        @Override
        public void setFloat(float value) {
            ((float[]) array)[index] = value;
        }

        @Override
        public float floatValue() {
            return ((float[])array)[index];
        }
    }

    static class ByteArrayItem extends ManagedArrayItem<java.lang.Byte> implements Byte {

        public ByteArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Byte value() {
            return byteValue();
        }

        @Override
        public void set(java.lang.Byte value) {
            setByte(value);
        }

        @Override
        public void setByte(byte value) {
            ((byte[]) array)[index] = value;
        }

        @Override
        public byte byteValue() {
            return ((byte[])array)[index];
        }
    }

    static class DoubleArrayItem extends ManagedArrayItem<java.lang.Double> implements Double {

        public DoubleArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Double value() {
            return doubleValue();
        }

        @Override
        public void set(java.lang.Double value) {
            setDouble(value);
        }

        @Override
        public void setDouble(double value) {
            ((double[]) array)[index] = value;
        }

        @Override
        public double doubleValue() {
            return ((double[])array)[index];
        }
    }

    static class BooleanArrayItem extends ManagedArrayItem<java.lang.Boolean> implements Boolean {

        public BooleanArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Boolean value() {
            return booleanValue();
        }

        @Override
        public void set(java.lang.Boolean value) {
            setBoolean(value);
        }

        @Override
        public void setBoolean(boolean value) {
            ((boolean[]) array)[index] = value;
        }

        @Override
        public boolean booleanValue() {
            return ((boolean[])array)[index];
        }
    }

    static class ShortArrayItem extends ManagedArrayItem<java.lang.Short> implements Short {

        public ShortArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public java.lang.Short value() {
            return shortValue();
        }

        @Override
        public void set(java.lang.Short value) {
            setShort(value);
        }

        @Override
        public void setShort(short value) {
            ((short[]) array)[index] = value;
        }

        @Override
        public short shortValue() {
            return ((short[])array)[index];
        }
    }

    static class CharArrayItem extends ManagedArrayItem<Character> implements Char {

        public CharArrayItem(Object array, int index) {
            super(array, index);
        }

        @Override
        public Character value() {
            return charValue();
        }

        @Override
        public void set(Character value) {
            setChar(value);
        }

        @Override
        public void setChar(char value) {
            ((char[]) array)[index] = value;
        }

        @Override
        public char charValue() {
            return ((char[])array)[index];
        }
    }
}
