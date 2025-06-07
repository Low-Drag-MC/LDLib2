package com.lowdragmc.lowdraglib2.gui.animation;

import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * @author KilaBash
 * @date 2022/9/8
 * @implNote MoveIn
 */
@Accessors(chain = true, fluent = true)
public class Transform extends Animation {
    protected int xOffset, yOffset;
    @Setter
    protected float scale = 1;
    protected boolean in;

    public Transform offset(int xOffset, int yOffset) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        return this;
    }

    public Transform setScale(float scale) {
        this.scale = scale;
        return this;
    }

    @Override
    public Transform duration(long duration) {
        return (Transform) super.duration(duration);
    }

    @Override
    public Transform delay(long delay) {
        return (Transform) super.delay(delay);
    }

    @Override
    public Transform ease(IEase ease) {
        return (Transform) super.ease(ease);
    }

    @Override
    public Transform onUpdate(FloatConsumer onUpdate) {
        return (Transform) super.onUpdate(onUpdate);
    }

    @Override
    public Animation onFinish(Runnable onFinish) {
        return super.onFinish(onFinish);
    }

    @Override
    public Animation size(@Nullable Size size) {
        return super.size(size);
    }

    @Override
    public Animation position(@Nullable Position position) {
        return super.position(position);
    }

    public boolean isIn() {
        return in;
    }

    public boolean isOut() {
        return !in;
    }

    public Animation setIn() {
        this.in = true;
        return this;
    }

    public Animation setOut() {
        this.in = false;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    public void pre(@NotNull GuiGraphics graphics) {
        graphics.pose().pushPose();
        Position position = widget.getPosition();
        Size size = widget.getSize();
        float oX = position.x + size.width / 2f;
        float oY = position.y + size.height / 2f;
        if (isIn()) {
            graphics.pose().translate(xOffset * (1 - getTime()), yOffset * (1 - getTime()), 0);
        } else {
            graphics.pose().translate(xOffset * getTime(), yOffset * getTime(), 0);
        }
        graphics.pose().translate(oX, oY,0);
        if (isIn()) {
            graphics.pose().scale(scale + (1 - scale) * getTime(), scale + (1 - scale) * getTime(), 1);
        } else {
            graphics.pose().scale(scale + (1 - scale) * (1- getTime()), scale + (1 - scale) * (1- getTime()), 1);
        }
        graphics.pose().translate(-oX, -oY,0);
    }

    @OnlyIn(Dist.CLIENT)
    public void post(@NotNull GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        float tickTime = getTick();
        if (tickTime >= delay) {
            if (interpolator != null) {
                interpolator.update(tickTime);
            }
            pre(graphics);
            widget.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            post(graphics);
        } else if (isOut()) {
            widget.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        float tickTime = getTick();
        if (tickTime >= delay) {
            if (interpolator != null) {
                interpolator.update(tickTime);
            }
            pre(graphics);
            widget.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            post(graphics);
        } else if (isOut()) {
            widget.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }
}
