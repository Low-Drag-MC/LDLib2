package com.lowdragmc.lowdraglib.kjs;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.data.UIProject;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.kjs.ui.BlockUIJSFactory;
import com.lowdragmc.lowdraglib.kjs.ui.ItemUIJSFactory;
import com.lowdragmc.lowdraglib.kjs.ui.UIEvents;
import com.lowdragmc.lowdraglib.misc.FluidStorage;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidStack;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.joml.Vector3f;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.fluid.FluidStackJS;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;

/**
 * @author KilaBash
 * @date 2023/3/26
 * @implNote GregTechKubeJSPlugin
 */
public class LDLibKubeJSPlugin extends KubeJSPlugin {


    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        super.registerClasses(type, filter);
        filter.allow("com.lowdragmc.lowdraglib");
    }

    @Override
    public void registerEvents() {
        super.registerEvents();
        UIEvents.INSTANCE.register();
    }

    @ExpectPlatform
    public static void registerPlatformBindings(BindingsEvent event) {
        throw new AssertionError();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        super.registerBindings(event);
        registerPlatformBindings(event);
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
        event.add("BlockSelectorWidget", BlockSelectorWidget.class);
        event.add("ButtonWidget", ButtonWidget.class);
        event.add("DialogWidget", DialogWidget.class);
        event.add("DraggableScrollableWidgetGroup", DraggableScrollableWidgetGroup.class);
        event.add("DraggableWidgetGroup", DraggableWidgetGroup.class);
        event.add("ImageWidget", ImageWidget.class);
        event.add("LabelWidget", LabelWidget.class);
        event.add("PhantomFluidWidget", PhantomFluidWidget.class);
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
        // storage
        event.add("ItemStackTransfer", ItemStackTransfer.class);
        event.add("FluidStorage", FluidStorage.class);
    }

    @Override
    public void registerTypeWrappers(ScriptType type, TypeWrappers typeWrappers) {
        super.registerTypeWrappers(type, typeWrappers);
        typeWrappers.register(FluidStack.class, (ctx, o) -> {
            var fluidStack = FluidStackJS.of(o).getFluidStack();
            return FluidStack.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
        });
    }

}
