package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.editor.ui.view.ui.UIEditorView;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UITemplate;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;

public class UIResource extends Resource<UITemplate> {
    public static final UIResource INSTANCE = new UIResource();

    public UIResource() {
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.WIDGET_BASIC;
    }

    @Override
    public String getName() {
        return "ui";
    }

    @Override
    public void buildBuiltin(ResourceInstance<UITemplate> resourceInstance) {
        var global = new FileResourceProvider<>(resourceInstance, new File(LDLib2.getAssetsDir(), "ldlib2/resources/global"));
        global.setName("global");
        resourceInstance.addBuiltinProvider(global);
    }

    @Nullable
    @Override
    public Tag serializeResource(UITemplate value, HolderLookup.Provider provider) {
        return UITemplate.CODEC.encodeStart(NbtOps.INSTANCE, value).result().orElse(null);
    }

    @Override
    public UITemplate deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return UITemplate.CODEC.parse(NbtOps.INSTANCE, nbt).result().orElse(UITemplate.MISSING);
    }

    @Override
    public ResourceProviderContainer<UITemplate> createResourceProviderContainer(IResourceProvider<UITemplate> provider) {
        return super.createResourceProviderContainer(provider)
                .setAddDefault(() -> UITemplate.of(new UIElement().layout(layout -> {
                    layout.setWidth(150);
                    layout.setHeight(150);
                }).style(style -> style.backgroundTexture(Sprites.RECT_SOLID))))
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(Icons.WIDGET_BASIC)))
                .setOnEdit((container, path) -> {
                    var template = provider.getResource(path);
                    if (template == null) return;
                    var editor = container.getEditor();
                    for (var view : editor.getAllViews()) {
                        // if it has already opened
                        if (view instanceof UIEditorView uiEditorView && uiEditorView.getTemplate() == template) return;
                    }
                    // TODO make it saved manually + check if resource is still valid
                    var newView = new UIEditorView().loadTemplate(template, () -> {
                        var resource = provider.getResource(path);
                        if (resource != null) {
                            provider.addResource(path, resource);
                            container.reloadSpecificResource(path);
                        }
                    });
                    newView.setCanRemove(true);
                    newView.setIcon(Icons.WIDGET_BASIC);
                    newView.setName(path.getResourceName());
                    editor.centerWindow.getLeftTop().addView(newView);
                });
    }
}
