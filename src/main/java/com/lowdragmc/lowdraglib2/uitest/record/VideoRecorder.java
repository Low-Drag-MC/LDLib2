package com.lowdragmc.lowdraglib2.uitest.record;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * One take: every frame rendered while it is open goes, in order, into one video file.
 *
 * <p>Per frame, on the render thread: a GPU pass converts the frame to I420 and
 * {@code copyTextureToBuffer} starts an asynchronous read into one of {@value #SLOTS} buffers. When the read
 * completes depends on the graphics backend: OpenGL queues it behind a fence
 * ({@code RenderSystem#queueFencedTask}) that the game drains at the start of every frame, while Vulkan runs
 * it from the {@code submit()} that ends the frame after the one that issued it. Either way the completion
 * runs on the render thread, in the order the reads were issued, so the video comes out in order; it maps
 * the buffer, copies the bytes into a pooled array and hands it to {@link FfmpegEncoder}'s writer thread.
 *
 * <p>So nothing here may wait for a read to complete within a frame - on Vulkan it cannot until the frame
 * has been submitted. Capturing never needs to: Vulkan keeps at most two frames in flight, fewer than the
 * slots. And {@link #finish()} only asks for the take to end: the frames still in flight close ffmpeg's
 * input when the last of them comes back, over the next frame or two, and {@link #isDone()} is polled once
 * a frame until then.
 */
public final class VideoRecorder {

    private static final int SLOTS = 3;
    private static final int ENCODER_BUFFERS = 4;
    private static final long READBACK_TIMEOUT_NANOS = 5_000_000_000L;

    private final int fps;
    private final Path output;
    private final int sourceWidth;
    private final int sourceHeight;
    private float regionX;
    private float regionY;
    private float regionWidth;
    private float regionHeight;
    private final YuvConverter converter;
    private final FfmpegEncoder encoder;
    @Nullable
    private final RecordingOverlay overlay;
    @Nullable
    private TextureTarget composite;
    private final GpuBuffer[] readbacks = new GpuBuffer[SLOTS];
    private long issued;
    private long collected;
    /** No more frames are taken: {@link #finish()} or {@link #abort()} was called. */
    private boolean finished;
    /** ffmpeg's input is closed, so every frame it will get has been handed over. */
    private boolean inputClosed;
    /** When {@link #isDone()} stops waiting for the frames still in flight after {@link #finish()}. */
    private long drainDeadline;
    /** Set once the buffers are gone, so a completion still queued behind a fence does nothing. */
    private boolean released;
    /** Why a completion could not hand its frame over; it may not throw, as it runs inside the game's queue. */
    @Nullable
    private IOException readbackFailure;

    private final long startedNanos = System.nanoTime();
    private long finishedNanos;
    private long convertNanos;
    private long collectNanos;

    private VideoRecorder(RecordSettings settings, int fps, Path output, @Nullable Path poster, long posterFrame,
                          RenderTarget source, @Nullable RecordingOverlay overlay, float[] region,
                          int outWidth, int outHeight) throws IOException {
        if (VideoClock.isStepping()) throw new IllegalStateException("another take is still open");
        this.fps = fps;
        this.overlay = overlay;
        this.output = output;
        this.sourceWidth = source.width;
        this.sourceHeight = source.height;
        setRegion(region[0], region[1], region[2], region[3]);
        this.converter = new YuvConverter(outWidth, outHeight);
        try {
            Files.createDirectories(output.getParent());
            var name = output.getFileName().toString();
            var log = output.resolveSibling(name.substring(0, name.lastIndexOf('.')) + ".ffmpeg.log");
            this.encoder = FfmpegEncoder.start(settings, fps, outWidth, outHeight, output, log, ENCODER_BUFFERS,
                    poster, posterFrame);
        } catch (IOException | RuntimeException e) {
            converter.close();
            throw e;
        }
        var device = RenderSystem.getDevice();
        for (int i = 0; i < SLOTS; i++) {
            int slot = i;
            readbacks[i] = device.createBuffer(() -> "ldlib2 recording readback " + slot,
                    GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, converter.readbackBytes);
        }
        VideoClock.beginStepping(fps);
    }

    /**
     * Opens a take and starts stepping {@link VideoClock}: from the next frame on, each
     * {@link #captureFrame} advances time by one video frame until {@link #finish()} or {@link #abort()}.
     *
     * @param region      {@code x, y, width, height} of {@code source} to record, in framebuffer pixels from
     *                    the top-left; scaled to {@code outWidth x outHeight}, and movable with {@link #setRegion}
     * @param outWidth    the video's width, even
     * @param poster      where to write a still of frame {@code posterFrame}, or {@code null} for none
     * @param overlay     drawn over every frame of the video only; the game's own frame is untouched
     * @throws IOException when ffmpeg cannot be started
     * @throws IllegalStateException when another take is still open
     */
    public static VideoRecorder open(RecordSettings settings, int fps, Path output, @Nullable Path poster, long posterFrame,
                                     RenderTarget source, @Nullable RecordingOverlay overlay, float[] region,
                                     int outWidth, int outHeight) throws IOException {
        return new VideoRecorder(settings, fps, output, poster, posterFrame, source, overlay, region, outWidth, outHeight);
    }

    /** Moves the recorded rectangle, in framebuffer pixels; takes effect from the next captured frame. */
    public void setRegion(float x, float y, float width, float height) {
        regionWidth = Math.max(2, Math.min(width, sourceWidth));
        regionHeight = Math.max(2, Math.min(height, sourceHeight));
        regionX = Math.max(0, Math.min(x, sourceWidth - regionWidth));
        regionY = Math.max(0, Math.min(y, sourceHeight - regionHeight));
    }

    public int fps() {
        return fps;
    }

    /** Seconds of video captured so far: the time at which the next frame will appear. */
    public double videoSeconds() {
        return issued / (double) fps;
    }

    /** Converts and queues the frame just rendered into {@code source}. Call once per frame, after rendering. */
    public void captureFrame(RenderTarget source) throws IOException {
        if (finished) throw new IllegalStateException("take already finished");
        if (source.width != sourceWidth || source.height != sourceHeight) {
            throw new IllegalStateException("the frame changed size during a take: " + sourceWidth + "x"
                    + sourceHeight + " -> " + source.width + "x" + source.height);
        }
        throwIfFailed();
        if (issued - collected == SLOTS) {
            awaitCollected(collected + 1);
        }
        long started = System.nanoTime();
        var frame = overlay == null ? source : composite(source);
        var view = frame.getColorTextureView();
        if (view == null) throw new IllegalStateException("the frame has no colour texture to record");
        converter.convert(view, regionX, regionY, regionWidth, regionHeight);
        int slot = (int) (issued % SLOTS);
        var readback = readbacks[slot];
        RenderSystem.getDevice().createCommandEncoder()
                .copyTextureToBuffer(converter.texture(), readback, 0, () -> collect(readback), 0);
        issued++;
        convertNanos += System.nanoTime() - started;
        VideoClock.advanceFrame();
    }

    /**
     * A copy of {@code source} with the overlay drawn on it. {@code source} is the game's own frame and is
     * only read, so screenshots and the window never show the overlay.
     */
    private RenderTarget composite(RenderTarget source) {
        if (composite == null) {
            // the game's own depth format, which NeoForge may have given a stencil aspect, so every gui pipeline
            // draws into it unchanged
            var depthFormat = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTexture().getFormat();
            composite = new TextureTarget("ldlib2 recording composite", source.width, source.height, GpuFormat.RGBA8_UNORM, depthFormat);
        }
        var sourceColor = source.getColorTexture();
        var color = composite.getColorTexture();
        var depth = composite.getDepthTexture();
        if (sourceColor == null || color == null || depth == null) {
            throw new IllegalStateException("the frame or the recording's composite has no textures");
        }
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(sourceColor, color, 0, 0, 0, 0, 0, source.width, source.height);
        // 0.0, the far plane: the gui draws reversed-Z, testing GREATER_THAN_OR_EQUAL, so 1.0 would hide it all
        encoder.clearDepthTexture(depth, 0.0);
        overlay.draw(composite);
        return composite;
    }

    /** The completion of one read, run by the game's fenced-task queue: may not throw. */
    private void collect(GpuBuffer readback) {
        if (released) return;
        long started = System.nanoTime();
        try {
            if (readbackFailure == null) {
                var frame = encoder.acquire();
                try (var view = readback.map(true, false)) {
                    view.data().get(0, frame, 0, converter.frameBytes);
                }
                encoder.submit(frame);
            }
        } catch (IOException e) {
            readbackFailure = e;
        } catch (RuntimeException e) {
            readbackFailure = new IOException("could not read a frame back", e);
        } finally {
            collected++;
            collectNanos += System.nanoTime() - started;
        }
        // the last frame of a finished take: everything ffmpeg will get has been handed over
        if (finished && !inputClosed && collected >= issued) {
            closeInput();
        }
    }

    /**
     * Pumps the game's fenced-task queue until {@code count} reads have completed. That only completes OpenGL's
     * reads; a Vulkan read completes in a frame's {@code submit()}, so on Vulkan this is reached only if every
     * slot is in flight, which its pipeline depth keeps from happening.
     */
    private void awaitCollected(long count) throws IOException {
        long deadline = System.nanoTime() + READBACK_TIMEOUT_NANOS;
        while (collected < count) {
            RenderSystem.executePendingTasks();
            if (collected >= count) break;
            if (System.nanoTime() > deadline) {
                throw new IOException("frame readback never completed: " + (count - collected) + " frame(s) still in flight");
            }
            Thread.onSpinWait();
        }
        throwIfFailed();
    }

    private void throwIfFailed() throws IOException {
        if (readbackFailure != null) throw readbackFailure;
    }

    /**
     * Ends the take: no more frames are taken, and ffmpeg's input closes once the frames still in flight have
     * come back, which takes the render loop a frame or two to complete. Poll {@link #isDone()} once a frame
     * afterwards, and {@link #failure()} once it is done.
     */
    public void finish() throws IOException {
        if (finished) return;
        finished = true;
        VideoClock.endStepping();
        drainDeadline = System.nanoTime() + READBACK_TIMEOUT_NANOS;
        // whatever has already come back; the rest completes with the frames to come
        RenderSystem.executePendingTasks();
        if (!inputClosed && collected >= issued) {
            closeInput();
        }
        throwIfFailed();
    }

    /**
     * Closes ffmpeg's input now, without waiting for the frames still in flight, which are dropped - at most
     * the last two. For a take ended while the render loop is not running, which is what completes them.
     */
    public void closeNow() {
        finished = true;
        VideoClock.endStepping();
        if (!inputClosed) {
            closeInput();
        }
    }

    private void closeInput() {
        inputClosed = true;
        finishedNanos = System.nanoTime();
        encoder.closeInput();
        release();
    }

    /** Stops without draining, for a take that cannot complete. The file is left however ffmpeg leaves it. */
    public void abort() {
        VideoClock.endStepping();
        finished = true;
        if (!inputClosed) {
            inputClosed = true;
            finishedNanos = System.nanoTime();
            release();
        }
        encoder.kill();
    }

    /**
     * Whether the take is over: every frame handed to ffmpeg and ffmpeg exited. Only meaningful after
     * {@link #finish()}. Frames that never come back are given up on after a while, which {@link #failure()}
     * then reports.
     */
    public boolean isDone() {
        if (!inputClosed) {
            if (!finished || System.nanoTime() < drainDeadline) return false;
            if (readbackFailure == null) {
                readbackFailure = new IOException("frame readback never completed: " + (issued - collected)
                        + " frame(s) still in flight");
            }
            closeInput();
        }
        return encoder.isFinished();
    }

    /** Why frames are missing from the video, if any are: a read that failed or never completed. */
    @Nullable
    public IOException failure() {
        return readbackFailure;
    }

    public int exitCode() {
        return encoder.exitCode();
    }

    public Path output() {
        return output;
    }

    public Path log() {
        return encoder.log();
    }

    public long frames() {
        return collected;
    }

    public String summary() {
        long frames = Math.max(1, collected);
        double wallSeconds = ((inputClosed ? finishedNanos : System.nanoTime()) - startedNanos) / 1e9;
        double videoSeconds = collected / (double) fps;
        return "%d frames %dx%d (%.2fs of video) in %.2fs wall, %.1f fps, %.2fx real time; per frame: convert %.2fms, collect %.2fms (encoder backpressure %.2fms)"
                .formatted(collected, converter.width, converter.height, videoSeconds, wallSeconds,
                        collected / Math.max(1e-9, wallSeconds), videoSeconds / Math.max(1e-9, wallSeconds),
                        convertNanos / 1e6 / Math.max(1, issued), collectNanos / 1e6 / frames,
                        encoder.acquireWaitNanos() / 1e6 / frames);
    }

    private void release() {
        released = true;
        // A read still queued behind a fence keeps its buffer alive on the GPU side; its completion sees
        // `released` and never maps it.
        for (int i = 0; i < SLOTS; i++) {
            if (readbacks[i] != null) {
                readbacks[i].close();
                readbacks[i] = null;
            }
        }
        converter.close();
        if (composite != null) {
            composite.destroyBuffers();
            composite = null;
        }
    }
}
