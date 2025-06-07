package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = ReloadableServerResources.class, priority = 100)
public abstract class ReloadableServerResourcesMixin {
	@Inject(method = "loadResources", at = @At("HEAD"))
	private static void ldlib2$captureEarlyRegistries(ResourceManager resourceManager, LayeredRegistryAccess<RegistryLayer> access,
                             FeatureFlagSet flags, Commands.CommandSelection commands, int functionCompilationLevel,
                             Executor gameExecutor, Executor backgroundExecutor,
                             CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
		Platform.FROZEN_REGISTRY_ACCESS = access.compositeAccess();
	}
}
