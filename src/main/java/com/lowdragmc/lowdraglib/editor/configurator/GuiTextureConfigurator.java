package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.Icons;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.math.Size;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote GuiTextureConfigurator
 */
public class GuiTextureConfigurator extends ValueConfigurator<IGuiTexture>{
    protected ImageWidget preview;
    @Setter
    protected Consumer<ClickData> onPressCallback;
    @Setter
    protected Predicate<IGuiTexture> available;

    public GuiTextureConfigurator(String name, Supplier<IGuiTexture> supplier, Consumer<IGuiTexture> onUpdate, boolean forceUpdate) {
        super(name, supplier, onUpdate, IGuiTexture.EMPTY, forceUpdate);
        if (value == null) {
            value = defaultValue;
        }
    }

    @Override
    protected void onValueUpdate(IGuiTexture newValue) {
        if (Objects.equals(newValue, value)) return;
        super.onValueUpdate(newValue);
        preview.setImage(newValue);
    }

    @Override
    public void computeHeight() {
        super.computeHeight();
        setSize(Size.of(getSize().width, 15 + preview.getSize().height + 4));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && Editor.INSTANCE != null && preview.isMouseOverElement(mouseX, mouseY)) {
            var menu = TreeBuilder.Menu.start()
                    .leaf(Icons.DELETE, "ldlib.gui.editor.menu.remove", () -> {
                        onValueUpdate(IGuiTexture.EMPTY);
                        updateValue();
                    })
                    .leaf(Icons.COPY, "ldlib.gui.editor.menu.copy", () -> Editor.INSTANCE.setCopy("texture", value));
            if ("texture".equals(Editor.INSTANCE.getCopyType())) {
                menu.leaf(Icons.PASTE, "ldlib.gui.editor.menu.paste", () -> {
                    Editor.INSTANCE.ifCopiedPresent("texture", c -> {
                        if (c instanceof IGuiTexture texture) {
                            onValueUpdate(texture);
                            updateValue();
                        }
                    });
                });
            }
            Editor.INSTANCE.openMenu(mouseX, mouseY, menu);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void init(int width) {
        super.init(width);
        int w = Math.min(width - 6, 50);
        int x = (width - w) / 2;
        addWidget(preview = new ImageWidget(x, 17, w, w, value).setBorder(2, ColorPattern.T_WHITE.color));
        preview.setDraggingConsumer(
                o -> available == null ? (o instanceof IGuiTexture || o instanceof Integer || o instanceof String) : (o instanceof IGuiTexture texture && available.test(texture)),
                o -> preview.setBorder(2, ColorPattern.GREEN.color),
                o -> preview.setBorder(2, ColorPattern.T_WHITE.color),
                o -> {
                    IGuiTexture newTexture = null;
                    if (available != null && o instanceof IGuiTexture texture && available.test(texture)) {
                        newTexture = texture;
                    }else if (o instanceof IGuiTexture texture) {
                        newTexture = texture;
                    } else if (o instanceof Integer color) {
                        newTexture = new ColorRectTexture(color);
                    } else if (o instanceof String string) {
                        newTexture = new TextTexture(string);
                    }
                    if (newTexture != null) {
                        onValueUpdate(newTexture);
                        updateValue();
                    }
                    preview.setBorder(2, ColorPattern.T_WHITE.color);
                });
        if (onPressCallback != null) {
            addWidget(new ButtonWidget(x, 17, w, w, IGuiTexture.EMPTY, onPressCallback));
        }
    }

}
