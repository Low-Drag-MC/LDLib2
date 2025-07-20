package com.lowdragmc.lowdraglib2.core.mixins.accessor;

import com.mojang.blaze3d.shaders.Program;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Program.Type.class})
public interface ProgramTypeAccessor {
    @Invoker("<init>")
    static Program.Type ldlib2$createProgramType(String name, int ordinal, String typeName, String extension, int glId) {
        throw new AssertionError();
    }
}