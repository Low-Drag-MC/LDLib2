package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.NumberRange;
import lombok.Getter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TransformTexture
 */
@Configurable(name = "ldlib.gui.editor.group.transform")
public abstract class TransformTexture implements IGuiTexture {
    @Configurable
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1)
    @Getter
    protected float xOffset;

    @Configurable
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 1)
    @Getter
    protected float yOffset;

    @Configurable
    @NumberRange(range = {0, Float.MAX_VALUE})
    @Getter
    protected float scale = 1;

    @Configurable
    @NumberRange(range = {-Float.MAX_VALUE, Float.MAX_VALUE}, wheel = 5)
    @Getter
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

    @Environment(EnvType.CLIENT)
    protected void preDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        graphics.pose().pushPose();
        graphics.pose().translate(xOffset, yOffset, 0);

        graphics.pose().translate(x + width / 2f, y + height / 2f, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.pose().mulPose(new Quaternionf().rotationXYZ(0, 0, (float) Math.toRadians(rotation)));
        graphics.pose().translate(-x + -width / 2f, -y + -height / 2f, 0);
    }


    @Environment(EnvType.CLIENT)
    protected void postDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        graphics.pose().popPose();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public final void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        preDraw(graphics, x, y, width, height);
        drawInternal(graphics, mouseX, mouseY, x, y, width, height);
        postDraw(graphics, x, y, width, height);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public final void drawSubArea(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
            preDraw(graphics, x, y, width, height);
            drawSubAreaInternal(graphics, x, y, width, height, drawnU, drawnV, drawnWidth, drawnHeight);
            postDraw(graphics, x, y, width, height);
    }

    @Environment(EnvType.CLIENT)
    protected abstract void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height);

    @Environment(EnvType.CLIENT)
    protected void drawSubAreaInternal(GuiGraphics graphics, float x, float y, float width, float height, float drawnU, float drawnV, float drawnWidth, float drawnHeight) {
        drawInternal(graphics, 0, 0, x, y, (int) width, (int) height);
    }

}
