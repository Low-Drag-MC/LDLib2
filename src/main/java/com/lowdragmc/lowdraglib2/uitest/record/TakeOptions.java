package com.lowdragmc.lowdraglib2.uitest.record;

import org.jetbrains.annotations.Nullable;

/**
 * How one take is framed and encoded. Passed to {@code ScenarioBuilder#take(String, Consumer, Consumer)}:
 *
 * <pre>{@code
 * s.take("add_node", o -> o.region("#graph").padding(16).size(1920, 1080).fps(30), t -> t ...);
 * }</pre>
 *
 * <p>Everything defaults to the run's settings: the whole window, at its own pixel size, at
 * {@code -PldRecordFps}, with pointer, click ripples and the key display all shown. Region selectors are
 * resolved when the take starts, so the element must be laid out by then.
 *
 * <p>Pure data, safe to build on any dist.
 */
public final class TakeOptions {

    @Nullable
    String regionSelector;
    @Nullable
    float[] regionRect;
    float padding = 12;
    boolean keyStrip = true;
    int width;
    int height;
    float scale = 1;
    int fps;
    boolean cursor = true;
    boolean clicks = true;
    boolean keys = true;
    @Nullable
    String burnCaptions;
    long posterAtMs;
    boolean poster = true;

    /** The selector a {@link #region(String)} names, resolved by the runner when the take starts. */
    @Nullable
    public String regionSelector() {
        return regionSelector;
    }

    /** Records only the element's bounds plus {@link #padding}, instead of the whole window. */
    public TakeOptions region(String selector) {
        this.regionSelector = selector;
        this.regionRect = null;
        return this;
    }

    /** Records only this rectangle, in GUI-scaled screen coordinates, plus {@link #padding}. */
    public TakeOptions region(float x, float y, float width, float height) {
        this.regionRect = new float[]{x, y, width, height};
        this.regionSelector = null;
        return this;
    }

    /** Space kept around a {@link #region}, in GUI pixels. Default 12. */
    public TakeOptions padding(float padding) {
        this.padding = Math.max(0, padding);
        return this;
    }

    /**
     * Whether a {@link #region} take adds a strip below the region for the key display, so the keys never
     * cover the UI. Default on; off draws them over the bottom of the region instead.
     */
    public TakeOptions keyStrip(boolean keyStrip) {
        this.keyStrip = keyStrip;
        return this;
    }

    /**
     * The video's exact size in pixels. The recorded area grows around its centre to the same aspect
     * ratio, so nothing is stretched, and is scaled to fit: record a 4K window at {@code size(1920, 1080)}
     * for a supersampled 1080p video that stays sharp when {@code zoom} brings things closer.
     */
    public TakeOptions size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    /** The video's size as a multiple of the recorded area's pixel size. Ignored when {@link #size} is set. */
    public TakeOptions scale(float scale) {
        this.scale = scale;
        return this;
    }

    /** Frame rate of this take; video time advances {@code 1/fps} per rendered frame. Default {@code -PldRecordFps}. */
    public TakeOptions fps(int fps) {
        this.fps = fps;
        return this;
    }

    /** Whether the video shows the pointer. */
    public TakeOptions cursor(boolean cursor) {
        this.cursor = cursor;
        return this;
    }

    /** Whether the video shows a ripple where each click lands. */
    public TakeOptions clicks(boolean clicks) {
        this.clicks = clicks;
        return this;
    }

    /** Whether the video shows the keys being pressed. */
    public TakeOptions keys(boolean keys) {
        this.keys = keys;
        return this;
    }

    /**
     * Draws this language's captions into the video as well, above the key display. Every language is
     * still written as a {@code .vtt} file next to the video either way.
     */
    public TakeOptions burnCaptions(String language) {
        this.burnCaptions = language;
        return this;
    }

    /** Takes the poster image this far into the video instead of from its first frame. */
    public TakeOptions poster(long atMs) {
        this.posterAtMs = Math.max(0, atMs);
        this.poster = true;
        return this;
    }

    /** Writes no poster image. */
    public TakeOptions noPoster() {
        this.poster = false;
        return this;
    }
}
