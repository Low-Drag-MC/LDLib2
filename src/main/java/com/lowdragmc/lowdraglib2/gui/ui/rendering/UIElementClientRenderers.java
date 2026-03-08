package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UIElementClientRenderers {
    private static boolean initialized;

    private UIElementClientRenderers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UIElementRendererBootstrap.applyRegistry(LDLib2Registries.UI_ELEMENT_RENDERER_ENTRIES);
    }

}
