package com.lowdragmc.lowdraglib.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;

public class LDLibExtraCodecs {
    public static PrimitiveCodec<Character> CHAR = new PrimitiveCodec<>() {
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

    public final static Codec<Number> NUMBER = Codec.either(Codec.LONG, Codec.DOUBLE)
            .xmap(
                    either -> either.map(i -> i, d -> (Number)d),
                    number -> {
                        if (number instanceof Integer || number instanceof Long || number instanceof Short || number instanceof Byte) {
                            return Either.left(number.longValue());
                        } else {
                            return Either.right(number.doubleValue());
                        }
                    }
            );
}
