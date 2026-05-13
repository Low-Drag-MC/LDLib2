package com.lowdragmc.lowdraglib2.gui.texture.rendering;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;

import java.util.HashMap;
import java.util.Map;

public final class GuiTextureRendererRegistry {
    private static final Map<Class<?>, GuiTextureRenderer<?>> RENDERERS = new HashMap<>();
    private static final Map<Class<?>, GuiTextureRenderer<?>> RESOLVED_RENDERERS = new HashMap<>();

    private GuiTextureRendererRegistry() {
    }

    public static <T extends IGuiTexture> void register(Class<T> type, GuiTextureRenderer<? super T> renderer) {
        RENDERERS.put(type, renderer);
        RESOLVED_RENDERERS.clear();
    }

    public static void clear() {
        RENDERERS.clear();
        RESOLVED_RENDERERS.clear();
    }

    public static GuiTextureRenderer<IGuiTexture> defaultRenderer() {
        return DefaultGuiTextureRenderer.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T extends IGuiTexture> GuiTextureRenderer<? super T> findRenderer(T texture) {
        var type = texture.getClass();
        var resolved = RESOLVED_RENDERERS.get(type);
        if (resolved != null) {
            return (GuiTextureRenderer<? super T>) resolved;
        }
        Class<?> current = type;
        while (current != null && IGuiTexture.class.isAssignableFrom(current)) {
            var renderer = RENDERERS.get(current);
            if (renderer != null) {
                RESOLVED_RENDERERS.put(type, renderer);
                return (GuiTextureRenderer<? super T>) renderer;
            }
            current = current.getSuperclass();
        }
        RESOLVED_RENDERERS.put(type, DefaultGuiTextureRenderer.INSTANCE);
        return (GuiTextureRenderer<? super T>) DefaultGuiTextureRenderer.INSTANCE;
    }
}
