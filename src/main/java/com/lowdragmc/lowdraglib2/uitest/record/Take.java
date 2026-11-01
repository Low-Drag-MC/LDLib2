package com.lowdragmc.lowdraglib2.uitest.record;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.uitest.input.InputDriver;
import com.lowdragmc.lowdraglib2.uitest.input.InputListener;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * One recorded take and everything written alongside its video: a poster still, a WebVTT caption track
 * per language, and a timeline of what happened when. Also the camera, which can glide in on part of
 * the frame and back out while recording.
 *
 * <p>Installed as the input driver's listener for as long as it records, so the overlay and the timeline
 * both see every gesture. Render thread only.
 */
public final class Take implements InputListener {

    private final String name;
    private final TakeOptions options;
    private final InputDriver driver;
    private final RecordingOverlay overlay;
    private final VideoRecorder recorder;
    private final Path directory;
    @Nullable
    private final Path poster;
    private final double guiScale;
    private final float screenWidth;
    private final float screenHeight;
    private final float[] home;
    private final float aspect;
    private final float[] camera;
    private final float[] cameraFrom = new float[4];
    private final float[] cameraTo = new float[4];
    private long cameraStart;
    private long cameraNanos;
    private final Captions captions = new Captions();
    private final ServerPace serverPace = new ServerPace();
    private final JsonArray timeline = new JsonArray();
    private final List<Path> files = new ArrayList<>();
    private boolean finished;

    private Take(String name, TakeOptions options, InputDriver driver, RecordingOverlay overlay, VideoRecorder recorder,
                 Path directory, @Nullable Path poster, double guiScale, float screenWidth, float screenHeight,
                 float[] home, float aspect) {
        this.name = name;
        this.options = options;
        this.driver = driver;
        this.overlay = overlay;
        this.recorder = recorder;
        this.directory = directory;
        this.poster = poster;
        this.guiScale = guiScale;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.home = home;
        this.aspect = aspect;
        this.camera = home.clone();
    }

    /**
     * Starts recording, and installs the take as {@code driver}'s listener until {@link #finish()} or
     * {@link #abort()}.
     *
     * @param target the element a {@code region} option resolved to, {@code x, y, width, height} in GUI-scaled
     *               coordinates before padding; {@code null} when the options name no element
     * @throws IOException when ffmpeg cannot be started
     * @throws IllegalStateException when the region leaves nothing to record, or another take is open
     */
    public static Take open(RecordSettings settings, TakeOptions options, String name, Path directory, InputDriver driver,
                            RenderTarget source, double guiScale, @Nullable float[] target) throws IOException {
        float screenWidth = (float) (source.width / guiScale);
        float screenHeight = (float) (source.height / guiScale);
        var home = Framing.region(options, target, screenWidth, screenHeight);
        var size = Framing.outputSize(options, home, guiScale);
        if (size == null) throw new IllegalStateException("take '" + name + "' has nothing to record");

        int fps = options.fps > 0 ? options.fps : settings.fps();
        var poster = options.poster ? directory.resolve(name + ".png") : null;
        var overlay = new RecordingOverlay(driver);
        overlay.show(options.cursor, options.clicks, options.keys);
        overlay.setRegion(home[0], home[1], home[2], home[3]);
        VideoRecorder recorder;
        try {
            recorder = VideoRecorder.open(settings, fps, directory.resolve(name + ".mp4"), poster, options.posterAtMs * fps / 1000,
                    source, overlay, scaled(home, guiScale), size[0], size[1]);
        } catch (IOException | RuntimeException e) {
            overlay.close();
            throw e;
        }
        var take = new Take(name, options, driver, overlay, recorder, directory, poster, guiScale, screenWidth, screenHeight,
                home, size[0] / (float) size[1]);
        driver.setListener(take);
        return take;
    }

    private static float[] scaled(float[] rect, double scale) {
        return new float[]{(float) (rect[0] * scale), (float) (rect[1] * scale), (float) (rect[2] * scale), (float) (rect[3] * scale)};
    }

    public String name() {
        return name;
    }

    /** The part of the screen the take records, in GUI-scaled coordinates, before any zoom. */
    public float[] home() {
        return home.clone();
    }

    // region per frame

    /** Records the frame just rendered, then lets the integrated server catch up with the video's clock. */
    public void captureFrame(RenderTarget source) throws IOException {
        moveCamera();
        recorder.captureFrame(source);
        var minecraft = Minecraft.getInstance();
        serverPace.await(minecraft.getSingleplayerServer(), minecraft.isPaused(), recorder.videoSeconds());
    }

    private void moveCamera() {
        if (cameraNanos <= 0) return;
        float t = Math.min(1f, (Util.getNanos() - cameraStart) / (float) cameraNanos);
        float s = t * t * t * (10 - 15 * t + 6 * t * t);
        for (int i = 0; i < 4; i++) {
            camera[i] = cameraFrom[i] + (cameraTo[i] - cameraFrom[i]) * s;
        }
        if (t >= 1) cameraNanos = 0;
        var pixels = scaled(camera, guiScale);
        recorder.setRegion(pixels[0], pixels[1], pixels[2], pixels[3]);
        overlay.setRegion(camera[0], camera[1], camera[2], camera[3]);
    }

    // endregion

    // region directing

