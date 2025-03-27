package com.lowdragmc.lowdraglib.integration.jei;

import lombok.Getter;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public class ModularUIGuiEventListener<T extends ModularWrapper<?>> implements IJeiGuiEventListener {

    private final ModularWrapper<?> wrapper;
    @Getter
    private final ScreenRectangle area;

    public ModularUIGuiEventListener(ModularWrapper<?> wrapper) {
        this.wrapper = wrapper;
        this.area = new ScreenRectangle(0, 0, wrapper.modularUI.getWidth(), wrapper.modularUI.getHeight());
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        wrapper.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return wrapper.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return wrapper.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return wrapper.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return wrapper.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        return wrapper.keyPressed(keyCode, scanCode, modifiers);
    }
}
