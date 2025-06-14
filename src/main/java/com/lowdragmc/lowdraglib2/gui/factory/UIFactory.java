package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.core.mixins.accessor.ServerPlayerAccessor;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUIContainer;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib2.networking.s2c.SPacketUIOpen;
import com.lowdragmc.lowdraglib2.utils.ByteBufUtil;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
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

        ModularUIContainer container = new ModularUIContainer(uiTemplate, currentWindowId);

        //accumulate all initial updates of widgets in open packet
        var data = ByteBufUtil.writeCustomData(buf -> {
            writeHolderToSyncData(buf, holder);
            uiTemplate.mainGroup.writeInitialData(buf);
        }, player.server.registryAccess());

        PacketDistributor.sendToPlayer(player, new SPacketUIOpen(uiFactoryId, data, currentWindowId));

        ((ServerPlayerAccessor)player).callInitMenu(container);
        player.containerMenu = container;

        NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, container));
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
