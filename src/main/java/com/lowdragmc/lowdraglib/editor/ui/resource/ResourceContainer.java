package com.lowdragmc.lowdraglib.editor.ui.resource;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.configurator.ui.ArrayConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.resource.Resource;
import com.lowdragmc.lowdraglib.editor.resource.ResourceProvider;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.editor_outdated.Icons;
import com.lowdragmc.lowdraglib.gui.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.ui.Dialog;
import com.lowdragmc.lowdraglib.gui.ui.UIElement;
import com.lowdragmc.lowdraglib.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.appliedenergistics.yoga.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResourceContainer extends UIElement {
    public final UIElement providerList = new UIElement();
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
        addChildren(new UIElement().layout(layout -> {
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
        })), providerContainer.layout(layout -> {
            layout.setHeightPercent(100);
            layout.setWidthPercent(87);
        }).addEventListener(UIEvents.MOUSE_DOWN, event -> {
            // drag left border
            if (event.button == 0 && isMouseOver(providerContainer.getPositionX(), providerContainer.getPositionY(),
                    3, providerContainer.getSizeHeight(), event.x, event.y)) {
                providerContainer.startDrag(YogaEdge.LEFT, Icons.ARROW_LEFT_RIGHT).setDragTexture(-7, -4, 13, 7);
            }
        }).addEventListener(UIEvents.DRAG_SOURCE_UPDATE, event -> {
            if (event.dragHandler.draggingObject == YogaEdge.LEFT) {
                var x = 1 - (event.x - getPositionX()) / getSizeWidth();
                providerContainer.layout(layout -> layout.setWidthPercent(Mth.clamp(x * 100, 10, 90)));
            }
        }));

        if (resource.canAddFileResourceProvider()) {
            addButton.setActive(resource.canAddFileResourceProvider());
            addButton.textStyle(textStyle -> textStyle.textColor(ColorPattern.WHITE.color));
        }
        removeButton.setActive(false);

        loadResource();
    }

    private void onRemoveFileResourceProvider(UIEvent event) {
        if (selectedProvider != null) {
            new Dialog().darkenBackground()
                    .setTitle("ldlib.gui.editor.menu.remove")
                    .addContent(new UIElement().layout(layout -> {
                        layout.setWidth(50);
                        layout.setHeight(50);
                    }))
                    .addButton(new Button())
                    .addButton(new Button())
                    .show(editor);
            ((Resource)resource).removeResourceProvider(selectedProvider);
        }
    }

    private void onAddFileResourceProvider(UIEvent event) {
        Dialog.showFileDialog("title", LDLib.getAssetsDir(), false, file -> true, result -> {

        }).show(editor);
    }

    public void loadResource() {
        var lastSelectedProvider = selectedProvider;
        providerToggles.clear();
        providerList.clearAllChildren();
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
            providerList.addChild(toggle);
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

    @Override
    public void drawBackgroundAdditional(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawBackgroundAdditional(graphics, mouseX, mouseY, partialTicks);
        if (isChildHover() && isMouseOver(providerContainer.getPositionX(), providerContainer.getPositionY(),
                3, providerContainer.getSizeHeight(), mouseX, mouseY)) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            Icons.ARROW_LEFT_RIGHT.draw(graphics, mouseX, mouseY, mouseX - 7, mouseY - 4, 13, 7, partialTicks);
            graphics.pose().popPose();
        }
    }
}
