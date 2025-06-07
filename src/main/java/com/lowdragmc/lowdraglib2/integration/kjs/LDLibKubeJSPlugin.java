package com.lowdragmc.lowdraglib2.integration.kjs;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.editor_outdated.data.UIProject;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.widget.*;
import com.lowdragmc.lowdraglib2.gui.widget.layout.PhantomTankWidget;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.BlockUIJSFactory;
import com.lowdragmc.lowdraglib2.integration.kjs.ui.ItemUIJSFactory;
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
        event.add("BlockUIFactory", BlockUIJSFactory.class);
        event.add("ItemUIFactory", ItemUIJSFactory.class);
        event.add("UIProject", UIProject.class);
        // texture
        event.add("ResourceTexture", ResourceTexture.class);
        event.add("FillDirection", ProgressTexture.FillDirection.class);
        event.add("ProgressTexture", ProgressTexture.class);
        event.add("AnimationTexture", AnimationTexture.class);
        event.add("ColorRectTexture", ColorRectTexture.class);
        event.add("ColorRectTexture", ColorRectTexture.class);
        event.add("ItemStackTexture", ItemStackTexture.class);
        event.add("ResourceBorderTexture", ResourceBorderTexture.class);
        event.add("ShaderTexture", ShaderTexture.class);
        event.add("TextTexture", TextTexture.class);
        event.add("GuiTextureGroup", GuiTextureGroup.class);
        event.add("ColorPattern", ColorPattern.class);
        event.add("TextType", TextTexture.TextType.class);
        // LDLib Widget
        event.add("ModularUI", ModularUI.class);
        event.add("ButtonWidget", ButtonWidget.class);
        event.add("DialogWidget", DialogWidget.class);
        event.add("DraggableScrollableWidgetGroup", DraggableScrollableWidgetGroup.class);
        event.add("DraggableWidgetGroup", DraggableWidgetGroup.class);
        event.add("ImageWidget", ImageWidget.class);
        event.add("LabelWidget", LabelWidget.class);
        event.add("PhantomTankWidget", PhantomTankWidget.class);
        event.add("PhantomSlotWidget", PhantomSlotWidget.class);
        event.add("SceneWidget", SceneWidget.class);
        event.add("SelectableWidgetGroup", SelectableWidgetGroup.class);
        event.add("SlotWidget", SlotWidget.class);
        event.add("SwitchWidget", SwitchWidget.class);
        event.add("TabButton", TabButton.class);
        event.add("TabContainer", TabContainer.class);
        event.add("TankWidget", TankWidget.class);
        event.add("TextBoxWidget", TextBoxWidget.class);
        event.add("TextFieldWidget", TextFieldWidget.class);
        event.add("TreeListWidget", TreeListWidget.class);
        event.add("WidgetGroup", WidgetGroup.class);
        event.add("Widget", Widget.class);
        event.add("ProgressWidget", ProgressWidget.class);
        // math
        event.add("Vector3f", Vector3f.class);
        event.add("GuiSize", Size.class);
        event.add("GuiPos", Position.class);
    }

}
