package com.lowdragmc.lowdraglib2.uitest.record;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A take's caption tracks, one per language, and their WebVTT form. Times are seconds of video. */
final class Captions {

    private static final class Cue {
        private final double start;
        private double end = -1;
        private final String text;

        private Cue(double start, String text) {
            this.start = start;
            this.text = text;
        }
    }

    private final Map<String, List<Cue>> tracks = new LinkedHashMap<>();

    /** Ends the language's current caption at {@code seconds} and, unless {@code text} is empty, starts this one. */
    void caption(String language, String text, double seconds) {
        var cues = tracks.computeIfAbsent(language, key -> new ArrayList<>());
        if (!cues.isEmpty() && cues.getLast().end < 0) cues.getLast().end = seconds;
        if (!text.isEmpty()) cues.add(new Cue(seconds, text));
    }

    /**
     * Every language with at least one caption that was ever on screen, as a WebVTT document, captions
     * still showing ending at {@code end}. A caption replaced in the same instant it appeared is left out.
     */
    Map<String, String> toVtt(double end) {
        var documents = new LinkedHashMap<String, String>();
        for (var track : tracks.entrySet()) {
            var out = new StringBuilder("WEBVTT\n");
            boolean any = false;
            for (var cue : track.getValue()) {
                double stop = cue.end < 0 ? end : cue.end;
                if (stop <= cue.start) continue;
                any = true;
                out.append('\n').append(timestamp(cue.start)).append(" --> ").append(timestamp(stop)).append('\n')
                        .append(escape(cue.text)).append('\n');
            }
            if (any) documents.put(track.getKey(), out.toString());
        }
        return documents;
    }

    /** A cue may not hold a blank line, and {@code &}, {@code <} and {@code >} are markup in it. */
    static String escape(String text) {
        var lines = text.replace("\r", "").replaceAll("\n{2,}", "\n").strip();
        return lines.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String timestamp(double seconds) {
        long millis = Math.max(0, Math.round(seconds * 1000));
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d",
                millis / 3_600_000, millis / 60_000 % 60, millis / 1000 % 60, millis % 1000);
    }
}
