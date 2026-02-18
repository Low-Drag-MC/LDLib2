package com.lowdragmc.lowdraglib2.utils;

import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;

import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

@UtilityClass
public final class TagBuilder {

    public static Compound compound() {
        return Compound.create();
    }

    public static Compound compound(CompoundTag initial) {
        return Compound.create(initial);
    }

    public static List list() {
        return List.create();
    }

    public static List list(ListTag initial) {
        return List.create(initial);
    }

    public static final class Compound {
        private final CompoundTag tag;

        private Compound(CompoundTag tag) {
            this.tag = tag;
        }

        public static Compound create() {
            return new Compound(new CompoundTag());
        }

        public static Compound create(CompoundTag initial) {
            return new Compound(initial);
        }

        public Compound remove(String key) {
            this.tag.remove(key);
            return this;
        }

        // Generic: any Tag
        public Compound add(String key, @Nullable Tag value) {
            if (value == null) return this;
            this.tag.put(key, value);
            return this;
        }

        // Common specific overloads (call generic internally)
        public Compound add(String key, @Nullable CompoundTag value) {
            if (value == null) return this;
            this.tag.put(key, value);
            return this;
        }

        public Compound add(String key, @Nullable ListTag value) {
            if (value == null) return this;
            this.tag.put(key, value);
            return this;
        }

        public Compound add(String key, @Nullable String value) {
            if (value == null) return this;
            this.tag.putString(key, value);
            return this;
        }

        public Compound add(String key, int value) {
            this.tag.putInt(key, value);
            return this;
        }

        public Compound add(String key, boolean value) {
            this.tag.putBoolean(key, value);
            return this;
        }

        public Compound add(String key, float value) {
            this.tag.putFloat(key, value);
            return this;
        }

        public Compound add(String key, double value) {
            this.tag.putDouble(key, value);
            return this;
        }

        public Compound add(String key, byte value) {
            this.tag.putByte(key, value);
            return this;
        }

        public Compound add(String key, short value) {
            this.tag.putShort(key, value);
            return this;
        }

        public Compound add(String key, long value) {
            this.tag.putLong(key, value);
            return this;
        }

        public Compound add(String key, byte[] value) {
            this.tag.putByteArray(key, value);
            return this;
        }

        public Compound add(String key, int[] value) {
            this.tag.putIntArray(key, value);
            return this;
        }

        public Compound add(String key, long[] value) {
            this.tag.putLongArray(key, value);
            return this;
        }

        public Compound add(String key, @Nullable UUID uuid) {
            if (uuid == null) return this;
            this.tag.putUUID(key, uuid);
            return this;
        }

        public Compound addCompound(String key, Consumer<Compound> builder) {
            CompoundTag nested = new CompoundTag();
            builder.accept(Compound.create(nested));
            this.tag.put(key, nested);
            return this;
        }

        public Compound addList(String key, Consumer<List> builder) {
            ListTag nested = new ListTag();
            builder.accept(List.create(nested));
            this.tag.put(key, nested);
            return this;
        }

        public CompoundTag build() {
            return tag;
        }
    }

    public static final class List {
        private final ListTag tag;

        private List(ListTag tag) {
            this.tag = tag;
        }

        public static List create() {
            return new List(new ListTag());
        }

        public static List create(ListTag initial) {
            return new List(initial);
        }

        // Generic: add Tag(s)
        public List add(Tag... tags) {
            Collections.addAll(this.tag, tags);
            return this;
        }

        public List add(Collection<? extends Tag> tags) {
            this.tag.addAll(tags);
            return this;
        }

        // Primitives
        public List addString(String value) {
            this.tag.add(StringTag.valueOf(value));
            return this;
        }

        public List addInt(int value) {
            this.tag.add(IntTag.valueOf(value));
            return this;
        }

        public List addLong(long value) {
            this.tag.add(LongTag.valueOf(value));
            return this;
        }

        public List addShort(short value) {
            this.tag.add(ShortTag.valueOf(value));
            return this;
        }

        public List addByte(byte value) {
            this.tag.add(ByteTag.valueOf(value));
            return this;
        }

        public List addFloat(float value) {
            this.tag.add(FloatTag.valueOf(value));
            return this;
        }

        public List addDouble(double value) {
            this.tag.add(DoubleTag.valueOf(value));
            return this;
        }

        public List addBoolean(boolean value) {
            this.tag.add(ByteTag.valueOf((byte) (value ? 1 : 0)));
            return this;
        }

        // Arrays (as array tags)
        public List addByteArray(byte[] values) {
            this.tag.add(new ByteArrayTag(values));
            return this;
        }

        public List addIntArray(int[] values) {
            this.tag.add(new IntArrayTag(values));
            return this;
        }

        public List addLongArray(long[] values) {
            this.tag.add(new LongArrayTag(values));
            return this;
        }

        public List addCompound(Consumer<Compound> builder) {
            CompoundTag nested = new CompoundTag();
            builder.accept(Compound.create(nested));
            this.tag.add(nested);
            return this;
        }

        public List addList(Consumer<List> builder) {
            ListTag nested = new ListTag();
            builder.accept(List.create(nested));
            this.tag.add(nested);
            return this;
        }

        public ListTag build() {
            return tag;
        }
    }
}