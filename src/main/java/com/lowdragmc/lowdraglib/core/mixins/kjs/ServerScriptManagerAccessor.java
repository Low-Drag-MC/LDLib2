package com.lowdragmc.lowdraglib.core.mixins.kjs;

import dev.latvian.mods.kubejs.server.ServerScriptManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerScriptManager.class)
public interface ServerScriptManagerAccessor {
    @Accessor
    static ServerScriptManager getStaticInstance() {
        throw new AssertionError();
    }
}
