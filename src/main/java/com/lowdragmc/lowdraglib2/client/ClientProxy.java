package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.CommonProxy;
import net.minecraft.client.resources.model.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientProxy extends CommonProxy {

    public ClientProxy(IEventBus eventBus, ModContainer modContainer) {
        super(eventBus);
        eventBus.register(new ClientModBusEventListener());
        modContainer.registerConfig(ModConfig.Type.CLIENT, LDLibClientConfig.SPEC);
        // Without a screen factory NeoForge shows no Config button for the mod in the mod list, leaving the
        // file as the only way in. ConfigurationScreen builds the screen from the spec.
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @Override
    public void init(IEventBus eventBus) {
        LDLib2ClientRegistries.init();
        super.init(eventBus);
    }
}
