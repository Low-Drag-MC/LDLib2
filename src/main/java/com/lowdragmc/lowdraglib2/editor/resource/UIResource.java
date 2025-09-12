package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.editor.ui.view.ui.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;

public class UIResource extends Resource<UI> {
    public static final UIResource INSTANCE = new UIResource();

    public UIResource() {
    }

    @Override
    public void buildBuiltin(BuiltinResourceProvider<UI> provider) {

    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.WIDGET_BASIC;
    }

    @Override
    public String getName() {
        return "ui";
    }

    @Nullable
    @Override
    public Tag serializeResource(UI value, HolderLookup.Provider provider) {
        return value.serialize(provider);
    }

    @Override
    public UI deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return UI.fromNbt(provider, nbt instanceof CompoundTag tag ? tag : new CompoundTag());
    }

    @Override
    public ResourceProviderContainer<UI> createResourceProviderContainer(IResourceProvider<UI> provider) {
        return super.createResourceProviderContainer(provider)
                .setAddDefault(() -> UI.of(new UIElement().layout(layout -> {
                    layout.setWidth(150);
                    layout.setHeight(150);
                }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID))))
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(Icons.WIDGET_BASIC)))
                .setOnEdit((container, path) -> {
                    var ui = provider.getResource(path);
                    if (ui == null) return;
                    var editor = container.getEditor();
                    for (var view : editor.getAllViews()) {
                        // if it has already opened
                        if (view instanceof UIEditorView uiEditorView && uiEditorView.getCurrentUI() == ui) return;
                    }
                    var newView = new UIEditorView().loadUI(ui);
                    newView.setCanRemove(true);
                    newView.setIcon(Icons.WIDGET_BASIC);
                    newView.setName(path.getResourceName());
                    editor.centerWindow.getLeftTop().addView(newView);
                });
    }
}
