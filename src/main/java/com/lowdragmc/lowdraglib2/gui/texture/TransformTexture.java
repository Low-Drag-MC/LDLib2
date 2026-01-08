package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.syncdata.annotation.SkipPersistedValue;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * @author KilaBash
 * @date 2022/12/5
 * @implNote TransformTexture
 */
@Getter
public abstract class TransformTexture implements IGuiTexture {
    @Configurable(name = "Transform", subConfigurable = true)
    protected final Transform2D transform2D = new Transform2D();

    public TransformTexture rotate(float degree) {
        transform2D.rotation(degree);
        return this;
    }

    public TransformTexture scale(float scale) {
        transform2D.scale(scale);
        return this;
    }

    public TransformTexture scale(float width, float height) {
        transform2D.scale(width, height);
        return this;
    }

    public TransformTexture transform(float xOffset, float yOffset) {
        transform2D.translate(xOffset, yOffset);
        return this;
    }

    @Override
    public void beforeDeserialize() {
        transform2D.setIdentity();
    }

    @SkipPersistedValue(field = "transform2D")
    private boolean skipTransform2DPersisted(Transform2D transform2D) {
        return transform2D.isIdentity();
    }

    public void copyTransform(TransformTexture transformTexture) {
        transform2D.copyFrom(transformTexture.transform2D);
    }

    public void copyTransform(Transform2D transform) {
        transform2D.copyFrom(transform);
    }

    @OnlyIn(Dist.CLIENT)
    protected void preDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        transform2D.pushPose(graphics.pose(), x, y, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    protected void postDraw(GuiGraphics graphics, float x, float y, float width, float height) {
        transform2D.popPose(graphics.pose());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public final void draw(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks) {
        preDraw(graphics, x, y, width, height);
        drawInternal(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
        postDraw(graphics, x, y, width, height);
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract void drawInternal(GuiGraphics graphics, float mouseX, float mouseY, float x, float y, float width, float height, float partialTicks);

    @OnlyIn(Dist.CLIENT)
    protected void drawInternal(GUIContext context, float x, float y, float width, float height) {
        drawInternal(context.graphics, context.localMouseX, context.localMouseY, x, y, width, height, context.partialTick);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(GUIContext context, float x, float y, float width, float height) {
        preDraw(context.graphics, x, y, width, height);
        drawInternal(context, x, y, width, height);
        postDraw(context.graphics, x, y, width, height);
    }
}
