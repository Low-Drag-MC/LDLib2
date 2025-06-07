package com.lowdragmc.lowdraglib2.syncdata.var;

import lombok.Getter;

import java.lang.reflect.Field;

@Getter
@SuppressWarnings("unchecked")
public sealed class FieldVar<TYPE> implements IVar<TYPE> {
    protected final Field field;
    protected final Class<TYPE> type;
    protected final Object instance;

    public static <T> IVar<T> of(Field field, Object instance) {
        var type = field.getType();
        if (type.isPrimitive()) {
            if (type == int.class) {
                return (IVar<T>) new IntFieldVar(field, instance);
            } else if (type == long.class) {
                return (IVar<T>) new LongFieldVar(field, instance);
            } else if (type == float.class) {
                return (IVar<T>) new FloatFieldVar(field, instance);
            } else if (type == double.class) {
                return (IVar<T>) new DoubleFieldVar(field, instance);
            } else if (type == boolean.class) {
                return (IVar<T>) new BooleanFieldVar(field, instance);
            } else if (type == byte.class) {
                return (IVar<T>) new ByteFieldVar(field, instance);
            } else if (type == short.class) {
                return (IVar<T>) new ShortFieldVar(field, instance);
            } else if (type == char.class) {
                return (IVar<T>) new CharFieldVar(field, instance);
            }
        }
        return new FieldVar<>(field, instance);
    }

    protected FieldVar(Field field, Object instance) {
        field.setAccessible(true);
        this.type = (Class<TYPE>) field.getType();
        this.field = field;
        this.instance = instance;
    }

    @Override
    public TYPE value() {
        try {
            return (TYPE) field.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void set(TYPE value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class IntFieldVar extends FieldVar<Integer> {
        private IntFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Integer value() {
            try {
                return field.getInt(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Integer value) {
            try {
                field.setInt(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class LongFieldVar extends FieldVar<Long> {
        private LongFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Long value() {
            try {
                return field.getLong(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Long value) {
            try {
                field.setLong(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class FloatFieldVar extends FieldVar<Float> {
        private FloatFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Float value() {
            try {
                return field.getFloat(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Float value) {
            try {
                field.setFloat(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class DoubleFieldVar extends FieldVar<Double> {
        private DoubleFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Double value() {
            try {
                return field.getDouble(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Double value) {
            try {
                field.setDouble(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class BooleanFieldVar extends FieldVar<Boolean> {
        private BooleanFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Boolean value() {
            try {
                return field.getBoolean(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Boolean value) {
            try {
                field.setBoolean(instance, value != null && value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class ByteFieldVar extends FieldVar<Byte> {
        private ByteFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Byte value() {
            try {
                return field.getByte(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Byte value) {
            try {
                field.setByte(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class ShortFieldVar extends FieldVar<Short> {
        private ShortFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Short value() {
            try {
                return field.getShort(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Short value) {
            try {
                field.setShort(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class CharFieldVar extends FieldVar<Character> {
        private CharFieldVar(Field field, Object instance) {
            super(field, instance);
        }

        @Override
        public Character value() {
            try {
                return field.getChar(instance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void set(Character value) {
            try {
                field.setChar(instance, value == null ? 0 : value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
