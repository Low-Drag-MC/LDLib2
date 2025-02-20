package com.lowdragmc.lowdraglib.kjs.fabric;

import com.lowdragmc.lowdraglib.side.fluid.fabric.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.side.item.fabric.ItemTransferHelperImpl;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class LDLibKubeJSPluginImpl {
    public static void registerPlatformBindings(BindingsEvent event) {
        event.add("FluidTransferHelper", FluidTransferHelperImpl.class);
        event.add("ItemTransferHelper", ItemTransferHelperImpl.class);
    }
}
