package com.lowdragmc.lowdraglib2.editor.ui.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.Resource;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceProvider;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.util.SplitView;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResourceContainer extends UIElement {
    public final ScrollerView providerList = new ScrollerView();
    public final UIElement providerContainer = new UIElement();
    public final Resource<?> resource;
    public final Editor editor;
    private final Button addButton, removeButton;

    // runtime
    private final Map<ResourceProvider<?>, UIElement> providerToggles = new java.util.HashMap<>();
    @Getter
    @Nullable
    private ResourceProvider<?> selectedProvider = null;

    public ResourceContainer(Resource<?> resource, Editor editor) {
        getLayout().setFlex(1);
        getLayout().setHeightPercent(100);
        getLayout().setFlexDirection(YogaFlexDirection.ROW);

        this.resource = resource;
        this.editor = editor;
        addChildren(new SplitView.Horizontal().left(new UIElement().layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }).addChildren(new UIElement().layout(layout -> {
            layout.setFlexDirection(YogaFlexDirection.ROW);
        }).addChildren(
                addButton = (Button) new Button().setOnClick(this::onAddFileResourceProvider).setText("+")
                        .textStyle(textStyle -> textStyle.textColor(ColorPattern.GRAY.color).textShadow(false))
                        .layout(layout -> {
                            layout.setFlex(1);
                            layout.setHeight(12);}),
                removeButton = (Button) new Button().setOnClick(this::onRemoveFileResourceProvider).setText("-")
                        .textStyle(textStyle -> textStyle.textColor(ColorPattern.GRAY.color).textShadow(false))
                        .layout(layout -> {
                            layout.setFlex(1);
                            layout.setHeight(12);})
        ), providerList.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        }))).right(providerContainer.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        })).setPercentage(13));

        if (resource.canAddFileResourceProvider()) {
            addButton.setActive(resource.canAddFileResourceProvider());
            addButton.textStyle(textStyle -> textStyle.textColor(ColorPattern.WHITE.color));
        }
        removeButton.setActive(false);

        loadResource();
    }

    private void onRemoveFileResourceProvider(UIEvent event) {
        if (selectedProvider != null) {
            Dialog.showCheckBox("ldlib.gui.resource.remove_provider", "editor.remove.confirm", result -> {
                if (result) {
                    ((Resource)resource).removeResourceProvider(selectedProvider);
                    selectProvider(null);
                }
            }).show(editor);
        }
    }

    private void onAddFileResourceProvider(UIEvent event) {
        Dialog.showFileDialog("ldlib.gui.resource.add_provider", LDLib2.getAssetsDir(), true, file -> true, result -> {
            if (result.isFile()) {
                result = result.getParentFile();
            }
            if (result.isDirectory()) {
                ((Resource)resource).addResourceProvider(resource.createNewFileResourceProvider(result));
            }
        }).show(editor);
    }

    public void loadResource() {
        var lastSelectedProvider = selectedProvider;
        providerToggles.clear();
        providerList.clearAllScrollViewChildren();
        providerContainer.clearAllChildren();

        for (var provider : resource.getProviders()) {
            var toggle = new UIElement().layout(layout -> {
                layout.setHeight(12);
                layout.setWidthPercent(100);
                layout.setFlexDirection(YogaFlexDirection.ROW);
                layout.setAlignItems(YogaAlign.CENTER);
                layout.setPadding(YogaEdge.RIGHT, 2);
            }).addChildren(provider.createProviderToggle()).addEventListener(UIEvents.MOUSE_DOWN, event -> {
                if (event.button == 0) {
                    selectProvider(provider);
                }
            });
            providerList.addScrollViewChild(toggle);
            providerToggles.put(provider, toggle);
        }

        if (!resource.getProviders().isEmpty()) {
            if (lastSelectedProvider != null && resource.getProviders().contains(lastSelectedProvider)) {
                selectProvider(lastSelectedProvider);
            } else {
                selectProvider(resource.getProviders().getFirst());
            }
        } else {
            selectProvider(null);
        }
    }

    public void selectProvider(@Nullable ResourceProvider<?> provider) {
        if (selectedProvider == provider) return;
        if (selectedProvider != null) {
            var oldToggle = providerToggles.get(selectedProvider);
            if (oldToggle != null) {
                oldToggle.style(style -> style.overlayTexture(IGuiTexture.EMPTY));
            }
        }
        providerContainer.clearAllChildren();
        selectedProvider = provider;
        if (selectedProvider != null) {
            var toggle = providerToggles.get(selectedProvider);
            if (toggle != null) {
                toggle.style(style -> style.overlayTexture(ColorPattern.T_DARK_GRAY.rectTexture()));
            }
            var providerView = ((Resource)resource).createResourceProviderContainer(selectedProvider);
            providerView.setEditor(editor);
            providerView.reloadResourceContainer();
            providerContainer.addChild(providerView);
        }
        var canRemove = selectedProvider != null && ((Resource)resource).canRemoveResourceProvider(selectedProvider);
        removeButton.setActive(canRemove);
        removeButton.textStyle(textStyle -> textStyle.textColor(canRemove ? ColorPattern.WHITE.color : ColorPattern.GRAY.color));
    }

    @Override
    public void screenTick() {
        super.screenTick();
        // check if the providers have changed
        boolean changed = resource.getProviders().size() != providerToggles.size();
        if (!changed) {
            for (var provider : resource.getProviders()) {
                if (!providerToggles.containsKey(provider)) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            loadResource();
        }
    }
}
