package com.lowdragmc.lowdraglib.syncdata.accessor;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

@Getter
public class CustomDirectAccessor<TYPE> implements IDirectAccessor<IDirectVar<TYPE>>, IMarkFunction {
    private final Class<TYPE> type;
    private final boolean supportChildClass;
    private final Codec<TYPE> codec;
    private final StreamCodec<? super RegistryFriendlyByteBuf, TYPE> streamCodec;
    @Nullable
    private final IMarkFunction markFunction;

    protected CustomDirectAccessor(Class<TYPE> type, boolean supportChildClass,
                                   Codec<TYPE> codec, StreamCodec<? super RegistryFriendlyByteBuf, TYPE> streamCodec,
                                   @Nullable IMarkFunction<TYPE, ?> markFunction) {
        this.type = type;
        this.supportChildClass = supportChildClass;
        this.codec = codec;
        this.streamCodec = streamCodec;
        this.markFunction = markFunction;
    }

    public static <TYPE> Builder<TYPE> builder(Class<TYPE> type, boolean supportChildClass) {
        return new Builder<>(type, supportChildClass);
    }

    @Override
    public boolean test(Class<?> type) {
        return supportChildClass ? this.type.isAssignableFrom(type) : this.type == type;
    }

    @Override
    public <T> T readDirectVar(DynamicOps<T> op, IDirectVar<TYPE> var) {
        return codec.encodeStart(op, var.value()).getOrThrow();
    }

    @Override
    public <T> void writeDirectVar(DynamicOps<T> op, IDirectVar<TYPE> var, T payload) {
        var.set(codec.parse(op, payload).getOrThrow());
    }

    @Override
    public void readDirectVarToStream(RegistryFriendlyByteBuf buffer, IDirectVar<TYPE> var) {
        streamCodec.encode(buffer, var.value());
    }

    @Override
    public void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, IDirectVar<TYPE> var) {
        var.set(streamCodec.decode(buffer));
    }

    @Override
    public DirectRef<IDirectVar<TYPE>> createDirectRef(ManagedKey managedKey, IDirectVar<TYPE> var) {
        if (markFunction == null) {
            return new UniqueDirectRef<>(var, managedKey, this);
        }
        return new MutableDirectRef<>(var, managedKey, this);
    }

    @Override
    public IDirectVar<TYPE> createDirectVar(ManagedKey managedKey, @NotNull Object holder) {
        return DirectField.of(managedKey.getRawField(), holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Object obtainManagedMark(@NotNull Object value) {
        return markFunction == null ? codec.encodeStart(JavaOps.INSTANCE, (TYPE)value).getOrThrow() : markFunction.obtainManagedMark(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean areDifferent(@NotNull Object managedMark, @NotNull Object value) {
        return markFunction == null ? !Objects.equals(managedMark, codec.encodeStart(JavaOps.INSTANCE, (TYPE)value).getOrThrow()) :
                markFunction.areDifferent(managedMark, value);
    }

    public static class Builder<TYPE> {
        private final Class<TYPE> type;
        private final boolean supportChildClass;
        private Codec<TYPE> codec;
        private StreamCodec<? super RegistryFriendlyByteBuf, TYPE> streamCodec;
        private @Nullable IMarkFunction<TYPE, ?> markFunction;

        protected Builder(Class<TYPE> type, boolean supportChildClass) {
            this.type = type;
            this.supportChildClass = supportChildClass;
        }

        public Builder<TYPE> codec(Codec<TYPE> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<TYPE> streamCodec(StreamCodec<? super RegistryFriendlyByteBuf, TYPE> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        /**
         * Use the codec to mark the value. This will use the {@link JavaOps} to generate the mark.
         */
        public Builder<TYPE> codecMark() {
            this.markFunction = new IMarkFunction.Simple<>(
                    value -> codec.encodeStart(JavaOps.INSTANCE, value).getOrThrow(),
                    (mark, value) -> !Objects.equals(mark, codec.encodeStart(JavaOps.INSTANCE, value).getOrThrow()));
            return this;
        }

        /**
         * Copy the mark from the value.
         * This will use the {@link Objects#equals(Object, Object)} to compare the mark.
         * Make sure the object supports {@link Object#equals(Object)}.
         * @param managedMarkFunction the function to get the mark from the value.
         */
        public Builder<TYPE> copyMark(Function<TYPE, TYPE> managedMarkFunction) {
            this.markFunction = new IMarkFunction.Simple<>(managedMarkFunction, Objects::equals);
            return this;
        }

        /**
         * Custom mark function. This will use the given function to get and compare marks.
         * @param managedMarkFunction the function to get the mark from the value.
         * @param areDifferentFunction the function to compare the mark with the value.
         * @param <MARK> the type of the mark.
         */
        public <MARK> Builder<TYPE> customMark(Function<TYPE, MARK> managedMarkFunction, BiPredicate<MARK, TYPE> areDifferentFunction) {
            this.markFunction = new IMarkFunction.Simple<>(managedMarkFunction, areDifferentFunction);
            return this;
        }

        public CustomDirectAccessor<TYPE> build() {
            return new CustomDirectAccessor<>(type, supportChildClass,
                    Objects.requireNonNull(codec, "Codec is not set"),
                    Objects.requireNonNull(streamCodec, "Stream codec is not set"),
                    markFunction);
        }
    }
}
