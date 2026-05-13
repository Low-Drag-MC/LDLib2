package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.client.LDLib2ClientRegistries;

public final class GuiTextureClientRenderers {
    private static boolean initialized;

    private GuiTextureClientRenderers() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GuiTextureRendererBootstrap.applyRegistry(LDLib2ClientRegistries.GUI_TEXTURE_RENDERER_ENTRIES);
    }
}
