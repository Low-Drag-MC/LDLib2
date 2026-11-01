package com.lowdragmc.lowdraglib2.uitest.record;

/**
 * How a recording run encodes its takes.
 *
 * @param fps     video frame rate; also the rate video time advances, one frame per rendered frame
 * @param ffmpeg  the ffmpeg executable, a path or a bare name resolved on {@code PATH}
 * @param codec   an ffmpeg video encoder name, or {@code auto} for {@code h264_nvenc} when it works on
 *                this machine and {@code libx264} otherwise
 * @param quality constant-quality level: {@code -crf} for software encoders, {@code -cq} for NVENC.
 *                Lower is better; 18 is visually lossless for UI content
 */
public record RecordSettings(int fps, String ffmpeg, String codec, int quality) {

    public RecordSettings {
        if (fps <= 0) throw new IllegalArgumentException("fps must be positive, got " + fps);
    }
}
