package com.lowdragmc.lowdraglib.gui.ui;

import com.lowdragmc.lowdraglib.gui.ui.style.StyleSheet;
import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

public class ModularUI {
    @Getter
    private final StyleSheet styleSheet = new StyleSheet();
    @Getter
    @Nullable
    private Screen screen;

    public void setScreen(@Nullable ModularUIScreen screen) {
        this.screen = screen;
    }
}
