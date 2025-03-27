package com.lowdragmc.lowdraglib;

import com.lowdragmc.lowdraglib.editor.accessors.IConfiguratorAccessor;
import com.lowdragmc.lowdraglib.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.registry.AutoRegistry;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class LDLibRegistries {
    public final static AutoRegistry<ConfigAccessor, IConfiguratorAccessor, IConfiguratorAccessor<?>> CONFIG_ACCESSORS =
            AutoRegistry.create(LDLib.location("config_accessor"), ConfigAccessor.class,
                    IConfiguratorAccessor.class, null, null, AutoRegistry::noArgsInstance, null);

    public final static AutoRegistry.LDLibRegister<IGuiTexture, Supplier<IGuiTexture>> GUI_TEXTURES = AutoRegistry.LDLibRegister
            .create(LDLib.location("gui_texture"), IGuiTexture.class, AutoRegistry::noArgsCreator);

    public final static AutoRegistry.LDLibRegister<Resource, Supplier<Resource>> RESOURCES = AutoRegistry.LDLibRegister
            .create(LDLib.location("resource"), Resource.class, AutoRegistry::noArgsCreator);
}
