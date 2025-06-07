package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.configurator.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib2.editor_outdated.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib2.editor_outdated.data.IProject;
import com.lowdragmc.lowdraglib2.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.test.ui.IUITest;
import com.lowdragmc.lowdraglib2.utils.TypeAdapter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLib2Registries {
    public final static AutoRegistry.LDLibRegister<TypeAdapter.ITypeAdapter, TypeAdapter.ITypeAdapter> TYPE_ADAPTERS = AutoRegistry.LDLibRegister
            .create(LDLib2.id("type_adapter"), TypeAdapter.ITypeAdapter.class, AutoRegistry::noArgsInstance);

    public final static AutoRegistry.LDLibRegister<BaseNode, Supplier<BaseNode>> GRAPH_NODES = AutoRegistry.LDLibRegister
            .create(LDLib2.id("graph_node"), BaseNode.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegister<IConfigurableWidget, Supplier<IConfigurableWidget>> WIDGETS = AutoRegistry.LDLibRegister
            .create(LDLib2.id("widget"), IConfigurableWidget.class, AutoRegistry::noArgsCreator);

    @OnlyIn(Dist.CLIENT)
    public final static AutoRegistry.LDLibRegisterClient<IConfiguratorAccessor, IConfiguratorAccessor<?>> CONFIGURATOR_ACCESSORS = AutoRegistry.LDLibRegisterClient
            .create(LDLib2.id("configurator_accessor"), IConfiguratorAccessor.class, AutoRegistry::noArgsInstance);

    @OnlyIn(Dist.CLIENT)
    public final static AutoRegistry.LDLibRegisterClient<IGuiTexture, Supplier<IGuiTexture>> GUI_TEXTURES = AutoRegistry.LDLibRegisterClient
            .create(LDLib2.id("gui_texture"), IGuiTexture.class, AutoRegistry::noArgsCreator);

    @OnlyIn(Dist.CLIENT)
    public final static AutoRegistry.LDLibRegisterClient<IProject, Supplier<IProject>> PROJECTS = AutoRegistry.LDLibRegisterClient
            .create(LDLib2.id("project"), IProject.class, AutoRegistry::noArgsCreator);

    @OnlyIn(Dist.CLIENT)
    public final static AutoRegistry.LDLibRegisterClient<IRenderer, Supplier<IRenderer>> RENDERERS = AutoRegistry.LDLibRegisterClient
            .create(LDLib2.id("renderer"), IRenderer.class, AutoRegistry::noArgsCreator);

    @OnlyIn(Dist.CLIENT)
    public final static AutoRegistry.LDLibRegisterClient<IUITest, Supplier<IUITest>> UI_TESTS;

    static {
        if (LDLib2.isClient() && Platform.isDevEnv()) {
            UI_TESTS = AutoRegistry.LDLibRegisterClient.create(LDLib2.id("ui_test"), IUITest.class, AutoRegistry::noArgsCreator);
        } else {
            UI_TESTS = null;
        }
    }

    public static void init() {
        if (LDLib2.isClient()) {
            GUI_TEXTURES.register("empty", AutoRegistry.Holder.of(IGuiTexture.EmptyTexture.class.getAnnotation(LDLRegisterClient.class), IGuiTexture.EmptyTexture.class, () -> IGuiTexture.EMPTY));
        }
    }
}
