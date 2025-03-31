package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.utils.CustomResourcePack;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldLoader.PackConfig.class)
public abstract class PackConfigMixin {

    @ModifyVariable(method = "createResourceManager",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/packs/resources/MultiPackResourceManager;<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V",
                    shift = At.Shift.BEFORE,
                    by = 1
    ))
    private List<PackResources> injectCreateReload(List<PackResources> resourcePacks) {
        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(LDLib.getAssetsDir(), LDLib.MOD_ID, PackType.SERVER_DATA));
        return mutableList;
    }

}
