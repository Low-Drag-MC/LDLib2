package com.lowdragmc.lowdraglib2.integration.xei.rei;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.MinecraftAccessor;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
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
    private long lastTick;

    public ModularUIREIWidget(ModularUI modularUI, Rectangle bounds) {
        this.modularUI = modularUI;
        this.bounds = bounds;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.flush();

        // tick ui
        if (Minecraft.getInstance() instanceof MinecraftAccessor accessor) {
            if (accessor.getClientTickCount() != lastTick) {
                modularUI.tick();
                lastTick = accessor.getClientTickCount();
            }
        }

        // fix transform
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(bounds.x, bounds.y, 0);
        var realMouse = guiGraphics.pose().last().pose().invert(new Matrix4f()).transformPosition(new Vector3f(0, 0, 0));
        modularUI.getWidget().render(guiGraphics, (int) (mouseX - bounds.x - realMouse.x), (int) (mouseY - bounds.y - realMouse.y), partialTick);
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
        return modularUI.getWidget().mouseClicked(mouseX - bounds.x, mouseY - bounds.y, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return modularUI.getWidget().mouseReleased(mouseX - bounds.x, mouseY - bounds.y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return modularUI.getWidget().mouseDragged(mouseX - bounds.x, mouseY - bounds.y, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return modularUI.getWidget().mouseScrolled(mouseX - bounds.x, mouseY - bounds.y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return modularUI.getWidget().keyPressed(keyCode, scanCode, modifiers);
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
