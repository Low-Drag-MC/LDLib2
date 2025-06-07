package com.lowdragmc.lowdraglib2.syncdata.var;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ArrayVar<TYPE, TYPE_ARRAY> implements IVar<TYPE> {
    protected final TYPE_ARRAY array;
    protected final int index;
    @Getter
    private final Class<TYPE> type;

    @SuppressWarnings("unchecked")
    public static <TYPE, TYPE_ARRAY> ArrayVar<TYPE, TYPE_ARRAY> of(TYPE_ARRAY array, int index, Class<TYPE> type) {
        if (type.isPrimitive()) {
            if (type == int.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new IntArrayVar((int[]) array, index);
            } else if (type == long.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new LongArrayVar((long[]) array, index);
            } else if (type == float.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new FloatArrayVar((float[]) array, index);
            } else if (type == double.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new DoubleArrayVar((double[]) array, index);
            } else if (type == boolean.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new BooleanArrayVar((boolean[]) array, index);
            } else if (type == byte.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new ByteArrayVar((byte[]) array, index);
            } else if (type == short.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new ShortArrayVar((short[]) array, index);
            } else if (type == char.class) {
                return (ArrayVar<TYPE, TYPE_ARRAY>) new CharArrayVar((char[]) array, index);
            }
        }
        return (ArrayVar<TYPE, TYPE_ARRAY>) new ObjectArrayVar<>((TYPE[]) array, index, type);

    }

    private final static class ObjectArrayVar<TYPE> extends ArrayVar<TYPE, TYPE[]> {
        private ObjectArrayVar(TYPE[] array, int index, Class<TYPE> type) {
            super(array, index, type);
        }

        @Override
        public TYPE value() {
            return array[index];
        }

        @Override
        public void set(TYPE value) {
            array[index] = value;
        }
    }

    private final static class IntArrayVar extends ArrayVar<Integer, int[]> {
        private IntArrayVar(int[] array, int index) {
            super(array, index, int.class);
        }

        @Override
        public Integer value() {
            return array[index];
        }

        @Override
        public void set(Integer value) {
            array[index] = value;
        }
    }

    private final static class LongArrayVar extends ArrayVar<Long, long[]> {
        private LongArrayVar(long[] array, int index) {
            super(array, index, long.class);
        }

        @Override
        public Long value() {
            return array[index];
        }

        @Override
        public void set(Long value) {
            array[index] = value;
        }
    }

    private final static class FloatArrayVar extends ArrayVar<Float, float[]> {
        private FloatArrayVar(float[] array, int index) {
            super(array, index, float.class);
        }

        @Override
        public Float value() {
            return array[index];
        }

        @Override
        public void set(Float value) {
            array[index] = value;
        }
    }

    private final static class DoubleArrayVar extends ArrayVar<Double, double[]> {
        private DoubleArrayVar(double[] array, int index) {
            super(array, index, double.class);
        }

        @Override
        public Double value() {
            return array[index];
        }

        @Override
        public void set(Double value) {
            array[index] = value;
        }
    }

    private final static class BooleanArrayVar extends ArrayVar<Boolean, boolean[]> {
        private BooleanArrayVar(boolean[] array, int index) {
            super(array, index, boolean.class);
        }

        @Override
        public Boolean value() {
            return array[index];
        }

        @Override
        public void set(Boolean value) {
            array[index] = value;
        }
    }

    private final static class ByteArrayVar extends ArrayVar<Byte, byte[]> {
        private ByteArrayVar(byte[] array, int index) {
            super(array, index, byte.class);
        }

        @Override
        public Byte value() {
            return array[index];
        }

        @Override
        public void set(Byte value) {
            array[index] = value;
        }
    }

    private final static class ShortArrayVar extends ArrayVar<Short, short[]> {
        private ShortArrayVar(short[] array, int index) {
            super(array, index, short.class);
        }

        @Override
        public Short value() {
            return array[index];
        }

        @Override
        public void set(Short value) {
            array[index] = value;
        }
    }

    private final static class CharArrayVar extends ArrayVar<Character, char[]> {
        private CharArrayVar(char[] array, int index) {
            super(array, index, char.class);
        }

        @Override
        public Character value() {
            return array[index];
        }

        @Override
        public void set(Character value) {
            array[index] = value;
        }
    }
}
