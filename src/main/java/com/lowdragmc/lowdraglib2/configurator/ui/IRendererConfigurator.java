package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaEdge;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class IRendererConfigurator extends ValueConfigurator<IRenderer> {
    public final Scene preview = new Scene();
    @Getter @Nullable
    private RendererBlockEntity rendererBlock;
    @Setter
    protected Predicate<IRenderer> filter = Predicates.alwaysTrue();

    public IRendererConfigurator(String name, Supplier<IRenderer> supplier, Consumer<IRenderer> onUpdate, IRenderer defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setTips("editor.drag_drop_resource");
        if (value == null) {
            value = defaultValue;
        }

        var level = new TrackedDummyWorld();
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
        Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof RendererBlockEntity holder) {
                rendererBlock = holder;
                holder.setRenderer(value);
            }
        });

        preview.setRenderFacing(false);
        preview.setRenderSelect(false);
        preview.createScene(level);
        assert preview.getRenderer() != null;
        preview.getRenderer().setOnLookingAt(null); // better performance
        preview.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        preview.layout(layout -> {
            layout.setAspectRatio(1.0f);
            layout.setWidthPercent(100);
            layout.setMaxWidth(100);
            layout.setMaxHeight(100);
            layout.setAlignSelf(YogaAlign.CENTER);
            layout.setPadding(YogaEdge.ALL, 3);
        });
        preview.style(style -> style.backgroundTexture(Sprites.BORDER1_RT1));

        inlineContainer.addChild(preview);

        setPastable(IRenderer.class, pasted -> {
            if (pasted != null && filter.test(pasted)) {
                onPaste(pasted);
            }
        });
        setCopiable(IRenderer::copy);
        setCanDropPredicate(obj -> obj instanceof IRenderer && filter.test((IRenderer) obj));
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        var value = getValue();
        if (value != null && value != IRenderer.EMPTY) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> updateValueActively(IRenderer.EMPTY));
        }
        return menu;
    }

    @Override
    protected void onValueUpdatePassively(IRenderer newValue) {
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
        if (rendererBlock != null) {
            rendererBlock.setRenderer(newValue);
        }
    }
}