    /**
     * Glides the camera so {@code rect} (GUI-scaled, plus {@code padding}) fills the video, keeping its
     * aspect ratio, over {@code millis} of video time.
     */
    public void zoomTo(float[] rect, float padding, long millis) {
        var frame = Framing.zoom(rect, padding, options.keys || options.burnCaptions != null, aspect, screenWidth, screenHeight);
        glide(frame, millis);
        event("zoom", String.format(Locale.ROOT, "%.0f,%.0f %.0fx%.0f", frame[0], frame[1], frame[2], frame[3]));
    }

    /** Glides the camera back to the take's own framing. */
    public void zoomOut(long millis) {
        glide(home, millis);
        event("zoom", "home");
    }

    public boolean isCameraMoving() {
        return cameraNanos > 0;
    }

    private void glide(float[] to, long millis) {
        System.arraycopy(camera, 0, cameraFrom, 0, 4);
        System.arraycopy(to, 0, cameraTo, 0, 4);
        cameraStart = Util.getNanos();
        cameraNanos = Math.max(1, millis) * 1_000_000L;
    }

    /** See {@link RecordingOverlay#setSpotlight}. */
    public void spotlight(@Nullable Supplier<float[]> bounds, String description) {
        overlay.setSpotlight(bounds);
        event("spotlight", bounds == null ? "" : description);
    }

    /**
     * Starts a caption in {@code language} from the next frame; an empty {@code text} ends the current one.
     * A caption also ends when the next one in the same language starts, or with the take.
     */
    public void caption(String language, String text) {
        captions.caption(language, text, recorder.videoSeconds());
        if (language.equals(options.burnCaptions)) overlay.setCaption(text);
        event("caption", language + ": " + text);
    }

    public void step(String stepName) {
        event("step", stepName);
    }

    private void event(String type, String detail) {
        var event = new JsonObject();
        event.addProperty("t", Math.round(recorder.videoSeconds() * 1000) / 1000.0);
        event.addProperty("type", type);
        event.addProperty("detail", detail);
        timeline.add(event);
    }

    // endregion

    // region input

    @Override
    public void mouseDown(float x, float y, int button) {
        overlay.mouseDown(x, y, button);
        event("press", String.format(Locale.ROOT, "button %d at %.0f,%.0f", button, x, y));
    }

    @Override
    public void mouseUp(float x, float y, int button) {
        overlay.mouseUp(x, y, button);
        event("release", String.format(Locale.ROOT, "button %d at %.0f,%.0f", button, x, y));
    }

    @Override
    public void scroll(float x, float y, double amount) {
        overlay.scroll(x, y, amount);
        event("scroll", String.valueOf(amount));
    }

    @Override
    public void keyDown(int keyCode, int modifiers) {
        overlay.keyDown(keyCode, modifiers);
        event("keyDown", keyCode + (modifiers == 0 ? "" : " mods " + modifiers));
    }

    @Override
    public void keyUp(int keyCode, int modifiers) {
        overlay.keyUp(keyCode, modifiers);
    }

    @Override
    public void charTyped(char codePoint) {
        overlay.charTyped(codePoint);
        event("type", String.valueOf(codePoint));
    }

    // endregion

    // region finishing

    /**
     * Stops recording: writes the caption tracks and timeline, and has ffmpeg's input closed once the frames
     * still in flight come back over the next frame or two. Poll {@link #isDone()} once a frame for ffmpeg to
     * finish the video and poster, then {@link #failure()}. Safe to call twice; the second call does nothing.
     */
    public void finish() throws IOException {
        if (finished) return;
        finished = true;
        double end = recorder.videoSeconds();
        release();
        try {
            for (var track : captions.toVtt(end).entrySet()) {
                var file = directory.resolve(name + "." + track.getKey() + ".vtt");
                Files.writeString(file, track.getValue(), StandardCharsets.UTF_8);
                files.add(file);
            }
            var json = new JsonObject();
            json.addProperty("take", name);
            json.addProperty("fps", recorder.fps());
            json.addProperty("frames", Math.round(end * recorder.fps()));
            json.addProperty("seconds", end);
            json.add("events", timeline);
            var file = directory.resolve(name + ".timeline.json");
            Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(json), StandardCharsets.UTF_8);
            files.add(file);
        } finally {
            recorder.finish();
        }
    }

    /** Stops without draining, for a take that cannot complete: ffmpeg is killed and the video is lost. */
    public void abort() {
        finished = true;
        release();
        recorder.abort();
    }

    private void release() {
        driver.setListener(null);
        overlay.close();
    }

    public boolean isDone() {
        return recorder.isDone();
    }

    /** Closes ffmpeg's input without waiting for the frames still in flight, for a take ended outside the render loop. */
    public void closeNow() {
        recorder.closeNow();
    }

    /** Why frames are missing from the video, if any are. Meaningful once {@link #isDone()}. */
    @Nullable
    public IOException failure() {
        return recorder.failure();
    }

    public int exitCode() {
        return recorder.exitCode();
    }

    public Path video() {
        return recorder.output();
    }

    public Path log() {
        return recorder.log();
    }

    /** The poster, if ffmpeg wrote it, then the caption tracks and the timeline. */
    public List<Path> extras() {
        var out = new ArrayList<Path>();
        if (poster != null && Files.isRegularFile(poster)) out.add(poster);
        out.addAll(files);
        return out;
    }

    /** A poster was asked for but not written, because its time was past the end of the video. */
    public boolean posterMissing() {
        return poster != null && !Files.isRegularFile(poster);
    }

    public String summary() {
        return recorder.summary();
    }

    // endregion
}
