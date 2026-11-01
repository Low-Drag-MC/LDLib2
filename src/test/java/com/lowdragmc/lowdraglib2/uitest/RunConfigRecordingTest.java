package com.lowdragmc.lowdraglib2.uitest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A run is of tests or of recordings, and only a recording run carries encoding settings. */
class RunConfigRecordingTest {

    @AfterEach
    void clear() {
        for (var key : new String[]{RunConfig.PROP_RUN, RunConfig.PROP_RECORDINGS, RunConfig.PROP_RECORD_FPS,
                RunConfig.PROP_RECORD_FFMPEG, RunConfig.PROP_RECORD_CODEC, RunConfig.PROP_RECORDINGS_OUT}) {
            System.clearProperty(key);
        }
    }

    @Test
    void aTestRunNeverRecords() {
        System.setProperty(RunConfig.PROP_RUN, "all");
        System.setProperty(RunConfig.PROP_RECORD_FPS, "30");
        var config = RunConfig.fromSystemProperties();
        assertNotNull(config);
        assertFalse(config.recordings());
        assertNull(config.record());
    }

    @Test
    void aRecordingRunSelectsRecordingsAndEncodes() {
        System.setProperty(RunConfig.PROP_RECORDINGS, "tag:demo");
        System.setProperty(RunConfig.PROP_RECORD_FPS, "30");
        System.setProperty(RunConfig.PROP_RECORD_FFMPEG, "C:/tools/ffmpeg.exe");
        var config = RunConfig.fromSystemProperties();
        assertNotNull(config);
        assertTrue(config.recordings());
        assertEquals("tag:demo", config.selection());
        assertEquals(30, config.record().fps());
        assertEquals("C:/tools/ffmpeg.exe", config.record().ffmpeg());
        assertEquals("auto", config.record().codec());
    }

    @Test
    void testsAndRecordingsCannotShareARun() {
        System.setProperty(RunConfig.PROP_RUN, "all");
        System.setProperty(RunConfig.PROP_RECORDINGS, "all");
        assertThrows(IllegalArgumentException.class, RunConfig::fromSystemProperties);
    }

    @Test
    void neitherMeansNoRunAtAll() {
        assertNull(RunConfig.fromSystemProperties());
    }

    @Test
    void encodingDefaultsNeedNoProperties() {
        System.setProperty(RunConfig.PROP_RECORDINGS, "all");
        var record = RunConfig.fromSystemProperties().record();
        assertEquals(60, record.fps());
        assertEquals("ffmpeg", record.ffmpeg());
        assertEquals(18, record.quality());
    }

    @Test
    void anInteractiveTestNeverRecordsButAnInteractiveRecordingDoes() {
        assertNull(RunConfig.interactive("x").record());
        System.setProperty(RunConfig.PROP_RECORDINGS_OUT, "build/recs");
        var recording = RunConfig.interactiveRecording("x");
        assertTrue(recording.recordings());
        assertTrue(recording.outDir().endsWith("recs"));
    }

    @Test
    void aNonPositiveFrameRateIsLiftedToOne() {
        System.setProperty(RunConfig.PROP_RECORDINGS, "all");
        System.setProperty(RunConfig.PROP_RECORD_FPS, "0");
        assertEquals(1, RunConfig.fromSystemProperties().record().fps());
    }
}
