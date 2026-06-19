package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.IPausable;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.Getter;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ModularUIJEIWidget implements IRecipeWidget, IJeiGuiEventListener {
    public static final ScreenPosition ZERO = new ScreenPosition(0, 0);
    public final ModularUI modularUI;
    // runtime
    @Getter
    private Matrix4f localToWorld = new Matrix4f();

    public ModularUIJEIWidget(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    /// IRecipeWidget
    @Override
    public ScreenPosition getPosition() {
        return ModularUIJEIWidget.ZERO;
    }

    @Override
    public void drawWidget(GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.flush();
        var partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        // get real mouse
        localToWorld = guiGraphics.pose().last().pose().invert(new Matrix4f());
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        modularUI.getWidget().render(guiGraphics, (int) realMouse.x, (int) realMouse.y, partialTick);
    }

    public Vector2f getWorldMouse(float mouseX, float mouseY) {
        var realMouse = localToWorld.transformPosition(new Vector3f(0, 0, 0))
                .mul(-1)
                .add(mouseX, mouseY, 0);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    public Vector2f getWorldMouseNormal(float mouseX, float mouseY) {
        var realMouse = localToWorld.transformDirection(new Vector3f(0, 0, 0))
                .mul(-1)
                .add(mouseX, mouseY, 0);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
        if (!modularUI.getDragHandler().isDragging() && modularUI.getTooltipTexts() != null && !modularUI.getTooltipTexts().isEmpty()) {
            tooltipBuilder.addAll(modularUI.getTooltipTexts());
            if (modularUI.getTooltipComponent() != null) tooltipBuilder.add(modularUI.getTooltipComponent());
        }
    }

    @Override
    public void tick() {
        modularUI.tick();
    }

    /// IJeiGuiEventListener
    @Override
    public ScreenRectangle getArea() {
        return modularUI.getWidget().getRectangle();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        modularUI.getWidget().mouseMoved(realMouse.x, realMouse.y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return modularUI.getWidget().mouseClicked(realMouse.x, realMouse.y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return modularUI.getWidget().mouseReleased(realMouse.x, realMouse.y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        var realDrag = getWorldMouseNormal((float) dragX, (float) dragY);
        return modularUI.getWidget().mouseDragged(realMouse.x, realMouse.y, button, realDrag.x, realDrag.y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return modularUI.getWidget().mouseScrolled(realMouse.x, realMouse.y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        var result = modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
        // pause scroll
        if (!result && UIElement.isShiftDown() && modularUI.getLastHoveredElement() instanceof IDataConsumer<?> consumer) {
            for (IDataProvider<?> boundDataSource : consumer.getBoundDataSources()) {
                if (boundDataSource instanceof IPausable pausable) {
                    pausable.togglePause();
                }
            }
        }
        return result;
    }
}
