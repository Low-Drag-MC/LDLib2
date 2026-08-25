package com.lowdragmc.lowdraglib2.utils.animation;

import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class AnimationEngine {

    /**
     * One scheduled animation, and the handle {@link #play} gives back so it can be cancelled again.
     *
     * <p>The handle points at whichever engine is running the animation right now rather than the one
     * it was started on, because {@link #handOver} moves animations between engines. A cancel that
     * still reached into the original would quietly do nothing.
     */
    private static final class Playback implements ISubscription {
        private final KeyFrameAnimation animation;
        /**
         * The engine holding this playback, or {@code null} once it has finished or been cancelled.
         */
        @Nullable
        private AnimationEngine engine;
        /**
         * Assigned on the first frame the owning engine draws after this was queued.
         */
        @Nullable
        private AnimationRuntime runtime;

        private Playback(KeyFrameAnimation animation, AnimationEngine engine) {
            this.animation = animation;
            this.engine = engine;
        }

        @Override
        public void unsubscribe() {
            var current = engine;
            if (current == null) return;
            engine = null;
            current.waitToPlay.remove(this);
            current.playing.remove(this);
        }

        /**
         * Whether anything this animation drives is one of the objects {@code moved} accepts.
         */
        private boolean drivesAnyOf(Predicate<Object> moved) {
            for (var executor : animation.kfExecutors()) {
                var owner = executor.handler().owner();
                if (owner != null && moved.test(owner)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final long startTime = System.nanoTime();
    // runtime
    private final Queue<Playback> waitToPlay = new ConcurrentLinkedQueue<>();
    private final List<Playback> playing = new ArrayList<>();

    public ISubscription play(KeyFrameAnimation animation) {
        var playback = new Playback(animation, this);
        waitToPlay.add(playback);
        return playback;
    }

    public float getAppTime() {
        long elapsed = System.nanoTime() - startTime;
        return (float) (elapsed / 1_000_000_000.0);
    }

    public void updateFrame() {
        var time = getAppTime();
        Playback pending;
        while ((pending = waitToPlay.poll()) != null) {
            // Cancelled, or handed to another engine, between being queued and being started.
            if (pending.engine != this) continue;
            pending.runtime = new AnimationRuntime(time, pending.animation);
            playing.add(pending);
        }

        // Walked over a snapshot: a frame handler runs arbitrary code, and an animation that closes a
        // dialog when it finishes goes on to remove elements, start transitions and re-host subtrees,
        // all of which reach back into this list.
        var anyFinished = false;
        for (var playback : List.copyOf(playing)) {
            var runtime = playback.runtime;
            // Skip whatever left this engine, or was cancelled, earlier in this same walk.
            if (runtime == null || playback.engine != this) continue;
            runtime.update(time);
            if (runtime.isFinished()) {
                // Detached here and swept below rather than removed on the spot: a removal is a linear
                // scan, and a style change that starts a transition on every element would make
                // clearing them out again quadratic.
                playback.engine = null;
                anyFinished = true;
            }
        }
        if (anyFinished) {
            playing.removeIf(playback -> playback.engine == null);
        }
    }

    /**
     * Hands every animation driving something {@code moved} accepts over to {@code destination},
     * carrying the progress each of them had made.
     *
     * <p>An animation only advances while the engine holding it is updated, and an engine is only
     * updated while its own UI is drawn — so an element tree that ends up being drawn by a different UI
     * leaves its running animations behind in an engine nobody touches again, where they never reach
     * their {@code onFinished}. Moving them across is what stops a re-host from freezing them for good.
     *
     * <p>Progress survives because both engines measure from {@link System#nanoTime()} and differ only
     * in when they started, so a single offset converts between the clocks: a bar three quarters of the
     * way along stays three quarters of the way along, and one whose duration ran out while nothing was
     * being drawn finishes on the destination's next frame — exactly as it would have without the move.
     *
     * @param moved recognises the objects that have moved, compared against
     *              {@link IFrameValueHandler#owner()}
     */
    public void handOver(AnimationEngine destination, Predicate<Object> moved) {
        if (destination == this) return;
        var offset = destination.getAppTime() - getAppTime();

        for (var playback : List.copyOf(waitToPlay)) {
            // Nothing to rebase: a queued playback has no runtime yet, and takes its start time from
            // whichever engine gets to it first.
            if (!playback.drivesAnyOf(moved) || !waitToPlay.remove(playback)) continue;
            playback.engine = destination;
            destination.waitToPlay.add(playback);
        }
        for (var playback : List.copyOf(playing)) {
            if (!playback.drivesAnyOf(moved)) continue;
            playing.remove(playback);
            playback.engine = destination;
            if (playback.runtime != null) {
                playback.runtime.shiftClock(offset);
            }
            destination.playing.add(playback);
        }
    }
}
