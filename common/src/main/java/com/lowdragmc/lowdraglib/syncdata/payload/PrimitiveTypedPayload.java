package com.lowdragmc.lowdraglib.syncdata.payload;

import com.lowdragmc.lowdraglib.syncdata.SyncedFieldAccessors;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import javax.annotation.Nullable;

public abstract class PrimitiveTypedPayload<T> implements ITypedPayload<T> {

    @Override
    public byte getType() {
        return TypedPayloadRegistries.getId(this.getClass());
    }


    @Override
    @Deprecated
    public abstract T getPayload();

    public int getAsInt() {
        if (this instanceof IntPayload) {
            return ((IntPayload) this).value;
        }
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof ShortPayload) {
            return ((ShortPayload) this).value;
        }
        if (this instanceof LongPayload) {
            return (int) ((LongPayload) this).value;
        }
        if (this instanceof FloatPayload) {
            return (int) ((FloatPayload) this).value;
        }
        if (this instanceof DoublePayload) {
            return (int) ((DoublePayload) this).value;
        }
        throw new IllegalStateException("Cannot get int value from " + this.getClass().getSimpleName());
    }

    public long getAsLong() {
        if (this instanceof LongPayload) {
            return ((LongPayload) this).value;
        }
        if (this instanceof IntPayload) {
            return ((IntPayload) this).value;
        }
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof ShortPayload) {
            return ((ShortPayload) this).value;
        }
        if (this instanceof FloatPayload) {
            return (long) ((FloatPayload) this).value;
        }
        if (this instanceof DoublePayload) {
            return (long) ((DoublePayload) this).value;
        }
        throw new IllegalStateException("Cannot get long value from " + this.getClass().getSimpleName());
    }

    public float getAsFloat() {
        if (this instanceof FloatPayload) {
            return ((FloatPayload) this).value;
        }
        if (this instanceof IntPayload) {
            return ((IntPayload) this).value;
        }
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof ShortPayload) {
            return ((ShortPayload) this).value;
        }
        if (this instanceof LongPayload) {
            return ((LongPayload) this).value;
        }
        if (this instanceof DoublePayload) {
            return (float) ((DoublePayload) this).value;
        }
        throw new IllegalStateException("Cannot get float value from " + this.getClass().getSimpleName());
    }

    public double getAsDouble() {
        if (this instanceof DoublePayload) {
            return ((DoublePayload) this).value;
        }
        if (this instanceof IntPayload) {
            return ((IntPayload) this).value;
        }
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof ShortPayload) {
            return ((ShortPayload) this).value;
        }
        if (this instanceof LongPayload) {
            return ((LongPayload) this).value;
        }
        if (this instanceof FloatPayload) {
            return ((FloatPayload) this).value;
        }
        throw new IllegalStateException("Cannot get double value from " + this.getClass().getSimpleName());
    }

    public byte getAsByte() {
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof IntPayload) {
            return (byte) ((IntPayload) this).value;
        }
        if (this instanceof ShortPayload) {
            return (byte) ((ShortPayload) this).value;
        }
        if (this instanceof LongPayload) {
            return (byte) ((LongPayload) this).value;
        }
        if (this instanceof FloatPayload) {
            return (byte) ((FloatPayload) this).value;
        }
        if (this instanceof DoublePayload) {
            return (byte) ((DoublePayload) this).value;
        }
        throw new IllegalStateException("Cannot get byte value from " + this.getClass().getSimpleName());
    }

    public short getAsShort() {
        if (this instanceof ShortPayload) {
            return ((ShortPayload) this).value;
        }
        if (this instanceof IntPayload) {
            return (short) ((IntPayload) this).value;
        }
        if (this instanceof BytePayload) {
            return ((BytePayload) this).value;
        }
        if (this instanceof LongPayload) {
            return (short) ((LongPayload) this).value;
        }
        if (this instanceof FloatPayload) {
            return (short) ((FloatPayload) this).value;
        }
        if (this instanceof DoublePayload) {
            return (short) ((DoublePayload) this).value;
        }
        throw new IllegalStateException("Cannot get short value from " + this.getClass().getSimpleName());
    }

    public boolean getAsBoolean() {
        if (this instanceof BooleanPayload) {
            return ((BooleanPayload) this).value;
        }
        throw new IllegalStateException("Cannot get boolean value from " + this.getClass().getSimpleName());
    }

    public char getAsChar() {
        if (this instanceof CharPayload) {
            return ((CharPayload) this).value;
        }
        throw new IllegalStateException("Cannot get char value from " + this.getClass().getSimpleName());
    }

    public boolean isNull() {
        return this instanceof NullPayload;
    }

    public static void registerAll() {
        TypedPayloadRegistries.register(NullPayload.class, () -> NullPayload.INSTANCE, null);
        TypedPayloadRegistries.register(BooleanPayload.class, BooleanPayload::new, SyncedFieldAccessors.BOOLEAN_ACCESSOR);
        TypedPayloadRegistries.register(BytePayload.class, BytePayload::new, SyncedFieldAccessors.BYTE_ACCESSOR);
        TypedPayloadRegistries.register(ShortPayload.class, ShortPayload::new, SyncedFieldAccessors.SHORT_ACCESSOR);
        TypedPayloadRegistries.register(IntPayload.class, IntPayload::new, SyncedFieldAccessors.INT_ACCESSOR);
        TypedPayloadRegistries.register(LongPayload.class, LongPayload::new, SyncedFieldAccessors.LONG_ACCESSOR);
        TypedPayloadRegistries.register(FloatPayload.class, FloatPayload::new, SyncedFieldAccessors.FLOAT_ACCESSOR);
        TypedPayloadRegistries.register(DoublePayload.class, DoublePayload::new, SyncedFieldAccessors.DOUBLE_ACCESSOR);
        TypedPayloadRegistries.register(CharPayload.class, CharPayload::new, SyncedFieldAccessors.CHAR_ACCESSOR);

    }

    public static PrimitiveTypedPayload<Integer> ofInt(int value) {
        var result = new IntPayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Long> ofLong(long value) {
        var result = new LongPayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Float> ofFloat(float value) {
        var result = new FloatPayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Double> ofDouble(double value) {
        var result = new DoublePayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Byte> ofByte(byte value) {
        var result = new BytePayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Short> ofShort(short value) {
        var result = new ShortPayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Boolean> ofBoolean(boolean value) {
        var result = new BooleanPayload();
        result.value = value;
        return result;
    }

    public static PrimitiveTypedPayload<Character> ofChar(char value) {
        var result = new CharPayload();
        result.value = value;
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> PrimitiveTypedPayload<T> ofNull() {
        return (PrimitiveTypedPayload<T>) NullPayload.INSTANCE;
    }

    @Nullable
    public static PrimitiveTypedPayload<?> tryOfBoxed(Object object) {
        if (object == null) {
            return ofNull();
        }
        if (object instanceof Integer integer) {
            return ofInt(integer);
        }
        if (object instanceof Long aLong) {
            return ofLong(aLong);
        }
        if (object instanceof Float aFloat) {
            return ofFloat(aFloat);
        }
        if (object instanceof Double aDouble) {
            return ofDouble(aDouble);
        }
        if (object instanceof Byte aByte) {
            return ofByte(aByte);
        }
        if (object instanceof Short aShort) {
            return ofShort(aShort);
        }
        if (object instanceof Boolean aBoolean) {
            return ofBoolean(aBoolean);
        }
        if (object instanceof Character character) {
            return ofChar(character);
        }

        return null;
    }


    @Override
    public boolean isPrimitive() {
        return true;
    }


    public static class NullPayload extends PrimitiveTypedPayload<Object> {
        private static final NullPayload INSTANCE = new NullPayload();

        private NullPayload() {

        }

        @Override
        public void writePayload(FriendlyByteBuf buf) {
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
        }

        @Override
        public Tag serializeNBT() {
            return null;
        }

        @Override
        public void deserializeNBT(Tag tag) {
        }

        @Override
        public Object getPayload() {
            return null;
        }
    }

    public static class IntPayload extends PrimitiveTypedPayload<Integer> {
        private int value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeVarInt(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readVarInt();
        }

        @Override
        public Tag serializeNBT() {
            return IntTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsInt();
        }

        @Override
        public Integer getPayload() {
            return value;
        }
    }

    public static class LongPayload extends PrimitiveTypedPayload<Long> {
        private long value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeVarLong(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readVarLong();
        }

        @Override
        public Tag serializeNBT() {
            return LongTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsLong();
        }

        @Override
        public Long getPayload() {
            return value;
        }
    }

    public static class BooleanPayload extends PrimitiveTypedPayload<Boolean> {
        private boolean value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeBoolean(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readBoolean();
        }

        @Override
        public Tag serializeNBT() {
            return ByteTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsByte() != 0;
        }

        @Override
        public Boolean getPayload() {
            return value;
        }
    }

    public static class FloatPayload extends PrimitiveTypedPayload<Float> {
        private float value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeFloat(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readFloat();
        }

        @Override
        public Tag serializeNBT() {
            return FloatTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsFloat();
        }

        @Override
        public Float getPayload() {
            return value;
        }
    }

    public static class DoublePayload extends PrimitiveTypedPayload<Double> {
        private double value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeDouble(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readDouble();
        }

        @Override
        public Tag serializeNBT() {
            return DoubleTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsDouble();
        }

        @Override
        public Double getPayload() {
            return value;
        }
    }

    public static class BytePayload extends PrimitiveTypedPayload<Byte> {
        private byte value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeByte(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readByte();
        }

        @Override
        public Tag serializeNBT() {
            return ByteTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsByte();
        }

        @Override
        public Byte getPayload() {
            return value;
        }
    }

    public static class ShortPayload extends PrimitiveTypedPayload<Short> {
        private short value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeShort(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readShort();
        }

        @Override
        public Tag serializeNBT() {
            return ShortTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = ((NumericTag) tag).getAsShort();
        }

        @Override
        public Short getPayload() {
            return value;
        }
    }

    public static class CharPayload extends PrimitiveTypedPayload<Character> {
        private char value;

        @Override
        public void writePayload(FriendlyByteBuf buf) {
            buf.writeChar(value);
        }

        @Override
        public void readPayload(FriendlyByteBuf buf) {
            value = buf.readChar();
        }

        @Override
        public Tag serializeNBT() {
            return IntTag.valueOf(value);
        }

        @Override
        public void deserializeNBT(Tag tag) {
            value = (char) ((NumericTag) tag).getAsInt();
        }

        @Override
        public Character getPayload() {
            return value;
        }
    }
}
