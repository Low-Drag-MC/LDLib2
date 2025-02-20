package com.lowdragmc.lowdraglib.kjs.forge;

import com.lowdragmc.lowdraglib.side.fluid.forge.FluidTransferHelperImpl;
import com.lowdragmc.lowdraglib.side.item.forge.ItemTransferHelperImpl;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public class LDLibKubeJSPluginImpl {
    public static void registerPlatformBindings(BindingsEvent event) {
        event.add("FluidTransferHelper", FluidTransferHelperImpl.class);
        event.add("ItemTransferHelper", ItemTransferHelperImpl.class);
    }
}
