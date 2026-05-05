package com.lowdragmc.lowdraglib2.core.mixins.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.event.ContainerMenuEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void ldlib2$onInit(Inventory playerInventory, boolean active, Player owner, CallbackInfo ci) {
        if (owner.level().isClientSide) {
            // Client-safe scheduling
            Platform.executeOnClient(() -> {
                NeoForge.EVENT_BUS.post(
                        new ContainerMenuEvent.Create(owner, (InventoryMenu)(Object)this)
                );
            });
        } else {
            // Server-safe scheduling
            Platform.executeOnServer(() -> {
                NeoForge.EVENT_BUS.post(
                        new ContainerMenuEvent.Create(owner, (InventoryMenu)(Object)this)
                );
            });
        }
    }
}
