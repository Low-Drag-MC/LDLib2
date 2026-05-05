package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.utils.CustomResourcePack;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mixin(WorldLoader.PackConfig.class)
public abstract class PackConfigMixin {

    @ModifyVariable(method = "createResourceManager",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/MultiPackResourceManager;<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V",
                    by = 1
            ), name = "openedPacks")
    private List<PackResources> injectCreateReload(List<PackResources> openedPacks) {
        var mutableList = new ArrayList<>(openedPacks);
        mutableList.add(new CustomResourcePack(new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID), LDLib2.MOD_ID, PackType.SERVER_DATA));
        return mutableList;
    }

}
