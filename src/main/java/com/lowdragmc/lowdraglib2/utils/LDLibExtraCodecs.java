package com.lowdragmc.lowdraglib2.utils;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.PrimitiveCodec;
import lombok.experimental.UtilityClass;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Arrays;
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

    public static final Codec<ItemStack> ITEM_STACK = new Codec<>() {
        @Override
        public <T> DataResult<Pair<ItemStack, T>> decode(DynamicOps<T> ops, T input) {
            var result = ItemStack.OPTIONAL_CODEC.decode(ops, input);
            if (result.isSuccess()) return result;

            var realOp = Platform.getClientRegistryAccess().createSerializationContext(ops);
            result = ItemStack.OPTIONAL_CODEC.decode(realOp, input);
            if (result.isSuccess()) return result;

            realOp = Platform.getServerRegistryAccess().createSerializationContext(ops);
            return ItemStack.OPTIONAL_CODEC.decode(realOp, input);
        }

        @Override
        public <T> DataResult<T> encode(ItemStack input, DynamicOps<T> ops, T prefix) {
            var result = ItemStack.OPTIONAL_CODEC.encode(input, ops, prefix);
            if (result.isSuccess()) return result;

            var realOp = Platform.getClientRegistryAccess().createSerializationContext(ops);
            result = ItemStack.OPTIONAL_CODEC.encode(input, realOp, prefix);
            if (result.isSuccess()) return result;
            
            realOp = Platform.getServerRegistryAccess().createSerializationContext(ops);
            return ItemStack.OPTIONAL_CODEC.encode(input, realOp, prefix);
        }
    };


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

    /**
     * Checks if the provided payload is equivalent to the empty value in the given {@link DynamicOps} instance
     * or if it represents a string with the value of "null".
     *
     * @param <T>    the generic type representing the data structure being operated on within {@link DynamicOps}
     * @param ops    the {@link DynamicOps} instance to interpret the payload
     * @param payload the data payload to be checked
     * @return {@code true} if the payload is empty or evaluates to the string "null"; otherwise, {@code false}
     */
    public static <T> boolean isEmptyOrStringNull(DynamicOps<T> ops, T payload) {
        return ops == ops.empty() || ops.getStringValue(payload).map(s -> s.equals(NULL_STRING)).result().orElse(false);
    }

    /**
     * Combines multiple codecs into one, maintaining compatibility with outdated versions.
     * The method tries to decode values using the latest codec first and falls back to
     * the outdated codecs in the order they are provided.
     *
     * @param <T>      the type of the object being encoded/decoded
     * @param latest   the most recent and preferred {@link Codec} to use for encoding/decoding
     * @param outdated an array of older {@link Codec} versions to maintain backward compatibility
     * @return a {@link Codec} that combines the latest codec with the fallback compatibility of outdated codecs
     */
    @SafeVarargs
    public static <T> Codec<T> compat(Codec<T> latest, Codec<T>... outdated) {
        if (outdated.length < 1) {
            return latest;
        }
        if (outdated.length == 1) {
            return Codec.either(latest, outdated[0]).xmap(
                    Either::unwrap,
                    Either::left
            );
        }
        return Codec.either(latest, compat(outdated[0], Arrays.copyOfRange(outdated, 1, outdated.length))).xmap(
                Either::unwrap,
                Either::left
        );
    }

    /**
     * Creates a codec for a given {@link Enum} type, allowing serialization and deserialization
     * between the enum's name and its corresponding instance. If deserialization fails, a fallback
     * enum value is returned.
     *
     * @param <T>      the type of the enum
     * @param clazz    the class object of the enum type
     * @param fallback the fallback value to return in case of a deserialization failure
     * @return a {@link Codec} for the supplied enum type
     */
    public static <T extends Enum<T>> Codec<T> enumCodec(Class<T> clazz, T fallback) {
        return Codec.STRING.xmap(
                name -> {
                    try {
                        return Enum.valueOf(clazz, name);
                    } catch (Exception e) {
                        return fallback;
                    }
                },
                Enum::name
        );
    }
}
