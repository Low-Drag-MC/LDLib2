package com.lowdragmc.lowdraglib2;

import net.fabricmc.api.ClientModInitializer;
import com.lowdragmc.lowdraglib2.client.ClientProxy;

public class LDLib2Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientProxy.register();
    }
}
