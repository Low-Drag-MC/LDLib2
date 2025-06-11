package com.lowdragmc.lowdraglib2.gui.widget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import com.lowdragmc.lowdraglib2.math.GradientColor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Setter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2023/5/30
 * @implNote GradientColorWidget
 */
public class GradientColorWidget extends WidgetGroup {
    protected final GradientColor gradientColor;
    protected final HsbColorWidget hsbColorWidget;
    protected int selectedAlphaPoint = -1, selectedRGBPoint = 0;
    @Setter
    protected Consumer<GradientColor> onUpdate;
    // runtime
    private boolean isDraggingAlphaPoint, isDraggingRGBPoint;
    private long lastClickTick;

    public GradientColorWidget(int x, int y, int width, GradientColor gradientColor) {
        super(x, y, width, width + 15 + 20 + 3);
        this.gradientColor = gradientColor;
        addWidget(new ImageWidget(3, 3 + 10, width - 6, 15, ColorPattern.WHITE.borderTexture(1)));

        addWidget(hsbColorWidget = new HsbColorWidget(3, 15 + 20 + 3, width - 6, width - 6)
                .setColorSupplier(() -> {
                    var t = selectedAlphaPoint >= 0 ? gradientColor.getAP().get(selectedAlphaPoint).x :
                            selectedRGBPoint >= 0 ? gradientColor.getRP().get(selectedRGBPoint).x :
                            0;
                    return gradientColor.getColor(t);
                })
                .setOnChanged(color -> {
                    if (selectedAlphaPoint >= 0) {
                        gradientColor.getAP().set(selectedAlphaPoint, new Vector2f(gradientColor.getAP().get(selectedAlphaPoint).x, ColorUtils.alpha(color)));
                        notifyChanged();
                    }
                    if (selectedRGBPoint >= 0) {
                        gradientColor.getRP().set(selectedRGBPoint, new Vector2f(gradientColor.getRP().get(selectedRGBPoint).x, ColorUtils.red(color)));
                        gradientColor.getGP().set(selectedRGBPoint, new Vector2f(gradientColor.getGP().get(selectedRGBPoint).x, ColorUtils.green(color)));
                        gradientColor.getBP().set(selectedRGBPoint, new Vector2f(gradientColor.getBP().get(selectedRGBPoint).x, ColorUtils.blue(color)));
                        notifyChanged();
                    }
                }));
        hsbColorWidget.setShowAlpha(false);
        hsbColorWidget.setShowRGB(true);
    }

    protected void notifyChanged() {
        if (onUpdate != null) onUpdate.accept(gradientColor);
    }

    private float getXPosition(float t) {
        var size = getSize();
        var pos = getPosition();
        return pos.x + 3 + (size.width - 6) * t;
    }

    private float getXCoord(float x) {
        var size = getSize();
        var position = getPosition();
        return (x - position.x - 3) / (size.width - 6);
    }

