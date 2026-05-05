package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.obj.ObjGeometry;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ObjGeometry.class)
public interface ObjGeometryAccessor {
    @Invoker
    Pair<BakedQuad, Direction> invokeMakeQuad(ModelBaker baker,
                                              int[][] indices,
                                              int tintIndex,
                                              Vector4f colorTint,
                                              Vector4f ambientColor,
                                              Material.Baked material,
                                              Transparency transparency,
                                              Transformation transform);
}
