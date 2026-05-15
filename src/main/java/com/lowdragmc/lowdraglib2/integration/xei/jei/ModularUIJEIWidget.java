package com.lowdragmc.lowdraglib2.integration.xei.jei;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import lombok.Getter;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ModularUIJEIWidget implements IRecipeWidget, IJeiGuiEventListener {
    public static final ScreenPosition ZERO = new ScreenPosition(0, 0);
    public final ModularUI modularUI;
    // runtime
    @Getter
    private Matrix3x2f localToWorld = new Matrix3x2f();

    public ModularUIJEIWidget(ModularUI modularUI) {
        this.modularUI = modularUI;
    }

    /// IRecipeWidget
    @Override
    public ScreenPosition getPosition() {
        return ModularUIJEIWidget.ZERO;
    }

    @Override
    public void drawWidget(GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        var partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        // get real mouse
        localToWorld = guiGraphics.pose().invert(localToWorld);
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        ModularUIClientAccess.getWidget(modularUI).extractRenderState(guiGraphics, (int) realMouse.x, (int) realMouse.y, partialTick);
    }

    public Vector2f getWorldMouse(float mouseX, float mouseY) {
        var realMouse = localToWorld.transformPosition(new Vector2f(0, 0))
                .mul(-1)
                .add(mouseX, mouseY);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    public Vector2f getWorldMouseNormal(float mouseX, float mouseY) {
        var realMouse = localToWorld.transformDirection(new Vector2f(0, 0))
                .mul(-1)
                .add(mouseX, mouseY);
        return new Vector2f(realMouse.x, realMouse.y);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltipBuilder, double mouseX, double mouseY) {
        if (!modularUI.getDragHandler().isDragging()) {
            var hoverTooltips = ModularUIClientAccess.getHoverTooltips(modularUI);
            if (hoverTooltips == null) return;
            for (var tooltip : hoverTooltips.tooltips()) {
                if (tooltip == null) continue;
                switch (tooltip) {
                    case TooltipComponent tooltipComponent -> tooltipBuilder.add(tooltipComponent);
                    case Component component -> tooltipBuilder.add(component);
                    default -> tooltipBuilder.add(Component.literal(tooltip.toString()));
                }
            }
        }
    }

    @Override
    public void tick() {
        modularUI.tick();
    }

    /// IJeiGuiEventListener
    @Override
    public ScreenRectangle getArea() {
        return ModularUIClientAccess.getWidget(modularUI).getRectangle();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        ModularUIClientAccess.getWidget(modularUI).mouseMoved(realMouse.x, realMouse.y);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return ModularUIClientAccess.getWidget(modularUI).mouseClicked(
                new MouseButtonEvent(realMouse.x, realMouse.y, new MouseButtonInfo(button, 0)), false
        );
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return ModularUIClientAccess.getWidget(modularUI).mouseReleased(
                new MouseButtonEvent(realMouse.x, realMouse.y, new MouseButtonInfo(button, 0))
        );
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        var realDrag = getWorldMouseNormal((float) dragX, (float) dragY);
        return ModularUIClientAccess.getWidget(modularUI).mouseDragged(
                new MouseButtonEvent(realMouse.x, realMouse.y, new MouseButtonInfo(button, 0)),
                realDrag.x, realDrag.y
        );
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var realMouse = getWorldMouse((float) mouseX, (float) mouseY);
        return ModularUIClientAccess.getWidget(modularUI).mouseScrolled(realMouse.x, realMouse.y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        return ModularUIClientAccess.getWidget(modularUI).keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }
}
