package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Range;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.IRendererAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.RecipeAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.arraylike.ArrayAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.arraylike.CollectionAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.EnumAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.PrimitiveAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.direct.RegistryAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.readonly.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib.syncdata.accessor.readonly.INBTSerializableReadOnlyAccessor;
import com.lowdragmc.lowdraglib.utils.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;


public class AccessorRegistries {
    public record AccessorHolder(IAccessor<?> accessor, int priority) { }
    private final static List<AccessorHolder> ACCESSOR_HOLDERS = Collections.synchronizedList(new ArrayList<>());
    private final static Map<Class<?>, IAccessor<?>> ACCESSOR_LOOKUP = new ConcurrentHashMap<>();
    private final static BiFunction<IAccessor, Class<?>, IAccessor<?>> ARRAY_ACCESSOR_LOOKUP = Util.memoize(ArrayAccessor::new);
    private final static BiFunction<IAccessor, Class<?>, IAccessor<?>> COLLECTION_ACCESSOR_LOOKUP = Util.memoize(CollectionAccessor::new);
    /**
     * Register an accessor with a given priority.
     * Lower priority accessors will be checked first.
     *
     * @param accessor The accessor to register
     * @param priority The priority of the accessor. Priority Range:
     * <ul>
     *   <li><code>-1</code> - Highest priority, for primitive types, e.g. int, long, float, double, boolean, byte, short, char, String</li>
     *   <li><code>100</code> - Medium priority, for registry types and Unique/Standalone/Final type, e.g. UUID, Block, Item, Fluid, EntityType, BlockEntityType</li>
     *   <li><code>1000</code> - Default priority, for common types, e.g. ItemStack, FluidStack</li>
     *   <li><code>1500</code> - Low priority, for read-only types, e.g., IManaged</li>
     *   <li><code>2000</code> - Lowest priority, for abstract/interface types, e.g., INBTSerializable</li>
     * </ul>
     */
    public static void registerAccessor(IAccessor<?> accessor, int priority) {
        synchronized (ACCESSOR_HOLDERS) {
            var index = Collections.binarySearch(
                    ACCESSOR_HOLDERS,
                    new AccessorHolder(accessor, priority),
                    Comparator.comparingInt(AccessorHolder::priority));
            if (index < 0) {
                ACCESSOR_HOLDERS.add(-index - 1, new AccessorHolder(accessor, priority));
            } else {
                ACCESSOR_HOLDERS.add(index, new AccessorHolder(accessor, priority));
            }
        }
    }

    private final static ThreadLocal<Integer> PRIORITY = ThreadLocal.withInitial(() -> 1000);

    /**
     * Set the priority for the current thread.
     * @param priority The priority of the accessor. Priority Range:
     * <ul>
     *   <li><code>-1</code> - Highest priority, for primitive types, e.g. int, long, float, double, boolean, byte, short, char, String</li>
     *   <li><code>100</code> - Medium priority, for registry types and Unique/Standalone/Final type, e.g. UUID, Block, Item, Fluid, EntityType, BlockEntityType</li>
     *   <li><code>1000</code> - Default priority, for common types, e.g. ItemStack, FluidStack</li>
     *   <li><code>1500</code> - Low priority, for read-only types, e.g., IManaged</li>
     *   <li><code>2000</code> - Lowest priority, for abstract/interface types, e.g., INBTSerializable</li>
     * </ul>
     */
    public static void setPriority(int priority) {
        PRIORITY.set(priority);
    }

    /**
     * Register an accessor by using the current priority {@link #setPriority(int)}.
     * Its useful for registering accessors in a static block.
     * @param accessor The accessor to register
     */
    public static void registerAccessor(IAccessor<?> accessor) {
        registerAccessor(accessor, PRIORITY.get());
    }

    public static IAccessor<?> findByClass(Class<?> clazz) {
        IAccessor<?> result = ACCESSOR_LOOKUP.computeIfAbsent(clazz, c -> {
            synchronized (ACCESSOR_HOLDERS) {
                for (AccessorHolder holder : ACCESSOR_HOLDERS) {
                    if (holder.accessor.test(c)) {
                        return holder.accessor;
                    }
                }
            }
            return null;
        });

        if (result == null) {
            throw new IllegalArgumentException("No payload found for class " + clazz.getName());
        }
        return result;
    }

