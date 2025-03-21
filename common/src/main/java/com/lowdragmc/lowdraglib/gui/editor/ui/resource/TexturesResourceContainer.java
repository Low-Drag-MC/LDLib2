package com.lowdragmc.lowdraglib.gui.editor.ui.resource;

import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.SelectorConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.UIResourceTexture;
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
        setDragging(key -> new UIResourceTexture(resource, key), o -> o);
        setOnEdit(key -> openTextureConfigurator(key, getResource().getResource(key)));
        setCanEdit(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setCanGlobalChange(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setCanRemove(key -> key.left().isEmpty() || !resource.getResourceName(key).equals("empty"));
        setOnMenu((selected, m) -> m.branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_resource", menu -> {
            for (AnnotationDetector.Wrapper<LDLRegister, IGuiTexture> wrapper : AnnotationDetector.REGISTER_TEXTURES) {
                IGuiTexture icon = wrapper.creator().get();
                String name = "ldlib.gui.editor.register.texture." + wrapper.annotation().name();
                menu.leaf(icon, name, () -> {
                    resource.addBuiltinResource(genNewFileName(), wrapper.creator().get());
                    reBuild();
                });
            }
        }));
    }

    private void openTextureConfigurator(Either<String, File> key, IGuiTexture current) {
        if (resource.getResourceName(key).equals("empty")) return;
        getPanel().getEditor().getConfigPanel().openConfigurator(ConfigPanel.Tab.RESOURCE, new IConfigurable() {
            @Override
            public void buildConfigurator(ConfiguratorGroup father) {
                AnnotationDetector.Wrapper<LDLRegister, IGuiTexture> defaultWrapper = null;
                for (var wrapper : AnnotationDetector.REGISTER_TEXTURES) {
                    if (wrapper.clazz() == current.getClass()) {
                        defaultWrapper = wrapper;
                    }
                }

                AnnotationDetector.Wrapper<LDLRegister, IGuiTexture> finalDefaultWrapper = defaultWrapper;
                SelectorConfigurator<AnnotationDetector.Wrapper<LDLRegister, IGuiTexture>> selectorConfigurator = new SelectorConfigurator<>(
                        "ldlib.gui.editor.name.texture_type",
                        () -> finalDefaultWrapper,
                        wrapper -> {
                            if (wrapper != finalDefaultWrapper) {
                                var newTexture = wrapper.creator().get();
                                getResource().addResource(key, newTexture);
                                getWidgets().get(key).setImage(newTexture);
                                openTextureConfigurator(key, newTexture);
                            }
                        },
                        finalDefaultWrapper,
                        false,
                        AnnotationDetector.REGISTER_TEXTURES,
                        w -> "ldlib.gui.editor.register.texture." + w.annotation().name()
                );
                selectorConfigurator.setTips("ldlib.gui.editor.tips.texture_type");
                father.addConfigurators(selectorConfigurator);
                current.buildConfigurator(father);
            }

        });
    }

}
