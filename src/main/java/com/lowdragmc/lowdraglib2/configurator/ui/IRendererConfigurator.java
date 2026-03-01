package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib2.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib2.editor.resource.IRendererResource;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.Style;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import dev.vfyjxf.taffy.style.AlignItems;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;

import org.jetbrains.annotations.Nullable;
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
            layout.setPipelineState(StyleOrigin.DEFAULT);
            layout.setAspectRatio(1.0f);
            layout.widthPercent(100);
            layout.maxWidth(100);
            layout.maxHeight(100);
            layout.alignSelf(AlignItems.CENTER);
            layout.paddingAll(3);
            layout.setPipelineState(StyleOrigin.INLINE);
        });
        preview.style(style -> Style.defaultPipeline(style, s -> s.backgroundTexture(Sprites.BORDER1_RT1)))
                .addClass("preview_bg");

        preview.addEventListener(UIEvents.MOUSE_DOWN, this::showRendererDialog);

        inlineContainer.getLayout().maxHeight(100);
        inlineContainer.addChild(preview);

        setPastable(IRenderer.class, pasted -> {
            if (pasted != null && filter.test(pasted)) {
                onPaste(pasted);
            }
        });
        setCopiable(IRenderer::copy);
        setCanDropPredicate(obj -> obj instanceof IRenderer && filter.test((IRenderer) obj));
    }

    protected void showRendererDialog(UIEvent event) {
        var previous = getValue();
        IRendererResource.INSTANCE.getResourceInstance().createSelectorDialog(event.x, event.y, renderer -> {
            onValueUpdatePassively(renderer);
            updateValue();
        }, () -> {
            if (previous == null) return;
            onValueUpdatePassively(previous);
            updateValue();
        }).show(getModularUI());
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        var value = getValue();
        if (value != null && value != IRenderer.EMPTY) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                updateValueActively(IRenderer.EMPTY);
                updateValue();
            });
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
