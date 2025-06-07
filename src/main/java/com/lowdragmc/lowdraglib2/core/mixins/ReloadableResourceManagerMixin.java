package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.model.custommodel.LDLMetadataSection;
import com.lowdragmc.lowdraglib2.gui.texture.ShaderTexture;
import com.lowdragmc.lowdraglib2.utils.CustomResourcePack;
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
        if (LDLib2.isClient()) {
            lowDragLib$injectClientResourcePack();
        }
        
        var mutableList = new ArrayList<>(resourcePacks);
        mutableList.add(new CustomResourcePack(Platform.getGamePath().toFile(), LDLib2.MOD_ID, PackType.CLIENT_RESOURCES));

        return mutableList;
    }

    @Unique
    @OnlyIn(Dist.CLIENT)
    private static void lowDragLib$injectClientResourcePack() {
        LDLMetadataSection.clearCache();
        if (LDLib2.isRemote()) {
            ShaderTexture.clearCache();
        }
    }

}
