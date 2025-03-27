package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.math.Rect;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector4f;

/**
 * FreeGraphView allows user to freely explore its view with any scale and offset.
 */
public class FreeGraphView extends WidgetGroup {
    @Getter
    @Setter
    protected float xOffset, yOffset;
    @Getter
    @Setter
    protected float scale = 1;
    @Getter
    @Setter
    protected boolean drawGrid;
    @Getter
    @Setter
    protected int gridWidth = 50;
    @Getter
    @Setter
    protected boolean useScissor = true;

    // runtime
    protected double lastMouseX, lastMouseY;
    protected boolean isDragging = false;

    public FreeGraphView(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void resetFitScaleByWidgets() {
        int minX, minY, maxX, maxY;
        if (this.widgets.isEmpty()) {
            this.xOffset = 0;
            this.yOffset = 0;
            this.scale = 1;
            return;
        }
        minX = minY = Integer.MAX_VALUE;
        maxX = maxY = Integer.MIN_VALUE;
        for (var widget : this.widgets) {
            var position = widget.getPosition();
            minX = Math.min(minX, position.x - 120);
            minY = Math.min(minY, position.y - 20);
            maxX = Math.max(maxX, position.x + widget.getSize().width + 20);
            maxY = Math.max(maxY, position.y + widget.getSize().height + 20);
        }
        this.xOffset = minX;
        this.yOffset = minY;
        var scaleWidth = (float) getSize().width / (maxX - minX);
        var scaleHeight = (float) getSize().height / (maxY - minY);
        this.scale = Math.min(scaleWidth, scaleHeight);
        if (scale < 0.5f) {
            this.scale = 0.5f;
        }
        this.xOffset -= (getSize().width / scale - (maxX - minX)) / 2;
        this.yOffset -= (getSize().height / scale - (maxY - minY)) / 2;
    }

    public void resetFitScaleWithArea(Rect area) {
        var minX = area.left;
        var minY = area.up;
        var maxX = area.right;
        var maxY = area.down;
        // set the view offset and scale by range of visible nodes
        this.xOffset = minX;
        this.yOffset = minY;
        var scaleWidth = (float) getSize().width / (maxX - minX);
        var scaleHeight = (float) getSize().height / (maxY - minY);
        this.scale = Math.min(scaleWidth, scaleHeight);
        if (scale < 0.5f) {
            this.scale = 0.5f;
        }
        this.xOffset -= (getSize().width / scale - (maxX - minX)) / 2;
        this.yOffset -= (getSize().height / scale - (maxY - minY)) / 2;
    }

    public Vector2d getViewPosition(double x, double y) {
        var realX = ((x - this.getPositionX()) / scale + xOffset);
        var realY = ((y - this.getPositionY()) / scale + yOffset);
        return new Vector2d(realX, realY);
    }

    public Vector2i getViewPosition(int x, int y) {
        var realX = ((x - this.getPositionX()) / scale + xOffset);
        var realY = ((y - this.getPositionY()) / scale + yOffset);
        return new Vector2i((int) realX, (int) realY);
    }

    public Vector2f getViewPosition(float x, float y) {
        var realX = ((x - this.getPositionX()) / scale + xOffset);
        var realY = ((y - this.getPositionY()) / scale + yOffset);
        return new Vector2f(realX, realY);
    }

    /********** Correct Event Position and Rendering for child widgets. **********/
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        var realMouse = getViewPosition(mouseX, mouseY);
        if (super.mouseClicked(realMouse.x, realMouse.y, button)) {
            return true;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 0) {
                isDragging = true;
            }
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        var realMouse = getViewPosition(mouseX, mouseY);
        return super.mouseReleased(realMouse.x, realMouse.y, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // update view offset
        if (isDragging) {
            xOffset += (float) (lastMouseX - mouseX) / scale;
            yOffset += (float) (lastMouseY - mouseY) / scale;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        var realMouse = getViewPosition(mouseX, mouseY);
        dragX = dragX / scale;
        dragY = dragY / scale;
        return super.mouseDragged(realMouse.x, realMouse.y, button, dragX, dragY) || isDragging;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelX, double wheelY) {
        // update view scale
        var realMouse = getViewPosition(mouseX, mouseY);
        if (super.mouseWheelMove(realMouse.x, realMouse.y, wheelX, wheelY)) {
            return true;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            var newScale = (float) Mth.clamp(scale + wheelY * 0.1f, 0.1f, 10f);
            if (newScale != scale) {
                xOffset += (float) (mouseX - this.getPositionX()) / scale - (float) (mouseX - this.getPositionX()) / newScale;
                yOffset += (float) (mouseY - this.getPositionY()) / scale - (float) (mouseY - this.getPositionY()) / newScale;
                scale = newScale;
            }
            return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseMoved(double mouseX, double mouseY) {
        var realMouse = getViewPosition(mouseX, mouseY);
        return super.mouseMoved(realMouse.x, realMouse.y);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTooltipTexts(mouseX, mouseY);
        var realMouse = getViewPosition(mouseX, mouseY);

        graphics.pose().pushPose();
        graphics.pose().translate(this.getPositionX(), this.getPositionY(), 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-xOffset, -yOffset, 0);
        drawWidgetsForeground(graphics, realMouse.x, realMouse.y, partialTicks);
        graphics.pose().popPose();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.drawBackgroundTexture(graphics, mouseX, mouseY);

        var realMouse = getViewPosition(mouseX, mouseY);
        var pos = getPosition();
        var size = getSize();

        if (useScissor) {
            var trans = graphics.pose().last().pose();
            var realPos = trans.transform(new Vector4f(pos.x, pos.y, 0, 1));
            var realPos2 = trans.transform(new Vector4f(pos.x + size.width, pos.y + size.height, 0, 1));
            graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(this.getPositionX(), this.getPositionY(), 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().translate(-xOffset, -yOffset, 0);

        if (drawGrid) {
            graphics.drawManaged(() -> {
                var w = size.width / scale;
                var h = size.height / scale;
                var sx = (int) (pos.x / scale + xOffset - w - xOffset % gridWidth);
                var sy = (int) (pos.y / scale + yOffset - h - yOffset % gridWidth);
                sx -= (sx % gridWidth);
                sy -= (sy % gridWidth);
                for (int x = sx; x < sx + 3 * w; x += gridWidth) {
                    for (int y = sy; y < sy + 3 * h; y += gridWidth) {
                        DrawerHelper.drawSolidRect(graphics, x, sy, 1, (int) (3 * h), ColorPattern.T_GRAY.color);
                        DrawerHelper.drawSolidRect(graphics, sx, y, (int) (3 * w), 1, ColorPattern.T_GRAY.color);
                    }
                }
            });
        }

        drawWidgetsBackground(graphics, realMouse.x, realMouse.y, partialTicks);

        graphics.pose().popPose();

        if (useScissor) graphics.disableScissor();
    }

}
