package com.lowdragmc.lowdraglib2.gui.ui;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode
public class UITemplate {
    private static UITemplate MISSING = null;

    public static final Codec<UITemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CompoundTag.CODEC.fieldOf("template").forGetter(value -> value.data),
            Codec.STRING.optionalFieldOf("builtin_styles").forGetter(ui -> Optional.ofNullable((ui.builtinStyles == null || ui.builtinStyles.isBlank()) ? null : ui.builtinStyles)),
            ResourceLocation.CODEC.listOf().optionalFieldOf("stylesheets").forGetter(ui -> Optional.ofNullable(ui.stylesheets.isEmpty() ? null : ui.stylesheets))
    ).apply(instance, (template, customStyles, stylesheets) ->
            new UITemplate(template, customStyles.orElse(""), stylesheets.orElseGet(Collections::emptyList))));
    public static final StreamCodec<FriendlyByteBuf, UITemplate> STREAM_CODEC = StreamCodec.of((buffer, value) -> {
        buffer.writeNbt(value.data);
        buffer.writeUtf((value.builtinStyles == null || value.builtinStyles.isBlank()) ? "" : value.builtinStyles);
        buffer.writeVarInt(value.stylesheets.size());
        for (var stylesheet : value.stylesheets) {
            buffer.writeResourceLocation(stylesheet);
        }
    }, buffer -> {
        var data = buffer.readNbt();
        var builtinStyles = buffer.readUtf();
        if (builtinStyles.isBlank()) {
            builtinStyles = null;
        }
        var stylesheets = new ArrayList<ResourceLocation>();
        var size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            stylesheets.add(buffer.readResourceLocation());
        }
        return new UITemplate(data, builtinStyles, stylesheets);
    });

    @Setter
    @Getter
    private CompoundTag data;
    @Getter
    @Nullable
    private String builtinStyles;
    @Getter
    private final List<ResourceLocation> stylesheets = new ArrayList<>();
    // cache
    @Nullable
    private Stylesheet customStylesheet;

    private UITemplate(CompoundTag data, @Nullable String builtinStyles, List<ResourceLocation> stylesheets) {
        this.data = data;
        this.builtinStyles = builtinStyles;
        this.stylesheets.addAll(stylesheets);
    }

    private UITemplate(CompoundTag data, String builtinStyles, ResourceLocation... stylesheets) {
        this(data, builtinStyles, stylesheets == null ? Collections.emptyList() : List.of(stylesheets));
    }

    public void setBuiltinStyles(@Nullable String builtinStyles) {
        this.builtinStyles = builtinStyles;
        this.customStylesheet = null;
    }

    @Nullable
    public Stylesheet getCustomStylesheet() {
        if (builtinStyles != null) {
            if (customStylesheet == null) {
                customStylesheet = Stylesheet.parse(builtinStyles);
                customStylesheet.setName("builtin");
            }
            return customStylesheet;
        }
        return null;
    }

    public UI createUI() {
        var root = new UIElement();
        initUI(root);
        var ui = UI.of(root, getAllStylesheets());
        var event = new CreateUI(this, ui);
        NeoForge.EVENT_BUS.post(event);
        return event.ui;
    }

    public void initUI(UIElement root) {
        root.deserializeNBT(Platform.getFrozenRegistry(), data);
    }

    public List<Stylesheet> getAllStylesheets() {
        var stylesheets = new ArrayList<Stylesheet>();
        var customStylesheet = getCustomStylesheet();
        if (customStylesheet != null) {
            stylesheets.add(customStylesheet);
        }
        for (var location : this.stylesheets) {
            var stylesheet = StylesheetManager.INSTANCE.getStylesheet(location);
            if (stylesheet != null) {
                stylesheets.add(stylesheet);
            }
        }
        return stylesheets;
    }

    public static UITemplate of(CompoundTag data, String customStylesheet, ResourceLocation... styles) {
        return new UITemplate(data, customStylesheet, styles);
    }

    public static UITemplate of(UIElement root) {
        return new UITemplate(root.serializeNBT(Platform.getFrozenRegistry()), null);
    }

    public static UITemplate of(UIElement root, ResourceLocation... styles) {
        return new UITemplate(root.serializeNBT(Platform.getFrozenRegistry()), null, styles);
    }

    public static UITemplate missing() {
        if (MISSING == null) {
            MISSING = UITemplate.of(new Label().setText("Missing")
                    .textStyle(textStyle -> textStyle.textColor(ColorPattern.RED.color)));
        }
        return MISSING;
    }

    public UITemplate copy() {
        return new UITemplate(
                data.copy(),
                builtinStyles,
                stylesheets
        );
    }

    public void copyStylesFrom(UITemplate other) {
        this.builtinStyles = other.builtinStyles;
        this.stylesheets.clear();
        this.stylesheets.addAll(other.stylesheets);
    }

    public static class CreateUI extends Event {
        public final UITemplate template;
        public UI ui;

        public CreateUI(UITemplate template, UI ui) {
            this.template = template;
            this.ui = ui;
        }
    }
}
