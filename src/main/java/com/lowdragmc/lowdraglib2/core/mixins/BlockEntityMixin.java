package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.holder.IManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.IPersistManagedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.ISyncMangedHolder;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.jetbrains.annotations.Nullable;

/**
 * @author KilaBash
 * @date 2022/11/27
 * @implNote BlockEntityMixin
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

    @Shadow
    @Nullable
    public abstract Level getLevel();

    @Inject(method = "getUpdateTag", at = @At(value = "RETURN"))
    private void injectGetUpdateTag(HolderLookup.Provider provider, CallbackInfoReturnable<CompoundTag> cir) {
        if (this instanceof ISyncMangedHolder syncMangedHolder) {
            var tag = cir.getReturnValue();
            tag.put(syncMangedHolder.getSyncTag(), syncMangedHolder.serializeInitialData(provider));
        }
    }

    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    private void injectSaveAdditional(ValueOutput output, CallbackInfo ci) {
        if (this instanceof IPersistManagedHolder persistManagedHolder) {
            persistManagedHolder.saveManagedPersistentData(output, false);
        }
    }

    @Inject(method = "loadAdditional", at = @At(value = "RETURN"))
    private void injectLoad(ValueInput input, CallbackInfo ci) {
        if (this instanceof ISyncMangedHolder syncMangedHolder) {
            var initial = input.read(syncMangedHolder.getSyncTag(), CompoundTag.CODEC);
            if (initial.isPresent()) {
                HolderLookup.Provider provider;
                if (input instanceof TagValueInput tagInput) {
                    provider = tagInput.context.lookup();
                } else {
                    provider = Platform.getFrozenRegistry();
                }
                syncMangedHolder.deserializeInitialData(provider, initial.get());
                return;
            }
        }
        if (this instanceof IPersistManagedHolder persistManagedHolder) {
            persistManagedHolder.loadManagedPersistentData(input);
        }
    }

    @Inject(method = "setRemoved", at = @At(value = "RETURN"))
    private void injectSetRemoved(CallbackInfo ci) {
        if (this instanceof ISyncPersistRPCBlockEntity syncMangedHolder && getLevel() instanceof ServerLevel) {
            syncMangedHolder.detachAsyncLogic();
        }
    }

    @Inject(method = "clearRemoved", at = @At(value = "RETURN"))
    private void injectClearRemoved(CallbackInfo ci) {
        if (this instanceof IManagedHolder managed) {
            managed.getRootStorage().requireInit();
            if (managed instanceof ISyncMangedHolder syncMangedHolder && getLevel() instanceof ServerLevel) {
                syncMangedHolder.attachAsyncLogic();
            }
        }
    }

}
