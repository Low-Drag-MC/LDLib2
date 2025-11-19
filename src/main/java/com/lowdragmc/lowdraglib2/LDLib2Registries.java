package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceProviderType;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.LDLRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.IMenuTest;
import com.lowdragmc.lowdraglib2.test.ui.IScreenTest;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLib2Registries {
    public final static AutoRegistry.LDLibRegister<UIElement, Supplier<UIElement>> UI_ELEMENTS = AutoRegistry.LDLibRegister
            .create(LDLib2.id("ui_element"), UIElement.class, AutoRegistry::noArgsCreator);

    public final static LDLRegistry.String<ResourceProviderType> RESOURCE_PROVIDER_TYPES = new LDLRegistry.String<>(LDLib2.id("resource_provider_types"));

    public static AutoRegistry.LDLibRegister<IMenuTest, Supplier<IMenuTest>> MENU_TESTS;

    @OnlyIn(Dist.CLIENT)
    public static AutoRegistry.LDLibRegisterClient<IConfiguratorAccessor, IConfiguratorAccessor<?>> CONFIGURATOR_ACCESSORS;

    @OnlyIn(Dist.CLIENT)
    public static AutoRegistry.LDLibRegisterClient<IGuiTexture, Supplier<IGuiTexture>> GUI_TEXTURES;

    @OnlyIn(Dist.CLIENT)
    public static AutoRegistry.LDLibRegisterClient<IRenderer, Supplier<IRenderer>> RENDERERS;

    @OnlyIn(Dist.CLIENT)
    public static AutoRegistry.LDLibRegisterClient<IScreenTest, Supplier<IScreenTest>> SCREEN_TESTS;

    static {
        if (LDLib2.isClient()) {
            CONFIGURATOR_ACCESSORS = AutoRegistry.LDLibRegisterClient
                    .create(LDLib2.id("configurator_accessor"), IConfiguratorAccessor.class, AutoRegistry::noArgsInstance);
            GUI_TEXTURES = AutoRegistry.LDLibRegisterClient
                    .create(LDLib2.id("gui_texture"), IGuiTexture.class, AutoRegistry::noArgsCreator);
            GUI_TEXTURES.setMissingKey("missing");
            RENDERERS = AutoRegistry.LDLibRegisterClient
                    .create(LDLib2.id("renderer"), IRenderer.class, AutoRegistry::noArgsCreator);
            if (Platform.isDevEnv()) {
                SCREEN_TESTS = AutoRegistry.LDLibRegisterClient.create(LDLib2.id("screen_test"), IScreenTest.class, AutoRegistry::noArgsCreator);
            }
        }
        if (Platform.isDevEnv()) {
            MENU_TESTS = AutoRegistry.LDLibRegister.create(LDLib2.id("menu_test"), IMenuTest.class, AutoRegistry::noArgsCreator);
            for (var menuTest : MENU_TESTS) {
                PlayerUIMenuType.register(LDLib2.id(menuTest.annotation().name()), player -> {
                    var test = menuTest.value().get();
                    test.init(player);
                    return test;
                });
            }
        }
    }

    public static void init() {
        if (LDLib2.isClient()) {
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

        RESOURCE_PROVIDER_TYPES.register(BuiltinResourceProvider.TYPE.getTypeName(), BuiltinResourceProvider.TYPE);
        RESOURCE_PROVIDER_TYPES.register(FileResourceProvider.TYPE.getTypeName(), FileResourceProvider.TYPE);
    }
}
