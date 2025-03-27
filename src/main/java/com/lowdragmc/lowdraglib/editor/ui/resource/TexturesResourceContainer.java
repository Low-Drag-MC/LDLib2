package com.lowdragmc.lowdraglib.editor.ui.resource;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.registry.AutoRegistry;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;

import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TexturesResourceContainer
 */
public class TexturesResourceContainer extends ResourceContainer<IGuiTexture, ImageWidget> {
    public TexturesResourceContainer(Resource<IGuiTexture> resource, ResourcePanel panel) {
        super(resource, panel);
        setWidgetSupplier(k -> new ImageWidget(0, 0, 30, 30, getResource().getResource(k)));
        setDragging(key -> new UIResourceTexture(resource, key), o -> o);
        setOnEdit(key -> openTextureConfigurator(key, getResource().getResource(key)));
        setOnRemove(key -> !key.equals("empty"));
    }

    private void openTextureConfigurator(String key, IGuiTexture current) {
        if (key.equals("empty")) return;
        getPanel().getEditor().getConfigPanel().openConfigurator(ConfigPanel.Tab.RESOURCE, new IConfigurable() {
            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                final AutoRegistry.Holder<LDLRegister, IGuiTexture, Supplier<IGuiTexture>> defaultHolder = current.getRegistryHolder();
                var selectorConfigurator = new SelectorConfigurator<>(
                        "ldlib.gui.editor.name.texture_type",
                        () -> defaultHolder,
                        holder -> {
                            if (holder != defaultHolder) {
                                var newTexture = holder.value().get();
                                getResource().addResource(key, newTexture);
                                getWidgets().get(key).setImage(newTexture);
                                openTextureConfigurator(key, newTexture);
                            }
                        },
                        defaultHolder,
                        false,
                        LDLibRegistries.GUI_TEXTURES.values().stream().toList(),
                        holder -> "%s.%s".formatted(LDLibRegistries.GUI_TEXTURES.getRegistryName(), holder.annotation().name())
                );
                selectorConfigurator.setTips("ldlib.gui.editor.tips.texture_type");
                father.addConfigurators(selectorConfigurator);
                current.buildConfigurator(father);
            }

        });
    }

    @Override
    protected TreeBuilder.Menu getMenu() {
        return TreeBuilder.Menu.start()
                .leaf(Icons.EDIT_FILE, "ldlib.gui.editor.menu.edit", this::editResource)
                .leaf("ldlib.gui.editor.menu.rename", this::renameResource)
                .crossLine()
                .leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", this::copy)
                .leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", this::paste)
                .branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
                    for (var holder : LDLibRegistries.GUI_TEXTURES) {
                        IGuiTexture icon = holder.value().get();
                        String name = "%s.%s".formatted(LDLibRegistries.GUI_TEXTURES.getRegistryName(), holder.annotation().name());
                        menu.leaf(icon, name, () -> {
                            resource.addResource(genNewFileName(), holder.value().get());
                            reBuild();
                        });
                    }
                })
                .leaf(Icons.REMOVE_FILE, "ldlib.gui.editor.menu.remove", this::removeSelectedResource);
    }
}
