package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;

public class UITemplate {
    public static final UITemplate MISSING = UITemplate.of(new Label().setText("Missing")
            .textStyle(textStyle -> textStyle.textColor(ColorPattern.RED.color)));

    public static final Codec<UITemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("a").forGetter(range -> range.data)
    ).apply(instance, UITemplate::of));

    @Setter
    @Getter
    private CompoundTag data;

    private UITemplate(CompoundTag data) {
        this.data = data;
    }

    public UI createUI() {
        var root = new UIElement();
        root.deserializeNBT(Platform.getFrozenRegistry(), data);
        return UI.of(root);
    }

    public static UITemplate of(CompoundTag data) {
        return new UITemplate(data);
    }

    public static UITemplate of(UIElement root) {
        return new UITemplate(Optional.ofNullable(root.serializeShortNBT(Platform.getFrozenRegistry())).orElseGet(CompoundTag::new));
    }
}
