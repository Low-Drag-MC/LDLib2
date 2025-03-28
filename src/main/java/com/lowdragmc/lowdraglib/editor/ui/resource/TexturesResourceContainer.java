package com.lowdragmc.lowdraglib.editor.ui.resource;

import com.lowdragmc.lowdraglib.LDLibRegistries;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.mojang.datafixers.util.Either;

import java.io.File;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TexturesResourceContainer
 */
public class TexturesResourceContainer extends ResourceContainer<IGuiTexture, ImageWidget> {
    public TexturesResourceContainer(Resource<IGuiTexture> resource, ResourcePanel panel) {
        super(resource, panel);
        setWidgetSupplier(k -> new ImageWidget(0, 0, 30, 30, getResource().getResource(k)));
        setDragging(key -> new UIResourceTexture(resource.getResource(key), key), o -> o);
        setOnEdit(key -> openTextureConfigurator(key, getResource().getResource(key)));
        setCanEdit(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setCanGlobalChange(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setCanRemove(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setOnMenu((selected, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
            for (var holder : LDLibRegistries.GUI_TEXTURES) {
                String name = holder.annotation().name();
                if (name.equals("empty") || name.equals("ui_resource_texture")) continue;
                IGuiTexture icon = holder.value().get();
                menu.leaf(icon, name, () -> {
                    resource.addBuiltinResource(genNewFileName(), holder.value().get());
                    reBuild();
                });
            }
        }));
    }

    private void openTextureConfigurator(Either<String, File> key, IGuiTexture current) {
        if (resource.getResourceName(key).equals("empty") || !current.isLDLRegister()) return;
        getPanel().getEditor().getConfigPanel().openConfigurator(ConfigPanel.Tab.RESOURCE, new IConfigurable() {
            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                final var defaultHolder = current.getRegistryHolder();
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
                        LDLibRegistries.GUI_TEXTURES.values().stream().filter(value -> {
                            var name = value.annotation().name();
                            return !name.equals("empty") && !name.equals("ui_resource_texture");
                        }).toList(),
                        holder -> holder.annotation().name()
                );
                selectorConfigurator.setTips("ldlib.gui.editor.tips.texture_type");
                father.addConfigurators(selectorConfigurator);
                current.buildConfigurator(father);
            }
        });
    }
}