    protected void openMenu(int mouseX, int mouseY) {
        if ((selectedAlphaPoint >= 0 && gradientColor.getAP().size() > 1) || (selectedRGBPoint >= 0 && gradientColor.getRP().size() > 1)) {
            var menu = TreeBuilder.Menu.start().leaf("Remove", () -> {
                if (selectedAlphaPoint >= 0) {
                    gradientColor.getAP().remove(selectedAlphaPoint);
                    selectedAlphaPoint = -1;
                    notifyChanged();
                } else if (selectedRGBPoint >= 0) {
                    gradientColor.getRP().remove(selectedRGBPoint);
                    gradientColor.getGP().remove(selectedRGBPoint);
                    gradientColor.getBP().remove(selectedRGBPoint);
                    selectedRGBPoint = -1;
                    notifyChanged();
                }
            });
            var widget = new MenuWidget<>(mouseX - getPosition().x, mouseY - getPosition().y, 14, menu.build())
                    .setNodeTexture(MenuWidget.NODE_TEXTURE)
                    .setLeafTexture(MenuWidget.LEAF_TEXTURE)
                    .setNodeHoverTexture(MenuWidget.NODE_HOVER_TEXTURE)
                    .setCrossLinePredicate(TreeBuilder.Menu::isCrossLine)
                    .setKeyIconSupplier(TreeBuilder.Menu::getIcon)
                    .setKeyNameSupplier(TreeBuilder.Menu::getName)
                    .setOnNodeClicked(TreeBuilder.Menu::handle);
            waitToAdded(widget.setBackground(MenuWidget.BACKGROUND));
        }

    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            if (button == 1) {
                openMenu((int) mouseX, (int) mouseY);
            } else {
                var clickTick = gui.getTickCount();
                var size = getSize();
                var pos = getPosition();
                // click alpha point
                for (int i = 0; i < gradientColor.getAP().size(); i++) {
                    if (isMouseOver((int) (getXPosition(gradientColor.getAP().get(i).x) - 5), pos.y + 3, 10, 10, mouseX, mouseY)) {
                        selectedAlphaPoint = i;
                        selectedRGBPoint = -1;
                        isDraggingAlphaPoint = true;
                        hsbColorWidget.setShowAlpha(true);
                        hsbColorWidget.setShowRGB(false);
                        return true;
                    }
                }
                // click rgb point
                for (int i = 0; i < gradientColor.getRP().size(); i++) {
                    if (isMouseOver((int) (getXPosition(gradientColor.getRP().get(i).x) - 5), pos.y + 3 + 10 + 15, 10, 10, mouseX, mouseY)) {
                        selectedRGBPoint = i;
                        selectedAlphaPoint = -1;
                        isDraggingRGBPoint = true;
                        hsbColorWidget.setShowAlpha(false);
                        hsbColorWidget.setShowRGB(true);
                        return true;
                    }
                }
                // double click
                if (clickTick - lastClickTick < 12) {
                    // create alpha point
                    if (isMouseOver(pos.x + 3, pos.y + 3, size.width - 6, 10, mouseX, mouseY)) {
                        int color = gradientColor.getColor(getXCoord((float)mouseX));
                        gradientColor.addAlpha(getXCoord((float)mouseX), ColorUtils.alpha(color));
                        notifyChanged();
                        return true;
                    }
                    // create rgb point
                    if (isMouseOver(pos.x + 3, pos.y + 3 + 10 + 15, size.width - 6, 10, mouseX, mouseY)) {
                        int color = gradientColor.getColor(getXCoord((float)mouseX));
                        gradientColor.addRGB(getXCoord((float) mouseX), ColorUtils.red(color), ColorUtils.green(color), ColorUtils.blue(color));
                        notifyChanged();
                        return true;
                    }
                } else {
                    lastClickTick = clickTick;
                }
            }
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingRGBPoint = false;
        this.isDraggingAlphaPoint = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        var size = getSize();
        var pos = getPosition();
        if (mouseX >= pos.x + 3 && mouseX <= pos.x + size.width - 6) {
            if (selectedAlphaPoint >= 0 && this.isDraggingAlphaPoint) {
                var t = getXCoord((float) Mth.clamp(mouseX, pos.x + 3, pos.x + size.width - 6));
                selectedAlphaPoint = gradientColor.addAlpha(t,  gradientColor.getAP().remove(selectedAlphaPoint).y);
                notifyChanged();
                return true;
            }
            if (selectedRGBPoint >= 0 && this.isDraggingRGBPoint) {
                var t = getXCoord((float) Mth.clamp(mouseX, pos.x + 3, pos.x + size.width - 6));
                selectedRGBPoint = gradientColor.addRGB(t,
                        gradientColor.getRP().remove(selectedRGBPoint).y,
                        gradientColor.getGP().remove(selectedRGBPoint).y,
                        gradientColor.getBP().remove(selectedRGBPoint).y);
                notifyChanged();
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull @Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // render background
        drawBackgroundTexture(graphics, mouseX, mouseY);

        var size = getSize();
        var pos = getPosition();

        // render point
        for (int i = 0; i < gradientColor.getAP().size(); i++) {
            Icons.DOWN.copy().setColor(i == selectedAlphaPoint ? ColorPattern.GREEN.color : -1).draw(graphics, mouseX, mouseY, getXPosition(gradientColor.getAP().get(i).x) - 5, pos.y + 3, 10, 10);
        }

        for (int i = 0; i < gradientColor.getRP().size(); i++) {
            Icons.UP.copy().setColor(i == selectedRGBPoint ? ColorPattern.GREEN.color : -1).draw(graphics, mouseX, mouseY, getXPosition(gradientColor.getRP().get(i).x) - 5, pos.y + 3 + 10 + 15, 10, 10);
        }
        // render grid
        var width = size.width - 6;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        var matrix = graphics.pose().last().pose();
        int p = 0;
        for (int x = 0; x < width; x += 3) {
            for (int y = 0; y < 15; y += 3) {
                var isWhite = (p++ % 2) == 0;
                float minX = pos.x + 3 + x;
                float maxX = pos.x + 3 + x + 3;
                float minY = pos.y + 13 + y;
                float maxY = pos.y + 13 + y + 3;
                bufferBuilder.addVertex(matrix, minX, maxY, 0.0f).setColor(isWhite ? -1 : ColorPattern.GRAY.color);
                bufferBuilder.addVertex(matrix, maxX, maxY, 0.0f).setColor(isWhite ? -1 : ColorPattern.GRAY.color);
                bufferBuilder.addVertex(matrix, maxX, minY, 0.0f).setColor(isWhite ? -1 : ColorPattern.GRAY.color);
                bufferBuilder.addVertex(matrix, minX, minY, 0.0f).setColor(isWhite ? -1 : ColorPattern.GRAY.color);
            }
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        // render color bar
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f mat = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var y = pos.y + 13;
        var yh = 15 + y;
        for (int i = 0; i < width; i++) {
            var x = getXPosition(i * 1f / width);
            var xw = getXPosition((i + 1f) / width);
            int startColor = gradientColor.getColor(i * 1f / width);
            int endColor = gradientColor.getColor((i + 1f) / width);
            float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
            float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
            float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
            float startBlue  = (float)(startColor       & 255) / 255.0F;
            float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
            float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
            float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
            float endBlue    = (float)(endColor         & 255) / 255.0F;

            buffer.addVertex(mat,xw, y, 0).setColor(endRed, endGreen, endBlue, endAlpha);
            buffer.addVertex(mat,x, y, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,x, yh, 0).setColor(startRed, startGreen, startBlue, startAlpha);
            buffer.addVertex(mat,xw, yh, 0).setColor(endRed, endGreen, endBlue, endAlpha);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        // render children
        drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);
    }
}
