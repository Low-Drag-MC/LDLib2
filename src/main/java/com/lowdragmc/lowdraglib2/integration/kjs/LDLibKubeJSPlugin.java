package com.lowdragmc.lowdraglib2.integration.kjs;

import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.UIEvents;
import org.joml.Vector3f;

/**
 * @author KilaBash
 * @date 2023/3/26
 * @implNote GregTechKubeJSPlugin
 */
public class LDLibKubeJSPlugin implements KubeJSPlugin {


    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.lowdragmc.lowdraglib");
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(UIEvents.INSTANCE);
    }

    @Override
    public void registerBindings(BindingRegistry event) {
        // math
        event.add("Vector3f", Vector3f.class);
        event.add("GuiSize", Size.class);
        event.add("GuiPos", Position.class);
    }

}
