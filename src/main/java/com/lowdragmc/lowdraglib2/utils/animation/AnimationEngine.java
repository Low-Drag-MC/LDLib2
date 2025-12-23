package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AnimationEngine {
    private final long startTime = System.nanoTime();
    // runtime
    private final Queue<KeyFrameAnimation> waitToPlay = new ConcurrentLinkedQueue<>();
    private final Map<KeyFrameAnimation, AnimationRuntime> runtimes = new HashMap<>();

    public ISubscription play(KeyFrameAnimation animation) {
        waitToPlay.add(animation);
        return () -> {
            waitToPlay.remove(animation);
            runtimes.remove(animation);
        };
    }

    public float getAppTime() {
        long elapsed = System.nanoTime() - startTime;
        return (float) (elapsed / 1_000_000_000.0);
    }

    public void updateFrame() {
        var time = getAppTime();
        KeyFrameAnimation pending;
        while ((pending = waitToPlay.poll()) != null) {
            runtimes.put(pending, new AnimationRuntime(time, pending));
        }

        var iter = runtimes.entrySet().iterator();
        // time in second
        while (iter.hasNext()) {
            var entry = iter.next();
            var runtime = entry.getValue();
            runtime.update(time);
            if (runtime.isFinished()) {
                iter.remove();
            }
        }
    }
}
