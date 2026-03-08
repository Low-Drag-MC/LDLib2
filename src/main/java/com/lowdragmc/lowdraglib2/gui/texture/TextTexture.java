package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@KJSBindings
@LDLRegisterClient(name = "text_texture", registry = "ldlib2:gui_texture")
public class TextTexture extends TransformTexture {
    @Configurable
    public String text;

    @Configurable
    @ConfigColor
    public int color;

    @Configurable
    @ConfigColor
    public int backgroundColor;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_width")
    @ConfigNumber(range = {1, Integer.MAX_VALUE})
    public int width;

    @Configurable
    @ConfigNumber(range = {0, Integer.MAX_VALUE})
    @Setter
    public float rollSpeed = 1;

    @Configurable
    public boolean dropShadow;

    @Configurable(tips = "ldlib.gui.editor.tips.image_text_type")
    public TextType type;

    public Supplier<String> supplier;
    List<String> texts;
    long lastTick;

    public TextTexture() {
        this("A", -1);
        setWidth(50);
    }

    public TextTexture(String text, int color) {
        this.color = color;
        this.type = TextType.NORMAL;
        this.text = LocalizationUtils.format(text);
        this.texts = Collections.singletonList(this.text);
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
        }
    }

    public TextTexture(String text) {
        this(text, -1);
        setDropShadow(true);
    }

    public TextTexture(Supplier<String> text) {
        this("", -1);
        setSupplier(text);
        setDropShadow(true);
    }

    public TextTexture setSupplier(Supplier<String> supplier) {
        this.supplier = supplier;
        return this;
    }

    @ConfigSetter(field = "text")
    public void updateText(String text) {
        this.text = LocalizationUtils.format(text);
        this.texts = Collections.singletonList(this.text);
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
        }
    }

    public TextTexture setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public TextTexture setColor(int color) {
        this.color = color;
        return this;
    }

    public TextTexture setDropShadow(boolean dropShadow) {
        this.dropShadow = dropShadow;
        return this;
    }

    public TextTexture setWidth(int width) {
        this.width = width;
        if (LDLib2.isClient()) {
            TextTextureClientSupport.refreshTexts(this);
        }
        return this;
    }

    public TextTexture setType(TextType type) {
        this.type = type;
        return this;
    }

    @Override
    public TextTexture copy() {
        var copied = new TextTexture(text, color);
        copied.type = type;
        copied.dropShadow = dropShadow;
        copied.rollSpeed = rollSpeed;
        copied.width = width;
        copied.backgroundColor = backgroundColor;
        copied.supplier = supplier;
        copied.texts = List.copyOf(texts);
        copied.copyTransform(this);
        return copied;
    }

    public int getLines() {
        return texts.size();
    }

    public enum TextType {
        NORMAL,
        HIDE,
        ROLL,
        ROLL_ALWAYS,
        LEFT,
        RIGHT,
        LEFT_HIDE,
        LEFT_ROLL,
        LEFT_ROLL_ALWAYS
    }
}
