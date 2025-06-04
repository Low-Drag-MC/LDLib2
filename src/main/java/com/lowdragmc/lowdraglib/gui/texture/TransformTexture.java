package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib.gui.ui.data.Pivot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TransformTexture
 */
@Getter
@Configurable(name = "ldlib.gui.editor.group.transform")
public abstract class TransformTexture implements IGuiTexture {
    @Configurable
    protected Pivot pivot = Pivot.CENTER;
    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1)
    protected float xOffset;

    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1)
    protected float yOffset;

    @Configurable
    @ConfigNumber(range = {0, Float.MAX_VALUE})
    protected float scale = 1;

    @Configurable
    @ConfigNumber(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 5)
    protected float rotation;

    public TransformTexture rotate(float degree) {
        rotation = degree;
        return this;
    }

    public TransformTexture scale(float scale) {
        this.scale = scale;
        return this;
    }

    public TransformTexture transform(float xOffset, float yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    protected void preDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        graphics.pose().pushPose();
        graphics.pose().translate(xOffset, yOffset, 0);

        var xPivot = pivot.getX() * width;
        var yPivot = pivot.getY() * height;
        var translationX = x + xPivot;
        var translationY = y + yPivot;

        graphics.pose().translate(translationX, translationY, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().mulPose(new Quaternionf().rotationXYZ(0, 0, (float) Math.toRadians(rotation)));
        graphics.pose().translate(-translationX, -translationY, 0);
    }

    @OnlyIn(Dist.CLIENT)
    protected void postDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        graphics.pose().popPose();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public final void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        preDraw(graphics, x, y, width, height);
        drawInternal(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
        postDraw(graphics, x, y, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks);

}
