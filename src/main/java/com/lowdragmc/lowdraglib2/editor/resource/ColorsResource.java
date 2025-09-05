package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.configurator.EditAction;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class ColorsResource extends Resource<Integer> {
    public static final ColorsResource INSTANCE = new ColorsResource();

    public ColorsResource() {
        setDefaultDisplayMode(DisplayMode.LIST);
        setDefaultUIWidth(15);
    }

    @Override
    public void buildBuiltin(BuiltinResourceProvider<Integer> provider) {
        for (ColorPattern value : ColorPattern.values()) {
            provider.addResource(value.colorName, value.color);
        }
    }

    @Override
    public IGuiTexture getIcon() {
        return Icons.COLOR;
    }

    @Override
    public String getName() {
        return "color";
    }

    @Nullable
    @Override
    public Tag serializeResource(Integer value, HolderLookup.Provider provider) {
        return IntTag.valueOf(value);
    }

    @Override
    public Integer deserializeResource(Tag nbt, HolderLookup.Provider provider) {
        return nbt instanceof IntTag intTag ? intTag.getAsInt() : -1;
    }

    @Override
    public ResourceProviderContainer<Integer> createResourceProviderContainer(IResourceProvider<Integer> provider) {
        return super.createResourceProviderContainer(provider)
                .setAddDefault(() -> -1)
                .setUiSupplier(path -> new UIElement().layout(layout -> {
                    layout.setWidthPercent(100);
                    layout.setHeightPercent(100);
                }).style(style -> style.backgroundTexture(new ColorRectTexture(provider.getResource(path)))))
                .setOnEdit((container, path) -> {
                    var colorSelector = new ColorSelector().setColor(provider.getResource(path));
                    var dialog = new Dialog();
                    dialog.addContent(colorSelector.layout(layout -> layout.setWidthPercent(100)))
                            .addButton(new Button().setOnClick(e -> {
                                var previousColor = provider.getResource(path);
                                var newColor = colorSelector.getColor();
                                container.getEditor().historyView.pushHistory(Component.translatable("editor.edit_color"), EditAction.of(() -> {
                                    provider.addResource(path, newColor);
                                    container.reloadSpecificResource(path);
                                }, () -> {
                                    provider.addResource(path, previousColor);
                                    container.reloadSpecificResource(path);
                                }));
                                dialog.close();
                            }).setText("ldlib.gui.tips.confirm"))
                            .addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.cancel"))
                            .show(container.getEditor());
                });
    }
}
