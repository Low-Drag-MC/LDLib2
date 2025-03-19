package com.lowdragmc.lowdraglib.syncdata;

import com.lowdragmc.lowdraglib.syncdata.accessor.*;
import com.lowdragmc.lowdraglib.utils.LDLibExtraCodecs;
import com.lowdragmc.lowdraglib.utils.Range;
import com.lowdragmc.lowdraglib.utils.Size;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import com.lowdragmc.lowdraglib.utils.Position;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.NeoForgeExtraCodecs;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.UUID;
import java.util.function.BiFunction;

@SuppressWarnings("unchecked")
public class SyncedFieldAccessors {

    public static final PrimitiveAccessor<Integer> INT_ACCESSOR = PrimitiveAccessor.of(Codec.INT, ByteBufCodecs.VAR_INT, int.class, Integer.class);
    public static final PrimitiveAccessor<Long> LONG_ACCESSOR = PrimitiveAccessor.of(Codec.LONG, ByteBufCodecs.VAR_LONG, long.class, Long.class);
    public static final PrimitiveAccessor<Float> FLOAT_ACCESSOR = PrimitiveAccessor.of(Codec.FLOAT, ByteBufCodecs.FLOAT, float.class, Float.class);
    public static final PrimitiveAccessor<Double> DOUBLE_ACCESSOR = PrimitiveAccessor.of(Codec.DOUBLE, ByteBufCodecs.DOUBLE, double.class, Double.class);
    public static final PrimitiveAccessor<Boolean> BOOLEAN_ACCESSOR = PrimitiveAccessor.of(Codec.BOOL, ByteBufCodecs.BOOL, boolean.class, Boolean.class);
    public static final PrimitiveAccessor<Byte> BYTE_ACCESSOR = PrimitiveAccessor.of(Codec.BYTE, ByteBufCodecs.BYTE, byte.class, Byte.class);
    public static final PrimitiveAccessor<Short> SHORT_ACCESSOR = PrimitiveAccessor.of(Codec.SHORT, ByteBufCodecs.SHORT, short.class, Short.class);
    public static final PrimitiveAccessor<Character> CHAR_ACCESSOR = PrimitiveAccessor.of(LDLibExtraCodecs.CHAR, ByteBufCodecs.VAR_INT.map(integer -> (char) integer.intValue(), character -> (int)character), char.class, Character.class);
    public static final PrimitiveAccessor<String> STRING_ACCESSOR = PrimitiveAccessor.of(Codec.STRING, ByteBufCodecs.STRING_UTF8, String.class);
    public static final EnumAccessor ENUM_ACCESSOR = new EnumAccessor();

    public static final RegistryAccessor<Block> BLOCK_ACCESSOR = RegistryAccessor.of(Block.class, BuiltInRegistries.BLOCK);
    public static final RegistryAccessor<Item> ITEM_ACCESSOR = RegistryAccessor.of(Item.class, BuiltInRegistries.ITEM);
    public static final RegistryAccessor<Fluid> FLUID_ACCESSOR = RegistryAccessor.of(Fluid.class, BuiltInRegistries.FLUID);
    public static final RegistryAccessor<EntityType<?>> ENTITY_TYPE_ACCESSOR = RegistryAccessor.of((Class<EntityType<?>>)(Class<?>) EntityType.class, BuiltInRegistries.ENTITY_TYPE);
    public static final RegistryAccessor<BlockEntityType<?>> BLOCK_ENTITY_TYPE_ACCESSOR = RegistryAccessor.of((Class<BlockEntityType<?>>)(Class<?>)BlockEntityType.class, BuiltInRegistries.BLOCK_ENTITY_TYPE);

    public static final INBTSerializableReadOnlyAccessor TAG_SERIALIZABLE_ACCESSOR = new INBTSerializableReadOnlyAccessor();

    public static final IManagedObjectAccessor MANAGED_ACCESSOR = new IManagedObjectAccessor();

    public static final CustomDirectAccessor<BlockPos> BLOCK_POA_ACCESSOR = CustomDirectAccessor.builder(BlockPos.class, false)
            .codec(BlockPos.CODEC)
            .streamCodec(BlockPos.STREAM_CODEC)
            .copyMark(BlockPos::new)
            .build();

    public static final CustomDirectAccessor<FluidStack> FLUID_STACK_ACCESSOR = CustomDirectAccessor.builder(FluidStack.class, false)
            .codec(FluidStack.CODEC)
            .streamCodec(FluidStack.STREAM_CODEC)
            .copyMark(FluidStack::copy)
            .build();

    public static final CustomDirectAccessor<ItemStack> ITEM_STACK_ACCESSOR = CustomDirectAccessor.builder(ItemStack.class, false)
            .codec(ItemStack.CODEC)
            .streamCodec(ItemStack.STREAM_CODEC)
            .copyMark(ItemStack::copy)
            .build();

