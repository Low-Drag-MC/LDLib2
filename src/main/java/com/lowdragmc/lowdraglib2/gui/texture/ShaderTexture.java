package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import net.minecraft.resources.Identifier;

@KJSBindings
@LDLRegisterClient(name = "shader_texture", registry = "ldlib2:gui_texture")
public class ShaderTexture extends TransformTexture implements AutoCloseable {
    public ShaderTexture() {
        this(LDLib2.id("fbm"));
    }

    public ShaderTexture(Identifier shaderLocation) {
        // todo
    }

    @Override
    public void close() {
    }
}
