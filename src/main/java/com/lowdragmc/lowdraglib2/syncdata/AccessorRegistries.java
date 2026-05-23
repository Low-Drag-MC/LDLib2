package com.lowdragmc.lowdraglib2.syncdata;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.data.LengthPercent;
import com.lowdragmc.lowdraglib2.gui.ui.data.Pivot;
import com.lowdragmc.lowdraglib2.gui.ui.data.Translate2D;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Range;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.arraylike.ArrayAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.arraylike.CollectionAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.maplike.MapAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.EnumAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.PrimitiveAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.RegistryAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IManagedObjectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.INBTSerializableReadOnlyAccessor;
import com.lowdragmc.lowdraglib2.utils.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.joml.*;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public class AccessorRegistries {
    public record AccessorHolder(IAccessor<?> accessor, int priority) { }
    private final static List<AccessorHolder> ACCESSOR_HOLDERS = Collections.synchronizedList(new ArrayList<>());
    private final static Map<Class<?>, IAccessor<?>> ACCESSOR_LOOKUP = new ConcurrentHashMap<>();
    private final static BiFunction<IAccessor, Class<?>, IAccessor<?>> ARRAY_ACCESSOR_LOOKUP = Util.memoize(ArrayAccessor::new);
    private final static BiFunction<IAccessor, Class<?>, IAccessor<?>> COLLECTION_ACCESSOR_LOOKUP = Util.memoize(CollectionAccessor::new);
    private final static Map<MapAccessorKey, IAccessor<?>> MAP_ACCESSOR_LOOKUP = new ConcurrentHashMap<>();
    private record MapAccessorKey(IAccessor<?> keyAccessor, Class<?> keyType, IAccessor<?> valueAccessor, Class<?> valueType) {}
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
        var result = findByClassOrNull(clazz);
        if (result == null) {
            throw new IllegalArgumentException("No payload found for class " + clazz.getName());
        }
        return result;
    }

    /**
     * Non-throwing variant of {@link #findByClass}. Returns {@code null} when no accessor matches,
     * letting the caller decide whether the absence is fatal or expected. Use this in optional /
     * data-driven paths (e.g. node-graph constant serialization where missing accessors should
     * gracefully degrade) — internal contracts that require an accessor should keep calling the
     * throwing variant.
     */
    @Nullable
    public static IAccessor<?> findByClassOrNull(Class<?> clazz) {
        return ACCESSOR_LOOKUP.computeIfAbsent(clazz, c -> {
            synchronized (ACCESSOR_HOLDERS) {
                for (AccessorHolder holder : ACCESSOR_HOLDERS) {
                    if (holder.accessor.test(c)) {
                        return holder.accessor;
                    }
                }
            }
            return null;
        });
    }

    public static IAccessor<?> findCollectionAccessor(IAccessor<?> childAccessor, Class<?> child) {
        return COLLECTION_ACCESSOR_LOOKUP.apply(childAccessor, child);
    }

    public static IAccessor<?> findArrayAccessor(IAccessor<?> childAccessor, Class<?> child) {
        return ARRAY_ACCESSOR_LOOKUP.apply(childAccessor, child);
    }

    public static IAccessor<?> findMapAccessor(IAccessor<?> keyAccessor, Class<?> keyType,
                                               IAccessor<?> valueAccessor, Class<?> valueType) {
        return MAP_ACCESSOR_LOOKUP.computeIfAbsent(
                new MapAccessorKey(keyAccessor, keyType, valueAccessor, valueType),
                k -> new MapAccessor(k.keyAccessor(), k.keyType(), k.valueAccessor(), k.valueType()));
    }

    public static IAccessor<?> findByType(Type type) {
        var result = findByTypeOrNull(type);
        if (result == null) {
            throw new IllegalArgumentException("No payload found for class " + type.getTypeName());
        }
        return result;
    }

    /**
     * Non-throwing variant of {@link #findByType}. Returns {@code null} when no accessor matches
     * the type (or any element of a container type) — caller decides whether the absence is
     * fatal. Container types (array/map/collection) cascade: if any element type can't be
     * resolved, the whole call returns {@code null} rather than constructing a partially-typed
     * accessor.
     */
    @Nullable
    public static IAccessor<?> findByTypeOrNull(Type type) {
        if (type instanceof GenericArrayType array) {
            var componentType = array.getGenericComponentType();
            var childAccessor = findByTypeOrNull(componentType);
            if (childAccessor == null) return null;
            var rawType = ReflectionUtils.getRawType(componentType);
            return findArrayAccessor(childAccessor, rawType == null ? Object.class : rawType);
        }
        var rawType = ReflectionUtils.getRawType(type);
        if (rawType != null) {
            if (rawType.isArray()) {
                var componentType = rawType.getComponentType();
                var childAccessor = findByTypeOrNull(componentType);
                if (childAccessor == null) return null;
                return findArrayAccessor(childAccessor, componentType);
            }
            if (Map.class.isAssignableFrom(rawType) && type instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getActualTypeArguments().length == 2) {
                    var keyTypeArg = parameterizedType.getActualTypeArguments()[0];
                    var valueTypeArg = parameterizedType.getActualTypeArguments()[1];
                    var keyAccessor = findByTypeOrNull(keyTypeArg);
                    var valueAccessor = findByTypeOrNull(valueTypeArg);
                    if (keyAccessor == null || valueAccessor == null) return null;
                    var rawKeyType = ReflectionUtils.getRawType(keyTypeArg);
                    var rawValueType = ReflectionUtils.getRawType(valueTypeArg);
                    return findMapAccessor(
                            keyAccessor, rawKeyType == null ? Object.class : rawKeyType,
                            valueAccessor, rawValueType == null ? Object.class : rawValueType);
                }
            }
            if (Collection.class.isAssignableFrom(rawType) && type instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getActualTypeArguments().length == 1) {
                    var componentType = parameterizedType.getActualTypeArguments()[0];
                    var childAccessor = findByTypeOrNull(componentType);
                    if (childAccessor == null) return null;
                    var rawComponentType = ReflectionUtils.getRawType(componentType);

                    return findCollectionAccessor(childAccessor, rawComponentType == null ? Object.class : rawComponentType);
                }
            }
            return findByClassOrNull(rawType);
        }
        return null;
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
        registerAccessor(CustomDirectAccessor.builder(UIEvent.class)
                .codec(UIEvent.CODEC)
                .streamCodec(UIEvent.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Position.class)
                .codec(Position.CODEC)
                .streamCodec(Position.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Size.class)
                .codec(Size.CODEC)
                .streamCodec(Size.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Pivot.class)
                .codec(Pivot.CODEC)
                .streamCodec(Pivot.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(LengthPercent.class)
                .codec(LengthPercent.CODEC)
                .streamCodec(LengthPercent.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Translate2D.class)
                .codec(Translate2D.CODEC)
                .streamCodec(Translate2D.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Range.class)
                .codec(Range.CODEC)
                .streamCodec(Range.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(ResourceLocation.class)
                .codec(ResourceLocation.CODEC)
                .streamCodec(ResourceLocation.STREAM_CODEC)
                .build());

        setPriority(1000);
        registerAccessor(CustomDirectAccessor.builder(Number.class)
                .codec(LDLibExtraCodecs.NUMBER)
                .streamCodec(ByteBufCodecs.fromCodec(LDLibExtraCodecs.NUMBER))
                .codecMark()
                .build());
        registerAccessor(CustomDirectAccessor.builder(Vector3f.class)
                .codec(ExtraCodecs.VECTOR3F)
                .streamCodec(ByteBufCodecs.VECTOR3F)
                .copyMark(Vector3f::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Vector3i.class)
                .codec(LDLibExtraCodecs.VECTOR3I)
                .streamCodec(StreamCodec.of(
                        (byteBuf, vector) -> {
                            byteBuf.writeVarInt(vector.x);
                            byteBuf.writeVarInt(vector.y);
                            byteBuf.writeVarInt(vector.z);
                        },
                        byteBuf -> new Vector3i(byteBuf.readVarInt(), byteBuf.readVarInt(), byteBuf.readVarInt())
                ))
                .copyMark(Vector3i::new)
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
        registerAccessor(CustomDirectAccessor.builder(Vector2f.class)
                .codec(LDLibExtraCodecs.VECTOR2F)
                .streamCodec(StreamCodec.of(
                        (byteBuf, vector) -> {
                            byteBuf.writeFloat(vector.x);
                            byteBuf.writeFloat(vector.y);
                        },
                        byteBuf -> new Vector2f(byteBuf.readFloat(), byteBuf.readFloat())
                ))
                .copyMark(Vector2f::new)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Vector2i.class)
                .codec(LDLibExtraCodecs.VECTOR2I)
                .streamCodec(StreamCodec.of(
                        (byteBuf, vector) -> {
                            byteBuf.writeVarInt(vector.x);
                            byteBuf.writeVarInt(vector.y);
                        },
                        byteBuf -> new Vector2i(byteBuf.readVarInt(), byteBuf.readVarInt())
                ))
                .copyMark(Vector2i::new)
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
        registerAccessor(CustomDirectAccessor.builder(ChunkPos.class)
                .codec(Codec.LONG.xmap(ChunkPos::new, ChunkPos::toLong))
                .streamCodec(ByteBufCodecs.VAR_LONG.map(ChunkPos::new, ChunkPos::toLong))
                .copyMark(chunkPos -> new ChunkPos(chunkPos.x, chunkPos.z))
                .build());
        registerAccessor(CustomDirectAccessor.builder(FluidStack.class)
                .codec(FluidStack.OPTIONAL_CODEC)
                .streamCodec(FluidStack.OPTIONAL_STREAM_CODEC)
                .customMark(FluidStack::copy, FluidStack::matches)
                .build());
        registerAccessor(CustomDirectAccessor.builder(ItemStack.class)
                .codec(LDLibExtraCodecs.ITEM_STACK)
                .streamCodec(ItemStack.OPTIONAL_STREAM_CODEC)
                .customMark(ItemStack::copy, ItemStack::matches)
                .build());
        if (LDLib2.isClient()) {
            registerAccessor(CustomDirectAccessor.builder(IGuiTexture.class, true)
                    .codec(IGuiTexture.CODEC)
                    .streamCodec(ByteBufCodecs.fromCodec(IGuiTexture.CODEC))
                    .copyMark(IGuiTexture::copy)
                    .build());
            registerAccessor(CustomDirectAccessor.builder(IRenderer.class, true)
                    .codec(IRenderer.CODEC)
                    .streamCodec(ByteBufCodecs.fromCodec(IRenderer.CODEC))
                    .build());
        }
        registerAccessor(CustomDirectAccessor.builder(RecipeHolder.class)
                .codec(RecordCodecBuilder.create(instance -> instance.group(
                        ResourceLocation.CODEC.fieldOf("id").forGetter(RecipeHolder::id),
                        Recipe.CODEC.fieldOf("recipe").forGetter(RecipeHolder::value)
                ).apply(instance, RecipeHolder::new)))
                .streamCodec((StreamCodec<RegistryFriendlyByteBuf, RecipeHolder>) (Object)RecipeHolder.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(Recipe.class, true)
                .codec((Codec<Recipe>) (Object) Recipe.CODEC)
                .streamCodec((StreamCodec<RegistryFriendlyByteBuf, Recipe>) (Object)Recipe.STREAM_CODEC)
                .build());
        registerAccessor(CustomDirectAccessor.builder(IResourcePath.class, true)
                .codec(IResourcePath.CODEC)
                .streamCodec(ByteBufCodecs.fromCodec(IResourcePath.CODEC))
                .build());
        registerAccessor(CustomDirectAccessor.builder(UITemplate.class)
                .codec(UITemplate.CODEC)
                .streamCodec(UITemplate.STREAM_CODEC)
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
    }

}