    public static IAccessor<?> findCollectionAccessor(IAccessor<?> childAccessor, Class<?> child) {
        return COLLECTION_ACCESSOR_LOOKUP.apply(childAccessor, child);
    }

    public static IAccessor<?> findArrayAccessor(IAccessor<?> childAccessor, Class<?> child) {
        return ARRAY_ACCESSOR_LOOKUP.apply(childAccessor, child);
    }

    public static IAccessor<?> findByType(Type type) {
        if (type instanceof GenericArrayType array) {
            var componentType = array.getGenericComponentType();
            var childAccessor = findByType(componentType);
            var rawType = ReflectionUtils.getRawType(componentType);

            return findArrayAccessor(childAccessor, rawType == null ? Object.class : rawType);
        }
        var rawType = ReflectionUtils.getRawType(type);
        if (rawType != null) {
            if (rawType.isArray()) {
                var componentType = rawType.getComponentType();
                var childAccessor = findByType(componentType);
                return findArrayAccessor(childAccessor, componentType);
            }
            if (Collection.class.isAssignableFrom(rawType)) {
                var componentType = ((ParameterizedType) type).getActualTypeArguments()[0];
                var childAccessor = findByType(componentType);
                var rawComponentType = ReflectionUtils.getRawType(componentType);

                return findCollectionAccessor(childAccessor, rawComponentType == null ? Object.class : rawComponentType);
            }
            return findByClass(rawType);
        }

        throw new IllegalArgumentException("No payload found for class " + type.getTypeName());
    }

    /**
     * Initialize the default accessors.
     */

