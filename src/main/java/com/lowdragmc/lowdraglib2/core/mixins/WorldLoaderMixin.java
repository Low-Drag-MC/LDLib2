package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(WorldLoader.class)
public abstract class WorldLoaderMixin {
    @ModifyExpressionValue(method = "lambda$load$0", at = @At(value = "INVOKE", target = "Lcom/mojang/datafixers/util/Pair;getSecond()Ljava/lang/Object;"))
    private static <S> S ldlib2$loadResourceManager(S original) {
        if (original instanceof ResourceManager resourceManager) {
            Platform.RESOURCE_MANAGER = resourceManager;
        }
        return original;
    }

    @Inject(method = "lambda$load$3", at = @At(value = "HEAD"))
    private static void ldlib2$closeResourceManager(CloseableResourceManager resources,
                                                    ReloadableServerResources managers,
                                                    Throwable throwable,
                                                    CallbackInfo ci) {
        Platform.RESOURCE_MANAGER = null;
    }
}
