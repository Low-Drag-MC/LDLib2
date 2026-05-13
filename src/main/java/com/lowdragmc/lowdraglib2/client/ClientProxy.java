package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.CommonProxy;
import net.minecraft.client.resources.model.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.*;

public class ClientProxy extends CommonProxy {

    public ClientProxy(IEventBus eventBus) {
        super(eventBus);
        eventBus.register(new ClientModBusEventListener());
    }

    @Override
    public void init(IEventBus eventBus) {
        LDLib2ClientRegistries.init();
        super.init(eventBus);
    }
}
