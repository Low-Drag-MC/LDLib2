package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleSheet;
import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;
import org.appliedenergistics.yoga.YogaConstants;

import javax.annotation.Nullable;

public class ModularUI {
    public final UIElement rootElement;
    public final StyleSheet styleSheet;
    // runtime
    @Getter
    @Nullable
    private Screen screen;
    @Getter
    private int screenWidth;
    @Getter
    private int screenHeight;
    @Getter
    private float leftPos;
    @Getter
    private float topPos;

    public ModularUI(UIElement rootElement, StyleSheet styleSheet) {
        this.rootElement = rootElement;
        this.styleSheet = styleSheet;
    }

    public ModularUI(UIElement rootElement) {
        this(rootElement, new StyleSheet());
    }

    public ModularUI() {
        this(new UIElement(), new StyleSheet());
    }

    public void setScreen(@Nullable ModularUIContainerScreen screen) {
        this.screen = screen;
    }

    public void init(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        var width = rootElement.getLayout().getWidth();
        var height = rootElement.getLayout().getHeight();
        switch (width.unit) {
            case PERCENT -> this.leftPos = (screenWidth - width.value * screenWidth * 0.01f) / 2;
            case POINT -> this.leftPos = (screenWidth - width.value) / 2;
            default -> this.leftPos = 0;
        }
        switch (height.unit) {
            case PERCENT -> this.topPos = (screenHeight - height.value * screenHeight * 0.01f) / 2;
            case POINT -> this.topPos = (screenHeight - height.value) / 2;
            default -> this.topPos = 0;
        }
        this.rootElement._setModularUIInternal(this);
        rootElement.init(screenWidth, screenHeight);
        rootElement.calculateLayout();
    }

}
