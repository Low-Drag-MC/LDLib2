package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ObjGeometryAccessor;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextMap;
import net.neoforged.neoforge.client.model.obj.ObjGeometry;
import net.neoforged.neoforge.client.model.obj.ObjMaterialLibrary;
import org.apache.commons.lang3.tuple.Pair;
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

@Mixin(targets = "net.neoforged.neoforge.client.model.obj.ObjGeometry$ModelMesh")
public abstract class ObjModelMixin {

    @Shadow @Final ObjGeometry this$0;

    @Shadow @Nullable public ObjMaterialLibrary.@Nullable Material mat;

    @Inject(method = "addQuads", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/client/model/obj/ObjGeometry;makeQuad([[IILorg/joml/Vector4f;Lorg/joml/Vector4f;Lnet/minecraft/client/renderer/block/model/BakedQuad$SpriteInfo;Lcom/mojang/math/Transformation;)Lorg/apache/commons/lang3/tuple/Pair;"))
    private void ldlib2$addQuads(
            QuadCollection.Builder builder,
            TextureSlots slots, ModelBaker baker,
            ModelState state, ModelDebugName debugName,
            ContextMap additionalProperties, CallbackInfo ci,
            @Local(name = "face") int[][] face, @Local(name = "spriteInfo") BakedQuad.SpriteInfo spriteInfo, @Local(name = "transform") Transformation transform) {
        if (this$0 instanceof ObjGeometryAccessor geometry) {
            var left = ldlib2$getLeftFaces(face);
            if (left.length >= 3) {
                ObjMaterialLibrary.Material mat = this.mat;
                assert mat != null;
                var tintIndex = mat.diffuseTintIndex;
                var colorTint = mat.diffuseColor;
                for (int[][] splitFaces : ldlib2$splitFaces(left)) {
                    Pair<BakedQuad, Direction> quad = geometry.invokeMakeQuad(splitFaces, tintIndex, colorTint, mat.ambientColor, spriteInfo, transform);
                    if (quad.getRight() == null)
                        builder.addUnculledFace(quad.getLeft());
                    else
                        builder.addCulledFace(quad.getRight(), quad.getLeft());
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
