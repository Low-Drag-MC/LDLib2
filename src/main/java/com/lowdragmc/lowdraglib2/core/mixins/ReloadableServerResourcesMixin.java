package com.lowdragmc.lowdraglib2.core.mixins;

import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(value = ReloadableServerResources.class, priority = 100)
public abstract class ReloadableServerResourcesMixin {
	@Inject(method = "loadResources", at = @At("HEAD"))
	private static void ldlib2$captureEarlyRegistries(ResourceManager resourceManager,
													  LayeredRegistryAccess<RegistryLayer> contextLayers,
													  List<Registry.PendingTags<?>> updatedContextTags,
													  FeatureFlagSet enabledFeatures,
													  Commands.CommandSelection commandSelection,
													  PermissionSet functionCompilationPermissions,
													  Executor backgroundExecutor,
													  Executor mainThreadExecutor,
													  CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
		Platform.SERVER_REGISTRY_ACCESS = contextLayers.compositeAccess();
	}
}
