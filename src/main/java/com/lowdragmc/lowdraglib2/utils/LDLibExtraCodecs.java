package com.lowdragmc.lowdraglib2.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import lombok.experimental.UtilityClass;
import net.minecraft.Util;
import net.minecraft.nbt.*;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3i;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@UtilityClass
public final class LDLibExtraCodecs {
    public final static MapCodec ERROR_DECODER = MapCodec.of(Encoder.empty(), new MapDecoder.Implementation<>() {
        @Override
        public <T> DataResult<Object> decode(final DynamicOps<T> ops, final MapLike<T> input) {
            return DataResult.error(() -> "Error decoding");
        }

        @Override
        public <T> Stream<T> keys(final DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "ERROR_DECODER";
        }
    });

    public final static Codec<UUID> UUID = Codec.STRING.xmap(java.util.UUID::fromString, java.util.UUID::toString);

    public final static Codec<Tag> TAG = ExtraCodecs.converter(NbtOps.INSTANCE);

    public final static PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
        public <T> DataResult<Character> read(DynamicOps<T> ops, T input) {
            return ops.getNumberValue(input).map(number -> (char) number.intValue());
        }

        public <T> T write(DynamicOps<T> ops, Character value) {
            return ops.createInt(value);
        }

        public String toString() {
            return "Char";
        }
    };

    public final static Codec<Number> NUMBER = TAG.xmap(
            tag -> switch (tag) {
                case IntTag intTag -> intTag.getAsInt();
                case LongTag longTag -> longTag.getAsLong();
                case ByteTag byteTag -> byteTag.getAsByte();
                case ShortTag shortTag -> shortTag.getAsShort();
                case FloatTag floatTag -> floatTag.getAsFloat();
                case DoubleTag doubleTag -> doubleTag.getAsDouble();
                case null -> null;
                default -> throw new IllegalArgumentException("Invalid tag type: " + tag.getClass().getName());
            },
            number -> switch (number) {
                case Integer value -> IntTag.valueOf(value);
                case Long value -> LongTag.valueOf(value);
                case Byte value -> ByteTag.valueOf(value);
                case Short value -> ShortTag.valueOf(value);
                case Float value -> FloatTag.valueOf(value);
                case Double value -> DoubleTag.valueOf(value);
                case null -> null;
                default -> DoubleTag.valueOf(number.doubleValue());
            }
    );

    public static final Codec<Vector3i> VECTOR3I = Codec.INT
            .listOf()
            .comapFlatMap(
                    list -> Util.fixedSize(list, 3).map(l -> new Vector3i(l.get(0), l.get(1), l.get(2))),
                    vec3i -> List.of(vec3i.x, vec3i.y, vec3i.z)
            );

    public static final Codec<Vector2f> VECTOR2F = Codec.FLOAT
            .listOf()
            .comapFlatMap(
                    list -> Util.fixedSize(list, 2).map(l -> new Vector2f(l.get(0), l.get(1))),
                    vec2f -> List.of(vec2f.x, vec2f.y)
            );

    public static final Codec<Vector2i> VECTOR2I = Codec.INT
            .listOf()
            .comapFlatMap(
                    list -> Util.fixedSize(list, 2).map(l -> new Vector2i(l.get(0), l.get(1))),
                    vec2i -> List.of(vec2i.x, vec2i.y)
            );


    /**
     * This codec use an empty encoder and a decoder that always return an error
     */
    public static <T> MapCodec<T> errorDecoder() {
        return ERROR_DECODER;
    }

    public final static String NULL_STRING = "_NULL_";

    /**
     * Why we need it? Because Mojang doesn't support null in the nbt.
     *
     * @param ops
     * @return
     * @param <T>
     */
    public static <T> T createStringNull(DynamicOps<T> ops) {
        return ops.createString(NULL_STRING);
    }

    public static <T> boolean isEmptyOrStringNull(DynamicOps<T> ops, T payload) {
        return ops == ops.empty() || ops.getStringValue(payload).map(s -> s.equals(NULL_STRING)).result().orElse(false);
    }
}
