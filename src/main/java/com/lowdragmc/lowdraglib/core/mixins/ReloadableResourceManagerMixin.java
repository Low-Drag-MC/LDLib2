package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib.utils.CustomResourcePack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ReloadableResourceManager.class)
public abstract class ReloadableResourceManagerMixin {

    @ModifyVariable(method = "createReload", at = @At("HEAD"), index = 4, argsOnly = true)
    private List<PackResources> injectCreateReload(List<PackResources> resourcePacks) {
        if (LDLib.isClient()) {
            lowDragLib$injectClientResourcePack();
        }
        
        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(LDLib.getAssetsDir(), LDLib.MOD_ID, PackType.CLIENT_RESOURCES));

        return mutableList;
    }

    @Unique
    @OnlyIn(Dist.CLIENT)
    private static void lowDragLib$injectClientResourcePack() {
        LDLMetadataSection.clearCache();
        if (LDLib.isRemote()) {
            ShaderTexture.clearCache();
        }
    }

}
