package com.lowdragmc.lowdraglib2.uitest.record;

import com.lowdragmc.lowdraglib2.LDLib2;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * An ffmpeg process fed raw I420 frames on stdin from a dedicated writer thread.
 *
 * <p>Frames travel in a fixed pool of buffers. When ffmpeg falls behind, {@link #acquire()} blocks the
 * render thread until a buffer comes back, which only slows the take down: video time is stepped per
 * frame, so the output is unaffected.
 */
final class FfmpegEncoder {

    private static final byte[] END = new byte[0];
    private static final Map<String, String> RESOLVED_CODECS = new ConcurrentHashMap<>();

    private final Process process;
    private final BlockingQueue<byte[]> free;
    private final BlockingQueue<byte[]> work = new LinkedBlockingQueue<>();
    private final Path log;
    private volatile IOException failure;
    private volatile long acquireWaitNanos;
    private boolean inputClosed;

    private FfmpegEncoder(Process process, Path log, int frameBytes, int poolSize) {
        this.process = process;
        this.log = log;
        this.free = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            free.add(new byte[frameBytes]);
        }
        var writer = new Thread(() -> write(process.getOutputStream()), "ldlib2-uitest-encoder");
        writer.setDaemon(true);
        writer.start();
    }

    /** @param poster where to write frame {@code posterFrame} as a PNG, or {@code null} for no still */
    static FfmpegEncoder start(RecordSettings settings, int fps, int width, int height, Path output, Path log,
                               int poolSize, Path poster, long posterFrame) throws IOException {
        var codec = resolveCodec(settings);
        var command = new ArrayList<>(List.of(settings.ffmpeg(), "-hide_banner", "-loglevel", "warning", "-y",
                "-f", "rawvideo", "-pix_fmt", "yuv420p", "-video_size", width + "x" + height,
                "-framerate", String.valueOf(fps),
                // On the input: ffmpeg 7+ negotiates colour through the filter graph and drops
                // primaries and transfer given only as output options.
                "-color_range", "tv", "-colorspace", "bt709", "-color_primaries", "bt709", "-color_trc", "bt709",
                "-i", "pipe:0"));
        command.addAll(codecArguments(codec, settings.quality()));
        command.addAll(List.of("-pix_fmt", "yuv420p",
                "-g", String.valueOf(fps * 2), "-movflags", "+faststart", output.toString()));
        if (poster != null) {
            if (posterFrame > 0) command.addAll(List.of("-vf", "select='gte(n," + posterFrame + ")'"));
            command.addAll(List.of("-frames:v", "1", "-update", "1", poster.toString()));
        }
        var process = launch(settings, new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile()));
        LDLib2.LOGGER.info("[uitest] encoding {}x{}@{} with {} -> {}", width, height, fps, codec, output);
        return new FfmpegEncoder(process, log, width * height * 3 / 2, poolSize);
    }

    private static List<String> codecArguments(String codec, int quality) {
        var q = String.valueOf(quality);
        return switch (codec) {
            // p4: 4K UI footage at ~120 fps on an RTX 4080 with SSIM 0.999; p5 halves that for no visible gain
            case "h264_nvenc", "hevc_nvenc", "av1_nvenc" ->
                    List.of("-c:v", codec, "-preset", "p4", "-tune", "hq", "-rc", "vbr", "-cq", q, "-b:v", "0");
            case "libx264", "libx265" -> List.of("-c:v", codec, "-preset", "veryfast", "-crf", q);
            default -> List.of("-c:v", codec, "-crf", q);
        };
    }

    /** The encoder {@code auto} stands for on this machine, probed once per ffmpeg executable. */
    static String resolveCodec(RecordSettings settings) throws IOException {
        if (!"auto".equalsIgnoreCase(settings.codec())) return settings.codec();
        var cached = RESOLVED_CODECS.get(settings.ffmpeg());
        if (cached != null) return cached;
        var codec = encodes(settings, "h264_nvenc") ? "h264_nvenc" : "libx264";
        RESOLVED_CODECS.put(settings.ffmpeg(), codec);
        return codec;
    }

    /** Encodes one frame the way a take feeds them, so a build without some optional input cannot fool it. */
    private static boolean encodes(RecordSettings settings, String codec) throws IOException {
        var probe = launch(settings, new ProcessBuilder(settings.ffmpeg(), "-hide_banner", "-loglevel", "error",
                "-f", "rawvideo", "-pix_fmt", "yuv420p", "-video_size", "256x256", "-framerate", "1", "-i", "pipe:0",
                "-frames:v", "1", "-c:v", codec, "-f", "null", "-")
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD));
        try (var stdin = probe.getOutputStream()) {
            stdin.write(new byte[256 * 256 * 3 / 2]);
        } catch (IOException ignored) {
            // ffmpeg already refused; its exit code says so
        }
        try {
            if (!probe.waitFor(15, TimeUnit.SECONDS)) {
                probe.destroyForcibly();
                return false;
            }
            return probe.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            probe.destroyForcibly();
            return false;
        }
    }

    private static Process launch(RecordSettings settings, ProcessBuilder builder) throws IOException {
        try {
            return builder.start();
        } catch (IOException e) {
            throw new IOException("could not start ffmpeg '" + settings.ffmpeg() + "'. Install it "
                    + "(e.g. winget install Gyan.FFmpeg) or point -PldRecordFfmpeg at the executable.", e);
        }
    }

    private void write(OutputStream stdin) {
        try {
            while (true) {
                var frame = work.take();
                if (frame == END) break;
                if (failure == null) {
                    try {
                        stdin.write(frame);
                    } catch (IOException e) {
                        failure = e;
                    }
                }
                free.add(frame);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            stdin.close();
        } catch (IOException e) {
            if (failure == null) failure = e;
        }
    }

    /** A buffer to fill with the next frame. Blocks while ffmpeg is behind. */
    byte[] acquire() throws IOException {
        long started = System.nanoTime();
        try {
            while (true) {
                if (failure != null) {
                    throw new IOException("ffmpeg stopped accepting frames, see " + log, failure);
                }
                if (!process.isAlive()) {
                    throw new IOException("ffmpeg exited with " + process.exitValue() + " mid-take, see " + log);
                }
                var frame = free.poll(100, TimeUnit.MILLISECONDS);
                if (frame != null) return frame;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted waiting for the encoder", e);
        } finally {
            acquireWaitNanos += System.nanoTime() - started;
        }
    }

    void submit(byte[] frame) {
        work.add(frame);
    }

    /** Ends the stream; ffmpeg finalises the file and exits on its own. */
    void closeInput() {
        if (inputClosed) return;
        inputClosed = true;
        work.add(END);
    }

    void kill() {
        closeInput();
        process.destroyForcibly();
    }

    boolean isFinished() {
        return !process.isAlive();
    }

    int exitCode() {
        return process.exitValue();
    }

    long acquireWaitNanos() {
        return acquireWaitNanos;
    }

    Path log() {
        return log;
    }
}
