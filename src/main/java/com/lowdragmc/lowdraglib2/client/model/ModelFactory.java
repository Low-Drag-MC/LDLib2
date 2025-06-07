package com.lowdragmc.lowdraglib2.client.model;

import com.google.gson.JsonParseException;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ModelBakeryAccessor;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Transformation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Function;

/**
 * Author: KilaBash
 * Date: 2022/04/24
 * Description:
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModelFactory {
    public static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();

    public static ModelBakery getModelBakery() {
        return Minecraft.getInstance().getModelManager().getModelBakery();
    }

    public static UnbakedModel getLDLibModel(UnbakedModel vanilla) {
        return vanilla;
    }

    public static ModelBaker getModelBaker() {
        return new ModelBaker() {
            @Override
            public UnbakedModel getModel(ResourceLocation location) {
                return getUnBakedModel(location);
            }

            @Override
            public UnbakedModel getTopLevelModel(ModelResourceLocation modelResourceLocation) {
                return ModelFactory.getTopLevelModel(modelResourceLocation);
            }

            @Override
            public @Nullable BakedModel bake(ResourceLocation location, ModelState state, Function<Material, TextureAtlasSprite> sprites) {
                UnbakedModel unbakedmodel = this.getModel(location);
                if (unbakedmodel instanceof BlockModel blockmodel) {
                    if (blockmodel.getRootModel() == ModelBakery.GENERATION_MARKER) {
                        return ITEM_MODEL_GENERATOR.generateBlockModel(Material::sprite, blockmodel).bake(this, blockmodel, Material::sprite, state, false);
                    }
                }
                return unbakedmodel.bake(this, Material::sprite, state);
            }

            @Override
            public @Nullable BakedModel bakeUncached(UnbakedModel unbakedModel, ModelState modelState, Function<Material, TextureAtlasSprite> function) {
                if (unbakedModel instanceof BlockModel blockmodel) {
                    if (blockmodel.getRootModel() == ModelBakery.GENERATION_MARKER) {
                        return ITEM_MODEL_GENERATOR.generateBlockModel(Material::sprite, blockmodel).bake(this, blockmodel, Material::sprite, modelState, false);
                    }
                }
                return unbakedModel.bake(this, Material::sprite, modelState);
            }

            @Override
            public Function<Material, TextureAtlasSprite> getModelTextureGetter() {
                return Material::sprite;
            }

            @Override
            public BakedModel bake(ResourceLocation location, ModelState transform) {
                return this.bake(location, transform, getModelTextureGetter());
            }
        };
    }

    public static UnbakedModel getUnBakedModel(ResourceLocation modelLocation) {
        return ((ModelBakeryAccessor)getModelBakery()).invokeGetModel(modelLocation);
    }

    public static UnbakedModel getTopLevelModel(ModelResourceLocation modelLocation) {
        return ((ModelBakeryAccessor)getModelBakery()).getTopLevelModels().get(modelLocation);
    }

    public static Quaternionf getQuaternion(Direction facing) {
        return switch (facing) {
            case UP -> new Quaternionf().rotateXYZ(Mth.HALF_PI, 0, 0);
            case DOWN -> new Quaternionf().rotateXYZ(-Mth.HALF_PI, 0, 0);
            case EAST -> new Quaternionf().rotateXYZ(0, -Mth.HALF_PI, 0);
            case WEST -> new Quaternionf().rotateXYZ(0, Mth.HALF_PI, 0);
            case SOUTH -> new Quaternionf().rotateXYZ(0, Mth.PI, 0);
            case NORTH -> new Quaternionf();
        };
    }

    public static ModelState getRotation(Direction facing) {
        return switch (facing) {
            case DOWN -> BlockModelRotation.X90_Y0;
            case UP -> BlockModelRotation.X270_Y0;
            case NORTH -> BlockModelRotation.X0_Y0;
            case SOUTH -> BlockModelRotation.X0_Y180;
            case WEST -> BlockModelRotation.X0_Y270;
            case EAST -> BlockModelRotation.X0_Y90;
        };
    }

    public static Direction modelFacing(Direction side, Direction frontFacing) {
        if (side == frontFacing) return Direction.NORTH;
        if (frontFacing == Direction.NORTH) return side;
        if (frontFacing == Direction.SOUTH) {
            if (side.getAxis() == Direction.Axis.Y) return side;
            return side.getOpposite();
        }
        if (frontFacing == Direction.EAST) {
            if (side.getAxis() == Direction.Axis.Y) return side;
            return side.getCounterClockWise();
        }
        if (frontFacing == Direction.WEST) {
            if (side.getAxis() == Direction.Axis.Y) return side;
            return side.getClockWise();
        }
        if (frontFacing == Direction.UP) {
            if (side == Direction.DOWN) return Direction.SOUTH;
            if (side.getAxis() == Direction.Axis.X) return side;
            if (side == Direction.SOUTH) return Direction.UP;
            if (side == Direction.NORTH) return Direction.DOWN;
        }
        if (frontFacing == Direction.DOWN) {
            if (side == Direction.UP) return Direction.SOUTH;
            if (side.getAxis() == Direction.Axis.X) return side;
            if (side == Direction.SOUTH) return Direction.DOWN;
            if (side == Direction.NORTH) return Direction.UP;
        }
        return side;
    }

    private record ModelStateWrapper(ModelState modelState, boolean lockedUV) implements ModelState {
        @Override
        @Nonnull
        public Transformation getRotation() {
            return modelState.getRotation();
        }

        @Override
        public boolean isUvLocked() {
            return lockedUV;
        }
    }

    public static ModelState getRotation(Direction facing, boolean lockedUV) {
        if (lockedUV) {
            return new ModelStateWrapper(getRotation(facing), true);
        } else {
            return getRotation(facing);
        }
    }

    public static Either<Material, String> parseTextureLocationOrReference(ResourceLocation pLocation, String pName) {
        if (isTextureReference(pName)) {
            return Either.right(pName.substring(1));
        } else {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(pName);
            if (resourcelocation == null) {
                throw new JsonParseException(pName + " is not valid resource location");
            } else {
                return Either.left(new Material(pLocation, resourcelocation));
            }
        }
    }

    public static Either<Material, String> parseBlockTextureLocationOrReference(String pName) {
        if (isTextureReference(pName)) {
            return Either.right(pName.substring(1));
        } else {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(pName);
            if (resourcelocation == null) {
                throw new JsonParseException(pName + " is not valid resource location");
            } else {
                return Either.left(new Material(TextureAtlas.LOCATION_BLOCKS, resourcelocation));
            }
        }
    }

    private static boolean isTextureReference(String pStr) {
        return pStr.charAt(0) == '#';
    }

    public static TextureAtlasSprite getBlockSprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(location);
    }

    public static TextureAtlasSprite getSprite(ResourceLocation atlas, ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(atlas).apply(location);
    }

    public static ItemTransform makeTransform(float rotationX, float rotationY, float rotationZ, float translationX, float translationY, float translationZ, float scaleX, float scaleY, float scaleZ) {
        var translation = new Vector3f(translationX, translationY, translationZ);
        translation.mul(0.0625f);
        translation.set(Mth.clamp(translation.x, -5.0F, 5.0F), Mth.clamp(translation.y, -5.0F, 5.0F), Mth.clamp(translation.z, -5.0F, 5.0F));
        return new ItemTransform(new Vector3f(rotationX, rotationY, rotationZ), translation, new Vector3f(scaleX, scaleY, scaleZ));
    }

    public static final ItemTransform TRANSFORM_BLOCK_GUI = makeTransform(30, 225, 0, 0, 0, 0, 0.625f, 0.625f, 0.625f);
    public static final ItemTransform TRANSFORM_BLOCK_GROUND = makeTransform(0, 0, 0, 0, 3, 0, 0.25f, 0.25f, 0.25f);
    public static final ItemTransform TRANSFORM_BLOCK_FIXED = makeTransform(0, 0, 0, 0, 0, 0, 0.5f, 0.5f, 0.5f);
    public static final ItemTransform TRANSFORM_BLOCK_3RD_PERSON_RIGHT = makeTransform(75, 45, 0, 0, 2.5f, 0, 0.375f, 0.375f, 0.375f);
    public static final ItemTransform TRANSFORM_BLOCK_1ST_PERSON_RIGHT = makeTransform(0, 45, 0, 0, 0, 0, 0.4f, 0.4f, 0.4f);
    public static final ItemTransform TRANSFORM_BLOCK_1ST_PERSON_LEFT = makeTransform(0, 225, 0, 0, 0, 0, 0.4f, 0.4f, 0.4f);

    /**
     * Mimics the vanilla model transformation used for most vanilla blocks,
     * and should be suitable for most custom block-like models.
     */
    public static final ItemTransforms MODEL_TRANSFORM_BLOCK = new ItemTransforms(TRANSFORM_BLOCK_3RD_PERSON_RIGHT, TRANSFORM_BLOCK_3RD_PERSON_RIGHT, TRANSFORM_BLOCK_1ST_PERSON_LEFT, TRANSFORM_BLOCK_1ST_PERSON_RIGHT, ItemTransform.NO_TRANSFORM, TRANSFORM_BLOCK_GUI, TRANSFORM_BLOCK_GROUND, TRANSFORM_BLOCK_FIXED);
}
