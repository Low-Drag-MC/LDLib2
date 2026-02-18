package com.lowdragmc.lowdraglib2.gui.ui.style.animation;

import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleBag;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.lowdragmc.lowdraglib2.utils.animation.*;
import lombok.Getter;

import org.jetbrains.annotations.Nullable;

public class TransitionAnimation<T> implements IFrameValueHandler<T> {
    public final StyleBag styleBag;
    public final Property<T> property;
    public final Animation animation;
    // runtime
    @Nullable
    @Getter
    private T initialValue;
    @Nullable
    @Getter
    private T currentValue;
    @Nullable
    @Getter
    private T targetValue;
    @Nullable
    @Getter
    private ISubscription subscription;

    public TransitionAnimation(StyleBag styleBag,
                               Property<T> property,
                               Animation animation) {
        this.styleBag = styleBag;
        this.property = property;
        this.animation = animation;
    }

    public void play(T initialValue, T targetValue) {
        var mui = styleBag.element.getModularUI();
        if (mui != null) {
            this.initialValue = initialValue;
            this.currentValue = initialValue;
            this.targetValue = targetValue;
            var keyExecutor = new KFExecutor<>(KeyFrames.of(property.getInterpolator(), initialValue, targetValue), this);
            subscription = mui.getAnimationEngine().play(KeyFrameAnimation.of(animation, keyExecutor));
        }
    }

    @Override
    public void accept(AnimationRuntime runtime, T value) {
        var oldValue = this.currentValue;
        this.currentValue = value;
        styleBag.onTransitionUpdate(this, oldValue, value);
    }

    @Override
    public void onFinished(AnimationRuntime runtime) {
        subscription = null;
        currentValue = null;
        targetValue = null;
        styleBag.onTransitionFinished(this);
    }

    public void stop() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
            currentValue = null;
            targetValue = null;
            styleBag.onTransitionFinished(this);
        }
    }
}