    public static void init() {
        setPriority(-1);

        registerAccessor(PrimitiveAccessor.of(Codec.INT, ByteBufCodecs.VAR_INT, int.class, Integer.class));
        registerAccessor(PrimitiveAccessor.of(Codec.LONG, ByteBufCodecs.VAR_LONG, long.class, Long.class));
        registerAccessor(PrimitiveAccessor.of(Codec.FLOAT, ByteBufCodecs.FLOAT, float.class, Float.class));
        registerAccessor(PrimitiveAccessor.of(Codec.DOUBLE, ByteBufCodecs.DOUBLE, double.class, Double.class));
        registerAccessor(PrimitiveAccessor.of(Codec.BOOL, ByteBufCodecs.BOOL, boolean.class, Boolean.class));
        registerAccessor(PrimitiveAccessor.of(Codec.BYTE, ByteBufCodecs.BYTE, byte.class, Byte.class));
        registerAccessor(PrimitiveAccessor.of(Codec.SHORT, ByteBufCodecs.SHORT, short.class, Short.class));
        registerAccessor(PrimitiveAccessor.of(LDLibExtraCodecs.CHAR, ByteBufCodecs.VAR_INT.map(integer -> (char) integer.intValue(), character -> (int)character), char.class, Character.class));
        registerAccessor(PrimitiveAccessor.of(Codec.STRING, ByteBufCodecs.STRING_UTF8, String.class));
        registerAccessor(new EnumAccessor());

        setPriority(100);

        registerAccessor(RegistryAccessor.of(Block.class, BuiltInRegistries.BLOCK));
        registerAccessor(RegistryAccessor.of(Item.class, BuiltInRegistries.ITEM));
        registerAccessor(RegistryAccessor.of(Fluid.class, BuiltInRegistries.FLUID));
        registerAccessor(RegistryAccessor.of((Class<EntityType<?>>)(Class<?>) EntityType.class, BuiltInRegistries.ENTITY_TYPE));
        registerAccessor(RegistryAccessor.of((Class<BlockEntityType<?>>)(Class<?>)BlockEntityType.class, BuiltInRegistries.BLOCK_ENTITY_TYPE));
        registerAccessor(CustomDirectAccessor.builder(UUID.class)
                .codec(LDLibExtraCodecs.UUID)
                .streamCodec(StreamCodec.of(
                        (byteBuf, uuid) -> {
                            byteBuf.writeLong(uuid.getMostSignificantBits());
                            byteBuf.writeLong(uuid.getLeastSignificantBits());
                        },
                        byteBuf -> new UUID(byteBuf.readLong(), byteBuf.readLong())
                ))
                .build());
        registerAccessor(CustomDirectAccessor.builder(BlockState.class)
                .codec(BlockState.CODEC)
                .streamCodec(ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC))
                .build());
        registerAccessor(CustomDirectAccessor.builder(Position.class)
                .codec(Position.CODEC)
                .streamCodec(Position.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Size.class)
                .codec(Size.CODEC)
                .streamCodec(Size.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Range.class)
                .codec(Range.CODEC)
                .streamCodec(Range.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(ResourceLocation.class, true)
                .codec(ResourceLocation.CODEC)
                .streamCodec(ResourceLocation.STREAM_CODEC)
                .build());

        setPriority(1000);

        registerAccessor(CustomDirectAccessor.builder(Vector3f.class)
                .codec(ExtraCodecs.VECTOR3F)
                .streamCodec(ByteBufCodecs.VECTOR3F)
                .copyMark(Vector3f::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Vector4f.class)
                .codec(ExtraCodecs.VECTOR4F)
                .streamCodec(StreamCodec.of(
                        (byteBuf, vector) -> {
                            byteBuf.writeFloat(vector.x);
                            byteBuf.writeFloat(vector.y);
                            byteBuf.writeFloat(vector.z);
                            byteBuf.writeFloat(vector.w);
                        },
                        byteBuf -> new Vector4f(byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat(), byteBuf.readFloat())
                ))
                .copyMark(Vector4f::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Quaternionf.class)
                .codec(ExtraCodecs.QUATERNIONF)
                .streamCodec(ByteBufCodecs.QUATERNIONF)
                .copyMark(Quaternionf::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(AABB.class)
                .codec(RecordCodecBuilder.create(instance -> instance.group(
                        Vec3.CODEC.fieldOf("min").forGetter(AABB::getMinPosition),
                        Vec3.CODEC.fieldOf("max").forGetter(AABB::getMaxPosition)
                ).apply(instance, AABB::new)))
                .streamCodec(StreamCodec.of(
                        (byteBuf, aabb) -> {
                            byteBuf.writeDoubleLE(aabb.minX);
                            byteBuf.writeDoubleLE(aabb.minY);
                            byteBuf.writeDoubleLE(aabb.minZ);
                            byteBuf.writeDoubleLE(aabb.maxX);
                            byteBuf.writeDoubleLE(aabb.maxY);
                            byteBuf.writeDoubleLE(aabb.maxZ);
                        },
                        byteBuf -> new AABB(
                                byteBuf.readDoubleLE(), byteBuf.readDoubleLE(), byteBuf.readDoubleLE(),
                                byteBuf.readDoubleLE(), byteBuf.readDoubleLE(), byteBuf.readDoubleLE())
                ))
                .build());
        registerAccessor(CustomDirectAccessor.builder(BlockPos.class)
                .codec(BlockPos.CODEC)
                .streamCodec(BlockPos.STREAM_CODEC)
                .copyMark(BlockPos::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(FluidStack.class)
                .codec(FluidStack.CODEC)
                .streamCodec(FluidStack.STREAM_CODEC)
                .copyMark(FluidStack::copy)
                .build());
        registerAccessor(CustomDirectAccessor.builder(ItemStack.class)
                .codec(ItemStack.CODEC)
                .streamCodec(ItemStack.STREAM_CODEC)
                .copyMark(ItemStack::copy)
                .build());
        registerAccessor(CustomDirectAccessor.builder(IGuiTexture.class)
                .codec(IGuiTexture.CODEC)
                .streamCodec(ByteBufCodecs.fromCodec(IGuiTexture.CODEC))
                .copyMark(IGuiTexture::copy)
                .build());


        setPriority(1500);

        registerAccessor(new IManagedObjectAccessor());

        setPriority(2000);

        registerAccessor(new INBTSerializableReadOnlyAccessor());
        registerAccessor(CustomDirectAccessor.builder(Tag.class, true)
                .codec(LDLibExtraCodecs.TAG)
                .streamCodec(ByteBufCodecs.TRUSTED_TAG)
                .copyMark(Tag::copy)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Component.class, true)
                .codec(ComponentSerialization.CODEC)
                .streamCodec(ComponentSerialization.STREAM_CODEC)
                .codecMark()
                .build());

        setPriority(1000);

        public static final IAccessor RECIPE_ACCESSOR = new RecipeAccessor();
        public static final IAccessor RENDERER_ACCESSOR = new IRendererAccessor();
    }

}
