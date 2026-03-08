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
    public static <T extends UIElement> UIElementRenderer<? super T> findParentRenderer(Class<? extends UIElement> type) {
        Class<?> current = type.getSuperclass();
        while (current != null && UIElement.class.isAssignableFrom(current)) {
            var renderer = RENDERERS.get(current);
            if (renderer != null) {
                return (UIElementRenderer<? super T>) renderer;
            }
            current = current.getSuperclass();
        }
        return DEFAULT_RENDERER;
    }

    @SuppressWarnings("unchecked")
    public static <T extends UIElement> UIElementRenderer<? super T> findRenderer(T element) {
        var type = element.getClass();
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
