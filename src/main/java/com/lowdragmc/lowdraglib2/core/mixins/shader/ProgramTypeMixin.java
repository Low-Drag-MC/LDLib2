package com.lowdragmc.lowdraglib2.core.mixins.shader;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.ProgramTypeAccessor;
import com.mojang.blaze3d.shaders.Program;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Program.Type.class)
public class ProgramTypeMixin {
    @Shadow
    @Final
    @Mutable
    private static Program.Type[] $VALUES;

    static {
        LDLibShaders.GEOMETRY_TYPE = ProgramTypeAccessor.ldlib2$createProgramType(
                "GEOMETRY",
                $VALUES.length,
                "geometry",
                ".gsh",
                GL32.GL_GEOMETRY_SHADER);
        $VALUES = ArrayUtils.add($VALUES, LDLibShaders.GEOMETRY_TYPE);
    }
}