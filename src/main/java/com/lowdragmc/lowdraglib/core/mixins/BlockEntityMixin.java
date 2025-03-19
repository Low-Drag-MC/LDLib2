package com.lowdragmc.lowdraglib.core.mixins;

import com.lowdragmc.lowdraglib.networking.s2c.SPacketAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IManagedBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote BlockEntityMixin
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

    @Inject(method = "getUpdateTag", at = @At(value = "RETURN"))
    private void injectGetUpdateTag(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        if (this instanceof IAutoSyncBlockEntity autoSyncBlockEntity) {
            var tag = cir.getReturnValue();
            tag.put(autoSyncBlockEntity.getSyncTag(), autoSyncBlockEntity.serializeInitialData());
        }
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    private void injectSaveAdditional(CompoundTag pTag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this instanceof IAutoPersistBlockEntity autoPersistBlockEntity) {
            autoPersistBlockEntity.saveManagedPersistentData(pTag, false);
        }
    }

    @Inject(method = "loadAdditional", at = @At(value = "RETURN"))
    private void injectLoad(CompoundTag pTag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this instanceof IAutoSyncBlockEntity autoSyncBlockEntity && pTag.get(autoSyncBlockEntity.getSyncTag()) instanceof CompoundTag tag) {
            autoSyncBlockEntity.deserializeInitialData(tag);
        } else if (this instanceof IAutoPersistBlockEntity autoPersistBlockEntity) {
            autoPersistBlockEntity.loadManagedPersistentData(pTag);
        }
    }

    @Inject(method = "setRemoved", at = @At(value = "RETURN"))
    private void injectSetRemoved(CallbackInfo ci) {
        if (this instanceof IAsyncAutoSyncBlockEntity autoSyncBlockEntity) {
            autoSyncBlockEntity.onInValid();
        }
    }

    @Inject(method = "clearRemoved", at = @At(value = "RETURN"))
    private void injectClearRemoved(CallbackInfo ci) {
        if (this instanceof IManagedBlockEntity managed) {
            managed.getRootStorage().init();
            if (managed instanceof IAsyncAutoSyncBlockEntity autoSyncBlockEntity) {
                autoSyncBlockEntity.onValid();
            }
        }
    }

}
