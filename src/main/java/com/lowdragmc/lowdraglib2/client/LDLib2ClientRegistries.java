package com.lowdragmc.lowdraglib2.client;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.rendering.RegisteredGuiTextureRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.RegisteredUIElementRenderer;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLib2ClientRegistries {
    public static AutoRegistry.LDLibRegisterClient<IConfiguratorAccessor, IConfiguratorAccessor<?>> CONFIGURATOR_ACCESSORS = AutoRegistry.LDLibRegisterClient
            .create(
                    LDLib2.id("configurator_accessor"),
                    IConfiguratorAccessor.class,
                    AutoRegistry::noArgsInstance
            );

    public static AutoRegistry.LDLibRegisterClient<IGuiTexture, Supplier<IGuiTexture>> GUI_TEXTURES = AutoRegistry.LDLibRegisterClient
            .create(
                    LDLib2.id("gui_texture"),
                    IGuiTexture.class,
                    AutoRegistry::noArgsCreator
            );

    public static AutoRegistry.LDLibRegisterClient<RegisteredGuiTextureRenderer, RegisteredGuiTextureRenderer> GUI_TEXTURE_RENDERER_ENTRIES = AutoRegistry.LDLibRegisterClient
            .create(
                    LDLib2.id("gui_texture_renderer"),
                    RegisteredGuiTextureRenderer.class,
                    AutoRegistry::noArgsInstance
            );

    public static AutoRegistry.LDLibRegisterClient<IRenderer, Supplier<IRenderer>> RENDERERS = AutoRegistry.LDLibRegisterClient
            .create(
                    LDLib2.id("renderer"),
                    IRenderer.class,
                    AutoRegistry::noArgsCreator
            );

    public static AutoRegistry.LDLibRegisterClient<RegisteredUIElementRenderer, RegisteredUIElementRenderer> UI_ELEMENT_RENDERER_ENTRIES = AutoRegistry.LDLibRegisterClient
            .create(
                    LDLib2.id("ui_element_renderer"),
                    RegisteredUIElementRenderer.class,
                    AutoRegistry::noArgsInstance
            );

    @Nullable
    public static AutoRegistry.LDLibRegisterClient<IScreenTest, Supplier<IScreenTest>> SCREEN_TESTS;

    static {
        GUI_TEXTURES.setMissingKey("missing");
        if (Platform.isDevEnv()) {
            SCREEN_TESTS = AutoRegistry.LDLibRegisterClient.create(LDLib2.id("screen_test"), IScreenTest.class, AutoRegistry::noArgsCreator);
        }
    }

    public static void init() {
        GUI_TEXTURES.register("empty", AutoRegistry.Holder.of(
                IGuiTexture.EmptyTexture.class.getAnnotation(LDLRegisterClient.class),
                IGuiTexture.EmptyTexture.class,
                () -> IGuiTexture.EMPTY));
        GUI_TEXTURES.register("missing", AutoRegistry.Holder.of(
                IGuiTexture.MissingTexture.class.getAnnotation(LDLRegisterClient.class),
                IGuiTexture.MissingTexture.class,
                () -> IGuiTexture.MISSING_TEXTURE));
        RENDERERS.register("empty", AutoRegistry.Holder.of(
                IRenderer.EmptyRenderer.class.getAnnotation(LDLRegisterClient.class),
                IRenderer.EmptyRenderer.class,
                () -> IRenderer.EMPTY));
    }
}
