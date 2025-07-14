package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.obj.ObjModel;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ObjModel.class)
public interface ObjModelAccessor {
    @Invoker
    Pair<BakedQuad, Direction> invokeMakeQuad(int[][] indices, int tintIndex, Vector4f colorTint, Vector4f ambientColor, TextureAtlasSprite texture, Transformation transform);
}
