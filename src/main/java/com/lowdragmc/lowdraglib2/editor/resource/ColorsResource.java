package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.ui.resource.ResourceProviderContainer;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.Dialog;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ColorSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.io.File;

public class ColorsResource extends Resource<Integer> {

    public ColorsResource() {
        var builtinResource = new BuiltinResourceProvider<>(this);
        for (ColorPattern value : ColorPattern.values()) {
            builtinResource.addResource(value.colorName, value.color);
        }
        addResourceProvider(builtinResource);
    }

    @Override
    public void buildDefault() {
        addResourceProvider(createNewFileResourceProvider(new File(LDLib2.getAssetsDir(), "ldlib/resources")).setName("global"));
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
    public ResourceProviderContainer<Integer> createResourceProviderContainer(ResourceProvider<Integer> provider) {
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
                                provider.addResource(path, colorSelector.getColor());
                                container.reloadSpecificResource(path);
                                dialog.close();
                            }).setText("ldlib.gui.tips.confirm"))
                            .addButton(new Button().setOnClick(e -> dialog.close()).setText("ldlib.gui.tips.cancel"))
                            .show(container.getEditor());
                });
    }
}
