package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataConsumer;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.IDataProvider;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.IPausable;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import lombok.Getter;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModularUIREIWidget extends Widget {
    public final ModularUI modularUI;
    public final Rectangle bounds;
    // runtime
    @Getter
    private Matrix4f localToWorld = new Matrix4f();

    public ModularUIREIWidget(ModularUI modularUI, Rectangle bounds) {
        this.modularUI = modularUI;
        this.bounds = bounds;
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.flush();

        // fix transform
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(bounds.x, bounds.y, 0);
        localToWorld = guiGraphics.pose().last().pose().invert(new Matrix4f());
        var realMouse = getWorldMouse((mouseX - bounds.x), (mouseY - bounds.y));
        modularUI.getWidget().render(guiGraphics, (int) realMouse.x, (int) realMouse.y, partialTick);
        pose.popPose();

        // check tooltips
        if (!modularUI.getDragHandler().isDragging() && modularUI.getTooltipTexts() != null && !modularUI.getTooltipTexts().isEmpty()) {
            var tooltip = Tooltip.create(modularUI.getTooltipTexts());
            if (modularUI.getTooltipComponent() != null) tooltip.add(modularUI.getTooltipComponent());
            if (ConfigObject.getInstance().shouldAppendModNames()) {
                var stack = modularUI.getTooltipStack();
                var modId = stack.getItem() instanceof Item item ? BuiltInRegistries.ITEM.getKey(item).getNamespace() : null;
                ClientHelper.getInstance().appendModIdToTooltips(tooltip, modId);
            }
            tooltip.queue();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) (mouseX - bounds.x), (float) (mouseY - bounds.y));
        return modularUI.getWidget().mouseClicked(realMouse.x, realMouse.y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        var realMouse = getWorldMouse((float) (mouseX - bounds.x), (float) (mouseY - bounds.y));
        return modularUI.getWidget().mouseReleased(realMouse.x, realMouse.y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var realMouse = getWorldMouse((float) (mouseX - bounds.x), (float) (mouseY - bounds.y));
        var realDrag = getWorldMouseNormal((float) dragX, (float) dragY);
        return modularUI.getWidget().mouseDragged(realMouse.x, realMouse.y, button, realDrag.x, realDrag.y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        var realMouse = getWorldMouse((float) (mouseX - bounds.x), (float) (mouseY - bounds.y));
        return modularUI.getWidget().mouseScrolled(realMouse.x, realMouse.y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return modularUI.getWidget().keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return modularUI.getWidget().charTyped(codePoint, modifiers);
    }

    @Override
    public void setFocused(boolean focused) {
        modularUI.getWidget().setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return modularUI.getWidget().isFocused();
    }


    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }
}