    public static final CustomDirectAccessor<UUID> UUID_ACCESSOR = CustomDirectAccessor.builder(UUID.class, false)
            .codec(LDLibExtraCodecs.UUID)
            .streamCodec(StreamCodec.of(
                    (byteBuf, uuid) -> {
                        byteBuf.writeLong(uuid.getMostSignificantBits());
                        byteBuf.writeLong(uuid.getLeastSignificantBits());
                    },
                    byteBuf -> new UUID(byteBuf.readLong(), byteBuf.readLong())
            ))
            .build();

    public static final CustomDirectAccessor<Tag> ENTITY_ACCESSOR = CustomDirectAccessor.builder(Tag.class, true)
            .codec(LDLibExtraCodecs.TAG)
            .streamCodec(ByteBufCodecs.TRUSTED_TAG)
            .copyMark(Tag::copy)
            .build();

    public static final CustomDirectAccessor<BlockState> BLOCK_STATE_ACCESSOR =
            CustomDirectAccessor.builder(BlockState.class, false)
                    .codec(BlockState.CODEC)
                    .streamCodec(ByteBufCodecs.fromCodecWithRegistries(BlockState.CODEC))
                    .build();

    public static final CustomDirectAccessor<Position> POSITION_ACCESSOR =
            CustomDirectAccessor.builder(Position.class, false)
                    .codec(Position.CODEC)
                    .streamCodec(Position.STREAM_CODEC)
                    .build();

    public static final CustomDirectAccessor<Size> SIZE_ACCESSOR =
            CustomDirectAccessor.builder(Size.class, false)
                    .codec(Size.CODEC)
                    .streamCodec(Size.STREAM_CODEC)
                    .build();

    public static final CustomDirectAccessor<Range> RANGE_ACCESSOR =
            CustomDirectAccessor.builder(Range.class, false)
                    .codec(Range.CODEC)
                    .streamCodec(Range.STREAM_CODEC)
                    .copyMark(Range::new)
                    .build();

    public static final CustomDirectAccessor<Vector3f> VECTOR3_ACCESSOR =
            CustomDirectAccessor.builder(Vector3f.class, false)
                    .codec(ExtraCodecs.VECTOR3F)
                    .streamCodec(ByteBufCodecs.VECTOR3F)
                    .copyMark(Vector3f::new)
                    .build();

    public static final CustomDirectAccessor<Vector4f> VECTOR4_ACCESSOR =
            CustomDirectAccessor.builder(Vector4f.class, false)
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
                    .build();

    public static final CustomDirectAccessor<Quaternionf> QUATERNION_ACCESSOR =
            CustomDirectAccessor.builder(Quaternionf.class, false)
                    .codec(ExtraCodecs.QUATERNIONF)
                    .streamCodec(ByteBufCodecs.QUATERNIONF)
                    .copyMark(Quaternionf::new)
                    .build();

    public static final CustomDirectAccessor<AABB> AABB_ACCESSOR =
            CustomDirectAccessor.builder(AABB.class, false)
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
                    .build();

    public static final CustomDirectAccessor<Component> COMPONENT_ACCESSOR =
            CustomDirectAccessor.builder(Component.class, true)
                    .codec(ComponentSerialization.CODEC)
                    .streamCodec(ComponentSerialization.STREAM_CODEC)
                    .codecMark()
                    .build();

    public static final CustomDirectAccessor<ResourceLocation> RESOURCE_LOCATION_ACCESSOR =
            CustomDirectAccessor.builder(ResourceLocation.class, true)
                    .codec(ResourceLocation.CODEC)
                    .streamCodec(ResourceLocation.STREAM_CODEC)
                    .build();

    public static final IAccessor RECIPE_ACCESSOR = new RecipeAccessor();
    public static final IAccessor GUI_TEXTURE_ACCESSOR = new IGuiTextureAccessor();
    public static final IAccessor RENDERER_ACCESSOR = new IRendererAccessor();

    private static final BiFunction<IAccessor, Class<?>, IAccessor> ARRAY_ACCESSOR_FACTORY = Util.memoize(ArrayAccessor::new);
    private static final BiFunction<IAccessor, Class<?>, IAccessor> COLLECTION_ACCESSOR_FACTORY = Util.memoize(CollectionAccessor::new);
    public static IAccessor collectionAccessor(IAccessor childAccessor, Class<?> child) {
        com.lowdragmc.lowdraglib.gui.editor.accessors.RangeAccessor
        return COLLECTION_ACCESSOR_FACTORY.apply(childAccessor, child);
    }
    public static IAccessor arrayAccessor(IAccessor childAccessor, Class<?> child) {
        return ARRAY_ACCESSOR_FACTORY.apply(childAccessor, child);
    }
}
