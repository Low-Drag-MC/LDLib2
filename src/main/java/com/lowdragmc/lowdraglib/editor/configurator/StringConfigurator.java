package com.lowdragmc.lowdraglib.editor.configurator;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/2
 * @implNote StringConfigurator
 */
public class StringConfigurator extends ValueConfigurator<String>{
    protected TextFieldWidget textFieldWidget;
    @Setter
    protected boolean isResourceLocation;
    @Setter
    protected boolean isCompoundTag;

    public StringConfigurator(String name, Supplier<String> supplier, Consumer<String> onUpdate, @Nonnull String defaultValue, boolean forceUpdate) {
        super(name, supplier, onUpdate, defaultValue, forceUpdate);
    }

    @Override
    protected void onValueUpdate(String newValue) {
        if (newValue == null) newValue = defaultValue;
        if (newValue.equals(value)) return;
        super.onValueUpdate(newValue);
        textFieldWidget.setCurrentString(value == null ? defaultValue : value);
    }

    @Override
    public void init(int width) {
        super.init(width);
        ImageWidget image;
        addWidget(image = new ImageWidget(leftWidth, 2, width - leftWidth - 3 - rightWidth, 10, ColorPattern.T_GRAY.rectTexture().setRadius(5)));
        image.setDraggingConsumer(
                o -> (!isResourceLocation && o instanceof Number) || o instanceof String || o instanceof ResourceLocation,
                o -> image.setImage(ColorPattern.GREEN.rectTexture().setRadius(5)),
                o -> image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5)),
                o -> {
                    if ((!isResourceLocation && o instanceof Number) || o instanceof String || o instanceof ResourceLocation) {
                        onValueUpdate(o.toString());
                        updateValue();
                    }
                    image.setImage(ColorPattern.T_GRAY.rectTexture().setRadius(5));
                });
        addWidget(textFieldWidget = new TextFieldWidget(leftWidth + 3, 2, width - leftWidth - 6 - rightWidth, 10, null, this::onStringUpdate));
        textFieldWidget.setClientSideWidget();
        textFieldWidget.setCurrentString(value == null ? defaultValue : value);
        textFieldWidget.setBordered(false);
        if (isResourceLocation) {
            textFieldWidget.setResourceLocationOnly();
        }
        if (isCompoundTag) {
            textFieldWidget.setCompoundTagOnly();
        }
    }

    private void onStringUpdate(String s) {
        value = s;
        updateValue();
    }
}
