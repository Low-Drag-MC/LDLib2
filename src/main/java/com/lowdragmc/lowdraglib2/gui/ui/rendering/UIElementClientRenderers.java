package com.lowdragmc.lowdraglib2.gui.ui.rendering;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;

public final class UIElementClientRenderers {
    private static boolean initialized;

    private UIElementClientRenderers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        UIElementRendererBootstrap.applyRegistry(LDLib2ClientRegistries.UI_ELEMENT_RENDERER_ENTRIES);
    }

}
