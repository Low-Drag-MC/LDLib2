package com.lowdragmc.lowdraglib.gui.editor.ui.resource;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib.client.renderer.impl.UIResourceRenderer;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.data.resource.Resource;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.editor.ui.ConfigPanel;
import com.lowdragmc.lowdraglib.gui.editor.ui.ResourcePanel;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Optional;

public class IRendererResourceContainer extends ResourceContainer<IRenderer, Widget> {

    public IRendererResourceContainer(Resource<IRenderer> resource, ResourcePanel panel) {
        super(resource, panel);
        setWidgetSupplier(k -> createPreview(getResource().getResource(k)));
        setDragging(key -> new UIResourceRenderer(resource, key),
                (k, o, p) -> new TextTexture(resource.getResourceName(k)));
        setOnEdit(key -> {
            if (getResource().getResource(key) instanceof IConfigurable configurable) {
                getPanel().getEditor().getConfigPanel().openConfigurator(ConfigPanel.Tab.RESOURCE, configurable);
            } else {
                getPanel().getEditor().getConfigPanel().clearAllConfigurators(ConfigPanel.Tab.RESOURCE);
            }
        });
    }

    protected SceneWidget createPreview(IRenderer renderer) {
        var level = new TrackedDummyWorld();
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
        Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof RendererBlockEntity holder) {
                holder.setRenderer(renderer);
            }
        });
        var sceneWidget = new SceneWidget(0, 0, 50, 50, null);
        sceneWidget.setRenderFacing(false);
        sceneWidget.setRenderSelect(false);
        sceneWidget.setScalable(false);
        sceneWidget.setDraggable(false);
        sceneWidget.setIntractable(false);
        sceneWidget.createScene(level);
        sceneWidget.getRenderer().setOnLookingAt(null); // better performance
        sceneWidget.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        return sceneWidget;
    }

    @Override
    protected TreeBuilder.Menu getMenu() {
        return super.getMenu().branch(Icons.ADD_FILE, "ldlib.gui.editor.menu.add_renderer", menu -> {
            for (var entry : AnnotationDetector.REGISTER_RENDERERS.entrySet()) {
                menu.leaf("ldlib.renderer.%s".formatted(entry.getKey()), () -> {
                    var renderer = entry.getValue().creator().get();
                    renderer.initRenderer();
                    resource.addBuiltinResource(genNewFileName(), renderer);
                    reBuild();
                });
            }
        });
    }
}
