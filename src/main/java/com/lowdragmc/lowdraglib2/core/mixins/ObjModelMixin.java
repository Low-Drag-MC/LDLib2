package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ObjModelAccessor;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelState;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.obj.ObjMaterialLibrary;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import net.neoforged.neoforge.client.model.renderable.CompositeRenderable;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

@Mixin(targets = "net.neoforged.neoforge.client.model.obj.ObjModel$ModelMesh")
public abstract class ObjModelMixin {

    @Shadow @Final ObjModel this$0;

    @Shadow @Nullable public ObjMaterialLibrary.@Nullable Material mat;

    @Inject(method = "bake", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/model/obj/ObjModel;makeQuad([[IILorg/joml/Vector4f;Lorg/joml/Vector4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/math/Transformation;)Lorg/apache/commons/lang3/tuple/Pair;"))
    private void ldlib2$bake(CompositeRenderable.PartBuilder<?> builder,
                             IGeometryBakingContext configuration,
                             CallbackInfo ci,
                             @Local List<BakedQuad> quads,
                             @Local int[][] faces) {
        if (this$0 instanceof ObjModelAccessor model) {
            var left = ldlib2$getLeftFaces(faces);
            if (left.length >= 3) {
                ObjMaterialLibrary.Material mat = this.mat;
                assert mat != null;
                var tintIndex = mat.diffuseTintIndex;
                var colorTint = mat.diffuseColor;
                for (int[][] splitFaces : ldlib2$splitFaces(left)) {
                    var quad = model.invokeMakeQuad(splitFaces, tintIndex, colorTint, mat.ambientColor, UnitTextureAtlasSprite.INSTANCE, Transformation.identity());
                    quads.add(quad.getLeft());
                }
            }
        }
    }

    @Inject(method = "addQuads", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/model/obj/ObjModel;makeQuad([[IILorg/joml/Vector4f;Lorg/joml/Vector4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lcom/mojang/math/Transformation;)Lorg/apache/commons/lang3/tuple/Pair;"))
    private void ldlib2$addQuads(IGeometryBakingContext owner,
                                 IModelBuilder<?> modelBuilder,
                                 Function<Material, TextureAtlasSprite> spriteGetter,
                                 ModelState modelTransform,
                                 CallbackInfo ci,
                                 @Local int[][] faces,
                                 @Local TextureAtlasSprite texture,
                                 @Local(name = "transform") Transformation transform) {
        if (this$0 instanceof ObjModelAccessor model) {
            var left = ldlib2$getLeftFaces(faces);
            if (left.length >= 3) {
                ObjMaterialLibrary.Material mat = this.mat;
                assert mat != null;
                var tintIndex = mat.diffuseTintIndex;
                var colorTint = mat.diffuseColor;
                for (int[][] splitFaces : ldlib2$splitFaces(left)) {
                    var quad = model.invokeMakeQuad(splitFaces, tintIndex, colorTint, mat.ambientColor, texture, transform);
                    if (quad.getRight() == null)
                        modelBuilder.addUnculledFace(quad.getLeft());
                    else
                        modelBuilder.addCulledFace(quad.getRight(), quad.getLeft());
                }
            }
        }
    }

    @Unique
    private int[][] ldlib2$getLeftFaces(int[][] faces) {
        if (faces.length <= 4) return new int[0][];
        var left = new int[faces.length - 2][];
        left[0] = faces[3];
        System.arraycopy(faces, 4, left, 1, faces.length - 4);
        left[left.length - 1] = faces[0];
        return left;
    }

    @Unique
    private List<int[][]> ldlib2$splitFaces(int[][] faces) {
        int n = faces.length;
        List<int[][]> parts = new ArrayList<>();

        if (n <= 4) {
            parts.add(Arrays.copyOf(faces, n));
            return parts;
        }

        // fill 4 points first
        int remainder = n % 4;           // get left point
        int limit = n - remainder;       // quad index

        for (int i = 0; i < limit; i += 4) {
            parts.add(Arrays.copyOfRange(faces, i, i + 4));
        }

        // process left points
        switch (remainder) {
            case 3 ->
                    parts.add(Arrays.copyOfRange(faces, limit, n));

            case 2 ->
                    parts.add(new int[][] {
                            faces[limit - 1], faces[limit], faces[limit + 1], faces[0]
                    });

            case 1 ->
                    parts.add(new int[][] {
                            faces[limit - 1], faces[limit], faces[0]
                    });

            default -> { /* remainder == 0，do nothing */ }
        }

        return parts;
    }
}
