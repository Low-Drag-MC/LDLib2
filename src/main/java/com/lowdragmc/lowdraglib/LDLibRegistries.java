package com.lowdragmc.lowdraglib;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.editor.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.editor.data.IProject;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.editor.ui.menu.MenuTab;
import com.lowdragmc.lowdraglib.graphprocessor.data.BaseNode;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.registry.AutoRegistry;
import com.lowdragmc.lowdraglib.test.ui.IUITest;
import com.lowdragmc.lowdraglib.utils.TypeAdapter;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLibRegistries {
    public final static AutoRegistry<ConfigAccessor, IConfiguratorAccessor, IConfiguratorAccessor<?>> CONFIG_ACCESSORS =
            AutoRegistry.create(LDLib.id("config_accessor"), ConfigAccessor.class,
                    IConfiguratorAccessor.class, null, null, AutoRegistry::noArgsInstance, null);

    public final static AutoRegistry.LDLibRegister<TypeAdapter.ITypeAdapter, TypeAdapter.ITypeAdapter> TYPE_ADAPTERS = AutoRegistry.LDLibRegister
            .create(LDLib.id("type_adapter"), TypeAdapter.ITypeAdapter.class, AutoRegistry::noArgsInstance);

    public final static AutoRegistry.LDLibRegister<BaseNode, Supplier<BaseNode>> GRAPH_NODES = AutoRegistry.LDLibRegister
            .create(LDLib.id("graph_node"), BaseNode.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegister<IConfigurableWidget, Supplier<IConfigurableWidget>> WIDGETS = AutoRegistry.LDLibRegister
            .create(LDLib.id("widget"), IConfigurableWidget.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<IGuiTexture, Supplier<IGuiTexture>> GUI_TEXTURES = AutoRegistry.LDLibRegisterClient
            .create(LDLib.id("gui_texture"), IGuiTexture.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<Resource, Supplier<Resource>> RESOURCES = AutoRegistry.LDLibRegisterClient
            .create(LDLib.id("resource"), Resource.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<MenuTab, Supplier<MenuTab>> MENU_TABS = AutoRegistry.LDLibRegisterClient
            .create(LDLib.id("menu_tab"), MenuTab.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<IProject, Supplier<IProject>> PROJECTS = AutoRegistry.LDLibRegisterClient
            .create(LDLib.id("project"), IProject.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<IRenderer, Supplier<IRenderer>> RENDERERS = AutoRegistry.LDLibRegisterClient
            .create(LDLib.id("renderer"), IRenderer.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegisterClient<IUITest, Supplier<IUITest>> UI_TESTS;
    static {
        if (LDLib.isClient() && Platform.isDevEnv()) {
            UI_TESTS = AutoRegistry.LDLibRegisterClient.create(LDLib.id("ui_test"), IUITest.class, AutoRegistry::noArgsCreator);
        } else {
            UI_TESTS = null;
        }
    }
}
