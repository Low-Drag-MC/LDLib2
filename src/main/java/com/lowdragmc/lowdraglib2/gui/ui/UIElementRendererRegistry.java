package com.lowdragmc.lowdraglib2.gui.ui;

import java.util.HashMap;
import java.util.Map;

public final class UIElementRendererRegistry {
    private static final UIElementRenderer<UIElement> DEFAULT_RENDERER = new UIElementRenderer<>() {
    };
    private static final Map<Class<?>, UIElementRenderer<?>> RENDERERS = new HashMap<>();
    private static final Map<Class<?>, UIElementRenderer<?>> RESOLVED_RENDERERS = new HashMap<>();

    private UIElementRendererRegistry() {
    }

    public static <T extends UIElement> void register(Class<T> type, UIElementRenderer<? super T> renderer) {
        RENDERERS.put(type, renderer);
        RESOLVED_RENDERERS.clear();
    }

    public static void clear() {
        RENDERERS.clear();
        RESOLVED_RENDERERS.clear();
    }

    public static UIElementRenderer<UIElement> defaultRenderer() {
        return DEFAULT_RENDERER;
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIElement> UIElementRenderer<? super T> findParentRendererByType(Class<? extends UIElement> type) {
        var current = type.getSuperclass();
        if (UIElement.class.isAssignableFrom(current)) {
            return findRendererByType((Class<? extends UIElement>) current);
        }
        return DEFAULT_RENDERER;
    }

    public static <T extends UIElement> UIElementRenderer<? super T> findRenderer(T element) {
        return findRendererByType(element.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIElement> UIElementRenderer<? super T> findRendererByType(Class<? extends T> type) {
        var resolved = RESOLVED_RENDERERS.get(type);
        if (resolved != null) {
            return (UIElementRenderer<? super T>) resolved;
        }
        Class<?> current = type;
        while (current != null && UIElement.class.isAssignableFrom(current)) {
            var renderer = RENDERERS.get(current);
            if (renderer != null) {
                RESOLVED_RENDERERS.put(type, renderer);
                return (UIElementRenderer<? super T>) renderer;
            }
            current = current.getSuperclass();
        }
        RESOLVED_RENDERERS.put(type, DEFAULT_RENDERER);
        return DEFAULT_RENDERER;
    }
}
