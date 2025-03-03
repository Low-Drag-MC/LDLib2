package com.lowdragmc.lowdraglib.gui.factory;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.core.mixins.accessor.ServerPlayerAccessor;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.networking.s2c.SPacketUIOpen;
import com.lowdragmc.lowdraglib.side.ForgeEventHooks;
import dev.latvian.mods.rhino.util.HideFromJS;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public abstract class UIFactory<T> {
    public final ResourceLocation uiFactoryId;
    public static final Map<ResourceLocation, UIFactory<?>> FACTORIES = new HashMap<>();

    public UIFactory(ResourceLocation uiFactoryId){
        this.uiFactoryId = uiFactoryId;
    }
    
    public static void register(UIFactory<?> factory) {
        FACTORIES.put(factory.uiFactoryId, factory);
    }

    @HideFromJS
    public final boolean openUI(T holder, ServerPlayer player) {
        ModularUI uiTemplate = createUITemplate(holder, player);
        if (uiTemplate == null) return false;
        uiTemplate.initWidgets();

        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        ((ServerPlayerAccessor)player).callNextContainerCounter();
        int currentWindowId = ((ServerPlayerAccessor)player).getContainerCounter();

        RegistryFriendlyByteBuf serializedHolder = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.server.registryAccess());
        writeHolderToSyncData(serializedHolder, holder);
        ModularUIContainer container = new ModularUIContainer(uiTemplate, currentWindowId);

        //accumulate all initial updates of widgets in open packet
        uiTemplate.mainGroup.writeInitialData(serializedHolder);

        PacketDistributor.sendToPlayer(player, new SPacketUIOpen(uiFactoryId, serializedHolder, currentWindowId));

        ((ServerPlayerAccessor)player).callInitMenu(container);
        player.containerMenu = container;

        //and fire forge event only in the end
        if (Platform.isForge()) {
            ForgeEventHooks.postPlayerContainerEvent(player, container);
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public final void initClientUI(RegistryFriendlyByteBuf serializedHolder, int windowId) {
        T holder = readHolderFromSyncData(serializedHolder);
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer entityPlayer = minecraft.player;

        ModularUI uiTemplate = createUITemplate(holder, entityPlayer);
        if (uiTemplate == null) return;
        uiTemplate.initWidgets();
        ModularUIGuiContainer container = new ModularUIGuiContainer(uiTemplate, windowId);
        uiTemplate.mainGroup.readInitialData(serializedHolder);
        minecraft.setScreen(container);
        minecraft.player.containerMenu = container.getMenu();
    }

    protected abstract ModularUI createUITemplate(T holder, Player entityPlayer);

    @OnlyIn(Dist.CLIENT)
    protected abstract T readHolderFromSyncData(RegistryFriendlyByteBuf syncData);

    protected abstract void writeHolderToSyncData(RegistryFriendlyByteBuf syncData, T holder);

}
