package com.lowdragmc.lowdraglib.editor_outdated.configurator;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib.editor_outdated.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib.math.Size;
import com.lowdragmc.lowdraglib.utils.virtuallevel.TrackedDummyWorld;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IRendererConfigurator extends ValueConfigurator<IRenderer>{
    private ImageWidget frame;
    private RendererBlockEntity holder;

    public IRendererConfigurator(String name, Supplier<IRenderer> supplier, Consumer<IRenderer> onUpdate, IRenderer defaultRenderer, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultRenderer, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(IRenderer newValue) {
        if (Objects.equals(newValue, value)) return;
        super.onValueUpdate(newValue);
        holder.setRenderer(newValue);
    }

    @Override
    public void computeHeight() {
        super.computeHeight();
        setSize(Size.of(getSize().width, 15 + frame.getSize().height + 6));
    }

    @Override
    public void init(int width) {
        super.init(width);
        int w = Math.min(width - 6, 100);
        int x = (width - w) / 2;

        addWidget(frame = new ImageWidget(x, 17, w, w, IGuiTexture.EMPTY).setBorder(2, ColorPattern.T_WHITE.color));

        var level = new TrackedDummyWorld();
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
        holder = (RendererBlockEntity) level.getBlockEntity(BlockPos.ZERO);
        assert holder != null;
        holder.setRenderer(value);

        var sceneWidget = new SceneWidget(x, 17, w, w, level);
        sceneWidget.setRenderFacing(false);
        sceneWidget.setRenderSelect(false);
        sceneWidget.createScene(level);
        sceneWidget.getRenderer().setOnLookingAt(null); // better performance
        sceneWidget.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);

        addWidget(sceneWidget);

        sceneWidget.setDraggingConsumer(
                o -> o instanceof IRenderer,
                o -> frame.setBorder(2, ColorPattern.GREEN.color),
                o -> frame.setBorder(2, ColorPattern.T_WHITE.color),
                o -> {
                    if (o instanceof IRenderer renderer) {
                        onValueUpdate(renderer);
                        updateValue();
                    }
                    frame.setBorder(2, ColorPattern.T_WHITE.color);
                });
    }

}
