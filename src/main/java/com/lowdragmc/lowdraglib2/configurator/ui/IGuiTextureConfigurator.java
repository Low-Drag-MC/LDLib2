package com.lowdragmc.lowdraglib2.configurator.ui;

import com.google.common.base.Predicates;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SupplierDataSource;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleOrigin;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.ModularUITooltipComponent;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Accessors(chain = true)
public class IGuiTextureConfigurator extends ValueConfigurator<IGuiTexture> {
    public final UIElement preview = new UIElement();
    @Setter
    protected Predicate<IGuiTexture> filter = Predicates.alwaysTrue();
    // runtime
    @Nullable
    private ModularUITooltipComponent hoverTooltips;

    public IGuiTextureConfigurator(String name, Supplier<IGuiTexture> supplier, Consumer<IGuiTexture> onUpdate, IGuiTexture defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
        setTips("editor.drag_drop_resource");
        if (value == null) {
            value = defaultValue;
        }

        var preview = new UIElement();
        preview.layout(layout -> {
                    layout.setPipelineState(StyleOrigin.DEFAULT);
                    layout.height(14);
                    layout.paddingAll(2);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(2);
                    layout.setPipelineState(StyleOrigin.INLINE);
                }).style(style -> {
                    style.setPipelineState(StyleOrigin.DEFAULT);
                    style.backgroundTexture(Sprites.RECT_RD_SOLID);
                    style.setPipelineState(StyleOrigin.IMPORTANT);
                    style.overlayTexture(DynamicTexture.of(() -> preview.isSelfOrChildHover() ?
                            Sprites.RECT_RD_T_SOLID : IGuiTexture.EMPTY));
                    style.setPipelineState(StyleOrigin.INLINE);
                }).addClass("configurator_preview_bg").addChild(new Label().bindDataSource(SupplierDataSource.of(() -> {
                    var value = getValue();
                    if (value instanceof UIResourceTexture uiResourceTexture) {
                        return Component.literal(uiResourceTexture.getResourcePath().getResourceName());
                    }
                    return value == null ? Component.empty() : Component.literal(value.getConfigurableName());
                })).textStyle(textStyle -> textStyle.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HOVER_ROLL)).layout(layout -> {
                    layout.heightPercent(100);
                    layout.flex(1);
                }).setOverflowVisible(false))
                .addChild(new UIElement().layout(layout -> {
                    layout.heightPercent(100);
                    layout.setAspectRatio(1);
                }).style(style -> style.backgroundTexture(DynamicTexture.of(this::getValue))))
                .addEventListener(UIEvents.HOVER_TOOLTIPS, this::onHoverTooltips);
        preview.addEventListener(UIEvents.MOUSE_DOWN, this::showTextureDialog);
        inlineContainer.addChildren(preview);

        setPastable(IGuiTexture.class, pasted -> {
            if (pasted != null && filter.test(pasted)) {
                onPaste(pasted);
            }
        });
        setCopiable(IGuiTexture::copy);
        setCanDropPredicate(obj ->
                (obj instanceof Integer color && filter.test(new ColorRectTexture(color))) ||
                obj instanceof IGuiTexture texture && filter.test(texture));
    }

    protected void showTextureDialog(UIEvent event) {
        var previous = getValue();
        TexturesResource.INSTANCE.getResourceInstance().createSelectorDialog(event.x, event.y, texture -> {
            onValueUpdatePassively(texture);
            updateValue();
        }, () -> {
            if (previous == null) return;
            onValueUpdatePassively(previous);
            updateValue();
        }).show(getModularUI());
    }

    protected void onDropObject(@Nullable Object object) {
        if (canDropObject(object)) {
            IGuiTexture texture = IGuiTexture.EMPTY;
            if (object instanceof Integer color) {
                texture = new ColorRectTexture(color);
            } else if (object instanceof IGuiTexture t) {
                texture = t;
            }
            onValueUpdatePassively(texture);
            updateValue();
        }
    }

    protected void onHoverTooltips(UIEvent event) {
        if (getValue() == null || getValue() == IGuiTexture.EMPTY) return;
        if (hoverTooltips == null) {
            hoverTooltips = new ModularUITooltipComponent(new UIElement().layout(layout -> {
                layout.width(100);
                layout.height(100);
            }).style(style -> style.backgroundTexture(DynamicTexture.of(this::getValue))));
        }
        event.hoverTooltips = new HoverTooltips(List.of(Component.translatable("ldlib.gui.editor.group.preview")), hoverTooltips, null, null);
    }

    @Override
    protected TreeBuilder.Menu createMenu() {
        var menu = super.createMenu();
        var value = getValue();
        if (value != null && value != IGuiTexture.EMPTY) {
            menu.leaf(Icons.REMOVE, "ldlib.gui.editor.menu.remove", () -> {
                updateValueActively(IGuiTexture.EMPTY);
                updateValue();
            });
        }
        return menu;
    }

    @Override
    protected void onValueUpdatePassively(IGuiTexture newValue) {
        if (newValue.equals(value)) return;
        super.onValueUpdatePassively(newValue);
    }
}
