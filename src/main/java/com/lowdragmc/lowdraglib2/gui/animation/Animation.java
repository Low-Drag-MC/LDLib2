package com.lowdragmc.lowdraglib2.gui.animation;

import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import com.lowdragmc.lowdraglib2.math.interpolate.Interpolator;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author KilaBash
 * @date 2022/9/8
 * @implNote Animation
 */
@Accessors(chain = true, fluent = true)
public class Animation {
    protected Widget widget;
    protected Interpolator interpolator;
    protected float time = 0;
    protected boolean isFinish;
    protected long startTick = -1;
    protected boolean init = false;
    // animation
    @Setter
    protected long duration = 250;
    @Setter
    protected long delay = 0;
    @Setter
    protected IEase ease = Eases.EaseLinear;
    @Setter
    protected FloatConsumer onUpdate;
    @Setter
    protected Runnable onFinish;
    @Nullable
    @Setter
    protected Size size;
    @Nullable
    @Setter
    protected Position position;
    // initial data
    protected Size initialSize;
    protected Position initialPosition;

    public Animation setWidget(Widget widget) {
        this.widget = widget;
        return this;
    }

    public Widget getWidget() {
        return widget;
    }

    protected void onInterpolatorUpdate(Number number) {
        time = number.floatValue();
    }

    protected float getTime() {
        return time;
    }

    protected void onInterpolatorFinish(Number number) {
        widget.setActive(true);
        interpolator = null;
        isFinish = true;
        if (onFinish != null) {
            onFinish.run();
        }
    }

    public boolean isFinish() {
        return isFinish;
    }

    public Animation appendOnFinish(Runnable onFinish) {
        if (this.onFinish != null) {
            final Runnable last = this.onFinish;
            this.onFinish = () -> {
                last.run();
                onFinish.run();
            };
        } else {
            this.onFinish = onFinish;
        }
        return this;
    }

    public Runnable getOnFinish() {
        return onFinish;
    }

    protected float getTick() {
        if (!init) {
            init();
        }
        return (System.currentTimeMillis() - startTick);
    }


    protected void init() {
        init = true;
        interpolator = new Interpolator(0, 1, duration, ease, this::onInterpolatorUpdate, this::onInterpolatorFinish);
        startTick = System.currentTimeMillis();
        widget.setActive(false);
        initialSize = widget.getSize();
        initialPosition = widget.getSelfPosition();
    }

    protected void updateWidget(float t) {
        if (onUpdate != null) {
            onUpdate.accept(t);
        }
        if (size != null) {
            widget.setSize(Size.of((int)(size.width * t + initialSize.width * (1 - t)), (int)(size.height * t + initialSize.height * (1 - t))));
        }
        if (position != null) {
            widget.setSelfPosition(Position.of((int)(position.x * t + initialPosition.x * (1 - t)), (int)(position.y * t + initialPosition.y * (1 - t))));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        float tickTime = getTick();
        if (tickTime >= delay) {
            if (interpolator != null) {
                interpolator.update(tickTime);
                updateWidget(getTime());
            }
        }
        widget.drawInBackground(graphics, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        float tickTime = getTick();
        if (tickTime >= delay) {
            if (interpolator != null) {
                interpolator.update(tickTime);
            }
        }
        widget.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

}
